package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.util.HttpClientProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal Hugging Face Hub client for uploading trace files to a dataset (task-280).
 * Mirrors the protocol of the official {@code @huggingface/hub} JS client ({@code who-am-i.ts},
 * {@code repo-exists.ts}, {@code create-repo.ts}, {@code commit.ts}):
 * <ol>
 *   <li>{@code POST /api/datasets/{repo}/preupload/main} tells, per file, whether it must go through LFS;</li>
 *   <li>LFS files: {@code POST /datasets/{repo}.git/info/lfs/objects/batch}, then a basic {@code PUT}
 *       (or a multipart upload when the server asks for one);</li>
 *   <li>{@code POST /api/datasets/{repo}/commit/main} with an NDJSON body: a header line, then
 *       {@code file} (inline base64) or {@code lfsFile} (sha256 pointer) lines.</li>
 * </ol>
 * The Xet transfer is not offered, so the server falls back to basic/multipart LFS.
 * <p>
 * Uses its own OkHttp client derived from the plugin's shared one but without its retry
 * interceptor, which would retry 401/409 responses. The token is only ever sent in the
 * {@code Authorization} header and never appears in exception messages.
 */
public class HuggingFaceHubClient {

    public static final String DEFAULT_HUB_URL = "https://huggingface.co";
    private static final MediaType JSON_TYPE = MediaType.get("application/json");
    private static final MediaType LFS_TYPE = MediaType.get("application/vnd.git-lfs+json");
    private static final MediaType NDJSON_TYPE = MediaType.get("application/x-ndjson");
    private static final MediaType OCTET = MediaType.get("application/octet-stream");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BRANCH = "main";

    /** A file to commit. */
    public record UploadFile(@NotNull String path, byte @NotNull [] content) {}

    /** Result of a successful commit. */
    public record CommitResult(@Nullable String commitUrl, @Nullable String commitOid) {}

    private final OkHttpClient http;
    private final String hubUrl;

    public HuggingFaceHubClient(@NotNull String hubUrl) {
        this(defaultClient(), hubUrl);
    }

    HuggingFaceHubClient(@NotNull OkHttpClient http, @NotNull String hubUrl) {
        this.http = http;
        this.hubUrl = HfDatasetTarget.stripSlash(hubUrl.isBlank() ? DEFAULT_HUB_URL : hubUrl.trim());
    }

    private static OkHttpClient defaultClient() {
        OkHttpClient.Builder builder = HttpClientProvider.getClient().newBuilder()
                .readTimeout(Duration.ofSeconds(120))
                .writeTimeout(Duration.ofSeconds(120));
        builder.interceptors().clear();
        return builder.build();
    }

    public @NotNull String hubUrl() {
        return hubUrl;
    }

    /** The username the token belongs to. */
    public @NotNull String whoAmI(@NotNull String token) throws HubApiException {
        JsonNode json = callJson(get(hubUrl + "/api/whoami-v2", token), "check the Hugging Face token");
        String name = json.path("name").asText("");
        if (name.isEmpty()) {
            throw new HubApiException(200, "Unexpected response from Hugging Face (no user name).");
        }
        return name;
    }

    public boolean datasetExists(@NotNull HfDatasetTarget target, @NotNull String token) throws HubApiException {
        Request request = get(hubUrl + "/api/datasets/" + target.repoId(), token);
        try (Response response = http.newCall(request).execute()) {
            if (response.code() == 404 || response.code() == 401) {
                return false;
            }
            if (!response.isSuccessful()) {
                throw error(response, "check dataset " + target.repoId());
            }
            return true;
        } catch (IOException e) {
            throw network(e);
        }
    }

    public void createDataset(@NotNull HfDatasetTarget target, @NotNull String token, boolean isPrivate)
            throws HubApiException {
        ObjectNode body = JSON.createObjectNode()
                .put("name", target.name())
                .put("type", "dataset")
                .put("organization", target.namespace())
                .put("visibility", isPrivate ? "private" : "public");
        callJson(post(hubUrl + "/api/repos/create", token, RequestBody.create(body.toString(), JSON_TYPE)),
                "create dataset " + target.repoId());
    }

