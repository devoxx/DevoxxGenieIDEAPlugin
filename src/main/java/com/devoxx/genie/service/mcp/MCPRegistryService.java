package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.registry.*;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

@Slf4j
public class MCPRegistryService {

    private static final String REGISTRY_BASE_URL = "https://registry.modelcontextprotocol.io/v0.1/servers";
    private final OkHttpClient client = HttpClientProvider.getClient();
    private final Gson gson = new GsonBuilder().create();

    private List<MCPRegistryServerEntry> cachedServers = null;

    @NotNull
    public static MCPRegistryService getInstance() {
        return ApplicationManager.getApplication().getService(MCPRegistryService.class);
    }

    /**
     * Fetch all servers from the registry and cache them in memory.
     *
     * @param forceRefresh if true, re-fetches from the registry even if cached
     * @return the complete list of servers
     * @throws IOException if any network request fails
     */
    @NotNull
    public List<MCPRegistryServerEntry> fetchAllServers(boolean forceRefresh) throws IOException {
        if (cachedServers != null && !forceRefresh) {
            return cachedServers;
        }
        List<MCPRegistryServerEntry> allServers = new ArrayList<>();
        String cursor = null;
        do {
            MCPRegistryResponse response = searchServers(null, cursor, 100);
            if (response.getServers() != null) {
                allServers.addAll(response.getServers());
            }
            cursor = (response.getMetadata() != null) ? response.getMetadata().getNextCursor() : null;
        } while (cursor != null && !cursor.isBlank());
        cachedServers = allServers;
        return cachedServers;
    }

    /**
     * Search for MCP servers in the registry.
     *
     * @param query  optional search query
     * @param cursor optional pagination cursor from a previous response
     * @param limit  max number of results to return
     * @return the registry response containing servers and pagination metadata
     * @throws IOException if the network request fails
     */
    @NotNull
    public MCPRegistryResponse searchServers(@Nullable String query,
                                             @Nullable String cursor,
                                             int limit) throws IOException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(REGISTRY_BASE_URL)).newBuilder();
        urlBuilder.addQueryParameter("limit", String.valueOf(limit));

        if (query != null && !query.isBlank()) {
            urlBuilder.addQueryParameter("q", query);
        }
        if (cursor != null && !cursor.isBlank()) {
            urlBuilder.addQueryParameter("cursor", cursor);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Registry API returned HTTP " + response.code());
            }
            if (response.body() == null) {
                throw new IOException("Registry API returned empty response");
            }

            MCPRegistryResponse result = gson.fromJson(response.body().string(), MCPRegistryResponse.class);
            return result != null ? result : new MCPRegistryResponse();
        }
    }

    /**
     * Convert a registry server info into an MCPServer configuration ready to be added to settings.
     *
     * @param serverInfo the server info from the registry
     * @param envValues  user-provided values for environment variables and headers
     * @return a configured MCPServer instance
     */
    @NotNull
    public MCPServer convertToMCPServer(@NotNull MCPRegistryServerInfo serverInfo,
                                        @NotNull Map<String, String> envValues) {
        // Prefer remotes with streamable-http
        if (serverInfo.getRemotes() != null && !serverInfo.getRemotes().isEmpty()) {
            return convertRemoteServer(serverInfo, envValues);
        }

        // Fall back to packages
        if (serverInfo.getPackages() != null && !serverInfo.getPackages().isEmpty()) {
            return convertPackageServer(serverInfo, envValues);
        }

        // Fallback: create a minimal server entry
        return MCPServer.builder()
                .name(serverInfo.getName())
                .transportType(MCPServer.TransportType.HTTP)
                .build();
    }

    @NotNull
    private MCPServer convertRemoteServer(@NotNull MCPRegistryServerInfo serverInfo,
                                          @NotNull Map<String, String> envValues) {
        MCPRegistryRemote remote = serverInfo.getRemotes().get(0);
        Map<String, String> headers = new HashMap<>();

        if (remote.getHeaders() != null) {
            for (MCPRegistryHeader header : remote.getHeaders()) {
                String value = envValues.getOrDefault(header.getName(), header.getValue());
                if (value != null && !value.isBlank()) {
                    headers.put(header.getName(), value);
                }
            }
        }

        return MCPServer.builder()
                .name(serverInfo.getName())
                .transportType(MCPServer.TransportType.HTTP)
                .url(remote.getUrl())
                .headers(headers)
                .build();
    }

    @NotNull
    private MCPServer convertPackageServer(@NotNull MCPRegistryServerInfo serverInfo,
                                           @NotNull Map<String, String> envValues) {
        MCPRegistryPackage pkg = serverInfo.getPackages().get(0);
        Map<String, String> env = new HashMap<>();

        if (pkg.getEnvironmentVariables() != null) {
            for (MCPRegistryEnvVar envVar : pkg.getEnvironmentVariables()) {
                String value = envValues.getOrDefault(envVar.getName(), "");
                if (!value.isBlank()) {
                    env.put(envVar.getName(), value);
                }
            }
        }

        String command;
        List<String> args;
        if ("npm".equalsIgnoreCase(pkg.getRegistryType())) {
            command = "npx";
            args = List.of("-y", pkg.getIdentifier());
        } else if ("oci".equalsIgnoreCase(pkg.getRegistryType())) {
            command = "docker";
            args = List.of("run", "-i", "--rm", pkg.getIdentifier());
        } else {
            // Unknown package type — use identifier as command
            command = pkg.getIdentifier();
            args = List.of();
        }

        return MCPServer.builder()
                .name(serverInfo.getName())
                .transportType(MCPServer.TransportType.STDIO)
                .command(command)
                .args(args)
                .env(env)
                .build();
    }

    /**
     * Determine the display type for a registry server entry.
     */
    @NotNull
    public String getServerType(@NotNull MCPRegistryServerInfo serverInfo) {
        if (serverInfo.getRemotes() != null && !serverInfo.getRemotes().isEmpty()) {
            return "Remote";
        }
        if (serverInfo.getPackages() != null && !serverInfo.getPackages().isEmpty()) {
            MCPRegistryPackage pkg = serverInfo.getPackages().get(0);
            if ("npm".equalsIgnoreCase(pkg.getRegistryType())) {
                return "npm";
            } else if ("oci".equalsIgnoreCase(pkg.getRegistryType())) {
                return "Docker";
            }
            return pkg.getRegistryType();
        }
        return "Unknown";
    }
}
