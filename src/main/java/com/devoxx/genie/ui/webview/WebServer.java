package com.devoxx.genie.ui.webview;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static com.devoxx.genie.ui.webview.template.ResourceLoader.loadResource;

@Slf4j
@SuppressWarnings("java:S6548") // Singleton is intentional — single web server instance per IDE
public class WebServer {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static WebServer instance;
    private int port = -1;
    private HttpServer httpServer;
    private ExecutorService executor;
    private final Map<String, String> resources = new ConcurrentHashMap<>();
    private final Map<String, String> scripts = new ConcurrentHashMap<>();

    private WebServer() {
        initializeEmbeddedResources();
    }

    public static synchronized WebServer getInstance() {
        if (instance == null) {
            instance = new WebServer();
        }
        return instance;
    }

    public void start() {
        if (isRunning()) return;

        try {
            port = findAvailablePort();
            httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            httpServer.createContext("/", this::handleRequest);
            executor = Executors.newCachedThreadPool();
            httpServer.setExecutor(executor);
            httpServer.start();
            log.info("Web server started on port {}", port);
        } catch (Exception e) {
            log.error("Failed to start web server", e);
            stop();
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        port = -1;
    }

    public boolean isRunning() {
        return httpServer != null;
    }

    public String getServerUrl() {
        return "http://localhost:" + port;
    }

    public String addDynamicResource(@NotNull String content) {
        String resourceId = "/dynamic/" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        log.info("Adding dynamic resource: {} - content length: {}", resourceId, content.length());
        resources.put(resourceId, content);
        return resourceId;
    }

    public String getResourceUrl(String resourcePath) {
        return getServerUrl() + resourcePath;
    }

    /**
     * Add a dynamic JavaScript script that can be injected into web views.
     *
     * @param scriptId unique identifier for the script
     * @param content  JavaScript content
     */
    public void addDynamicScript(@NotNull String scriptId, @NotNull String content) {
        String resourcePath = "/scripts/" + scriptId + ".js";
        log.info("Adding dynamic script: {}, content length: {}", resourcePath, content.length());
        resources.put(resourcePath, content);
        scripts.put(scriptId, resourcePath);
    }

    /**
     * Get the URL for a previously registered script.
     *
     * @param scriptId the script identifier
     * @return URL to the script, or null if not found
     */
    public String getScriptUrl(String scriptId) {
        String path = scripts.get(scriptId);
        return path != null ? getServerUrl() + path : null;
    }

    private int findAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            log.error("Failed to find available port", e);
            return 8090; // Fallback port
        }
    }

    public void initializeEmbeddedResources() {
        addStaticResource("/icons/copy.svg", "icons/copy.svg");
        addStaticResource("/icons/copy_dark.svg", "icons/copy_dark.svg");
    }

    /**
     * Add a static resource from the resources directory to be served by the web server.
     *
     * @param uriPath the URI path to serve the resource at
     * @param resourcePath the path to the resource in the resources directory
     */
    public void addStaticResource(String uriPath, String resourcePath) {
        try {
            String content = loadResource(resourcePath);
            if (!content.isEmpty()) {
                resources.put(uriPath, content);
                log.info("Added static resource: {} from {}", uriPath, resourcePath);
            } else {
                log.warn("Failed to load static resource: {}", resourcePath);
            }
        } catch (Exception e) {
            log.error("Error loading static resource: " + resourcePath, e);
        }
    }

    private void handleRequest(@NotNull HttpExchange exchange) throws IOException {
        try {
            // getPath() already excludes any query string
            String uri = exchange.getRequestURI().getPath();
            log.info("Handling request for: " + uri);

            if ("/health-check".equals(uri)) {
                handleHealthCheckRequest(exchange);
                return;
            }

            if (resources.containsKey(uri)) {
                String content = resources.get(uri);
                Headers headers = exchange.getResponseHeaders();
                String contentType = getContentType(uri);
                log.info("Serving content with type: {}", contentType);
                headers.set(HEADER_CONTENT_TYPE, contentType);
                addCorsHeaders(headers);
                writeResponse(exchange, 200, content);
            } else {
                log.warn("Resource not found: {}", uri);
                exchange.getResponseHeaders().set(HEADER_CONTENT_TYPE, "text/plain");
                writeResponse(exchange, 404, "Resource not found: " + uri);
            }
        } catch (Exception e) {
            log.error("Exception in web server handler", e);
        } finally {
            exchange.close();
        }
    }

    /**
     * Handle health check requests to verify server connectivity.
     */
    private void handleHealthCheckRequest(@NotNull HttpExchange exchange) {
        try {
            String healthStatus = "{\"status\":\"ok\",\"timestamp\":" + System.currentTimeMillis() + "}";
            Headers headers = exchange.getResponseHeaders();
            headers.set(HEADER_CONTENT_TYPE, "application/json");
            addCorsHeaders(headers);

            // Add cache control to prevent caching
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");

            writeResponse(exchange, 200, healthStatus);
            log.debug("Health check response sent");
        } catch (Exception e) {
            log.error("Error handling health check request", e);
            try {
                exchange.getResponseHeaders().set(HEADER_CONTENT_TYPE, "application/json");
                writeResponse(exchange, 500, "{\"status\":\"error\"}");
            } catch (IOException ioe) {
                log.error("Failed to send health check error response", ioe);
            }
        }
    }

    private void addCorsHeaders(@NotNull Headers headers) {
        headers.set(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        headers.set(HEADER_ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
        headers.set(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
    }

    private void writeResponse(@NotNull HttpExchange exchange, int statusCode, @NotNull String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private @NotNull String getContentType(@NotNull String uri) {
        if (uri.endsWith(".js")) {
            return "application/javascript";
        } else if (uri.endsWith(".css")) {
            return "text/css";
        } else if (uri.endsWith(".html") || uri.startsWith("/dynamic/")) {
            return "text/html";
        } else if (uri.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "text/plain";
        }
    }
}