    /** Commits the files to the dataset's main branch in one commit. */
    public @NotNull CommitResult commit(@NotNull HfDatasetTarget target, @NotNull String token,
                                        @NotNull List<UploadFile> files, @NotNull String title) throws HubApiException {
        Set<String> lfsPaths = preupload(target, token, files);
        List<UploadFile> lfsFiles = files.stream().filter(f -> lfsPaths.contains(f.path())).toList();
        Map<String, String> shas = new java.util.HashMap<>();
        for (UploadFile f : lfsFiles) {
            shas.put(f.path(), sha256(f.content()));
        }
        if (!lfsFiles.isEmpty()) {
            uploadLfs(target, token, lfsFiles, shas);
        }

        // Build each node before appending: ObjectNode.set() returns a generic <T extends JsonNode>,
        // which StringBuilder.append() would infer as CharSequence and fail with a ClassCastException.
        ObjectNode headerLine = JSON.createObjectNode().put("key", "header");
        headerLine.set("value", JSON.createObjectNode().put("summary", title));
        StringBuilder ndjson = new StringBuilder();
        ndjson.append(headerLine.toString()).append('\n');
        for (UploadFile f : files) {
            ObjectNode line = JSON.createObjectNode();
            if (lfsPaths.contains(f.path())) {
                line.put("key", "lfsFile").set("value", JSON.createObjectNode()
                        .put("path", f.path()).put("algo", "sha256")
                        .put("size", f.content().length).put("oid", shas.get(f.path())));
            } else {
                line.put("key", "file").set("value", JSON.createObjectNode()
                        .put("content", Base64.getEncoder().encodeToString(f.content()))
                        .put("path", f.path()).put("encoding", "base64"));
            }
            ndjson.append(line.toString()).append('\n');
        }
        JsonNode json = callJson(post(api(target, "commit"), token, RequestBody.create(ndjson.toString(), NDJSON_TYPE)),
                "commit to " + target.repoId());
        return new CommitResult(textOrNull(json, "commitUrl"), textOrNull(json, "commitOid"));
    }

    private Set<String> preupload(HfDatasetTarget target, String token, List<UploadFile> files) throws HubApiException {
        ArrayNode list = JSON.createArrayNode();
        for (UploadFile f : files) {
            byte[] sample = Arrays.copyOf(f.content(), Math.min(512, f.content().length));
            list.addObject().put("path", f.path()).put("size", f.content().length)
                    .put("sample", Base64.getEncoder().encodeToString(sample));
        }
        ObjectNode body = JSON.createObjectNode();
        body.set("files", list);
        JsonNode json = callJson(post(api(target, "preupload"), token, RequestBody.create(body.toString(), JSON_TYPE)),
                "prepare upload to " + target.repoId());
        Set<String> lfs = new HashSet<>();
        for (JsonNode file : json.path("files")) {
            if ("lfs".equals(file.path("uploadMode").asText())) {
                lfs.add(file.path("path").asText());
            }
        }
        return lfs;
    }

    private void uploadLfs(HfDatasetTarget target, String token, List<UploadFile> files, Map<String, String> shas)
            throws HubApiException {
        ObjectNode body = JSON.createObjectNode().put("operation", "upload").put("hash_algo", "sha_256");
        body.putArray("transfers").add("basic").add("multipart");
        body.putObject("ref").put("name", BRANCH);
        ArrayNode objects = body.putArray("objects");
        Map<String, UploadFile> byOid = new java.util.HashMap<>();
        for (UploadFile f : files) {
            String oid = shas.get(f.path());
            objects.addObject().put("oid", oid).put("size", f.content().length);
            byOid.put(oid, f);
        }
        Request request = new Request.Builder()
                .url(hubUrl + "/datasets/" + target.repoId() + ".git/info/lfs/objects/batch")
                .header("Authorization", "Bearer " + token)
                .header("Accept", LFS_TYPE.toString())
                .post(RequestBody.create(body.toString(), LFS_TYPE))
                .build();
        JsonNode json = callJson(request, "upload large files to " + target.repoId());
        for (JsonNode obj : json.path("objects")) {
            UploadFile file = byOid.get(obj.path("oid").asText());
            if (file == null) {
                throw new HubApiException(200, "Unexpected LFS response from Hugging Face (unknown object).");
            }
            if (obj.hasNonNull("error")) {
                throw new HubApiException(obj.path("error").path("code").asInt(-1),
                        "Hugging Face refused " + file.path() + ": " + obj.path("error").path("message").asText());
            }
            JsonNode upload = obj.path("actions").path("upload");
            if (upload.isMissingNode() || upload.isNull()) {
                continue; // already stored on the Hub
            }
            JsonNode header = upload.path("header");
            if (header.has("chunk_size")) {
                uploadMultipart(file, upload);
            } else {
                Request put = new Request.Builder().url(upload.path("href").asText())
                        .put(RequestBody.create(file.content(), OCTET)).build();
                call(put, "upload " + file.path());
            }
        }
    }

    private void uploadMultipart(UploadFile file, JsonNode upload) throws HubApiException {
        JsonNode header = upload.path("header");
        int chunkSize = Integer.parseInt(header.path("chunk_size").asText());
        List<String> parts = new ArrayList<>();
        for (Iterator<String> it = header.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            if (key.matches("\\d+")) {
                parts.add(key);
            }
        }
        parts.sort(java.util.Comparator.comparingInt(Integer::parseInt));
        byte[] content = file.content();
        if (parts.size() != (content.length + chunkSize - 1) / chunkSize) {
            throw new HubApiException(200, "Unexpected multipart response from Hugging Face for " + file.path());
        }
        ObjectNode complete = JSON.createObjectNode().put("oid", sha256(content));
        ArrayNode done = complete.putArray("parts");
        for (String part : parts) {
            int n = Integer.parseInt(part);
            int from = (n - 1) * chunkSize;
            byte[] chunk = Arrays.copyOfRange(content, from, Math.min(content.length, from + chunkSize));
            Request put = new Request.Builder().url(header.path(part).asText())
                    .put(RequestBody.create(chunk, OCTET)).build();
            try (Response response = http.newCall(put).execute()) {
                if (!response.isSuccessful()) {
                    throw error(response, "upload part " + n + " of " + file.path());
                }
                String etag = response.header("ETag");
                if (etag == null) {
                    throw new HubApiException(response.code(), "No ETag in multipart upload response for " + file.path());
                }
                done.addObject().put("partNumber", n).put("etag", etag);
            } catch (IOException e) {
                throw network(e);
            }
        }
        Request finish = new Request.Builder().url(upload.path("href").asText())
                .header("Accept", LFS_TYPE.toString())
                .post(RequestBody.create(complete.toString(), LFS_TYPE)).build();
        call(finish, "complete upload of " + file.path());
    }

    // ---------------------------------------------------------------------------------------

    private String api(HfDatasetTarget target, String action) {
        return hubUrl + "/api/datasets/" + target.repoId() + "/" + action + "/"
                + URLEncoder.encode(BRANCH, StandardCharsets.UTF_8);
    }

    private static Request get(String url, String token) {
        return new Request.Builder().url(url).header("Authorization", "Bearer " + token).get().build();
    }

    private static Request post(String url, String token, RequestBody body) {
        return new Request.Builder().url(url).header("Authorization", "Bearer " + token).post(body).build();
    }

    private void call(Request request, String what) throws HubApiException {
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw error(response, what);
            }
        } catch (IOException e) {
            throw network(e);
        }
    }

    private JsonNode callJson(Request request, String what) throws HubApiException {
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw error(response, what);
            }
            ResponseBody body = response.body();
            String text = body == null ? "" : body.string();
            return text.isBlank() ? JSON.createObjectNode() : JSON.readTree(text);
        } catch (IOException e) {
            throw network(e);
        }
    }

    /** Turns an error response into a readable message (port of {@code createApiError}). */
    static HubApiException error(Response response, String what) {
        int code = response.code();
        String serverMessage = null;
        try {
            ResponseBody body = response.body();
            String text = body == null ? "" : body.string();
            if (!text.isBlank() && text.trim().startsWith("{")) {
                JsonNode json = JSON.readTree(text);
                serverMessage = json.hasNonNull("error") ? json.get("error").asText()
                        : json.hasNonNull("message") ? json.get("message").asText() : null;
                if (json.hasNonNull("error_description")) {
                    serverMessage = (serverMessage == null ? "" : serverMessage + ": ") + json.get("error_description").asText();
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // fall back to the status-based message
        }
        String hint = switch (code) {
            case 401 -> "The Hugging Face token is missing or invalid.";
            case 403 -> "The Hugging Face token doesn't have write access to this dataset.";
            case 404 -> "Not found on Hugging Face.";
            case 409 -> "Conflict on Hugging Face (the dataset may already exist or was changed meanwhile).";
            case 429 -> "Hugging Face rate limit reached; try again in a minute.";
            default -> "Hugging Face returned HTTP " + code + ".";
        };
        String requestId = response.header("X-Request-Id");
        String message = "Could not " + what + ". " + hint
                + (serverMessage != null && !serverMessage.isBlank() ? " (" + serverMessage + ")" : "")
                + (requestId != null ? " Request ID: " + requestId : "");
        return new HubApiException(code, message);
    }

    private static HubApiException network(IOException e) {
        return new HubApiException("Could not reach Hugging Face: " + e.getMessage(), e);
    }

    private static @Nullable String textOrNull(JsonNode json, String field) {
        return json.hasNonNull(field) ? json.get(field).asText() : null;
    }

    static @NotNull String sha256(byte @NotNull [] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
