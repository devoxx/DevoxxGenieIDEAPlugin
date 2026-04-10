package com.devoxx.genie.chatmodel.local.exo;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.model.exo.ExoModelDTO;
import com.devoxx.genie.model.exo.ExoModelEntryDTO;
import com.devoxx.genie.service.exception.UnsuccessfulRequestException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class ExoModelService implements LocalLLMProvider {

    private static final Gson gson = new Gson();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json");

    /**
     * Returns the Exo API base URL (without /v1/ suffix).
     * The settings URL includes /v1/ for Langchain4j OpenAI compatibility,
     * but the Exo management API endpoints (/state, /models, /instance) are at the root.
     */
    private static String getExoApiBaseUrl() {
        String url = DevoxxGenieStateService.getInstance().getExoModelUrl();
        if (url.endsWith("/v1/")) {
            url = url.substring(0, url.length() - 3); // Remove "v1/"
        } else if (url.endsWith("/v1")) {
            url = url.substring(0, url.length() - 2); // Remove "v1"
        }
        return ensureEndsWithSlash(url);
    }

    @NotNull
    public static ExoModelService getInstance() {
        return ApplicationManager.getApplication().getService(ExoModelService.class);
    }

    /**
     * Returns only models that are downloaded (on disk) across all Exo cluster nodes.
     * Uses the /state endpoint's downloads section to find DownloadCompleted models,
     * then enriches with metadata from /models.
     */
    @Override
    public ExoModelEntryDTO[] getModels() throws IOException {
        String baseUrl = getExoApiBaseUrl();

        // Get downloaded model IDs from /state (DownloadCompleted entries)
        Set<String> downloadedModelIds = getDownloadedModelIds(baseUrl);

        if (downloadedModelIds.isEmpty()) {
            throw new IOException("No downloaded models found. Use the Exo dashboard to download models first.");
        }

        // Fetch full model metadata from /models and filter to only downloaded ones
        List<ExoModelEntryDTO> result = new ArrayList<>();
        String url = baseUrl + "models";
        Request request = new Request.Builder().url(url).build();
        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                ExoModelDTO dto = gson.fromJson(json, ExoModelDTO.class);
                if (dto.getData() != null) {
                    for (ExoModelEntryDTO model : dto.getData()) {
                        if (downloadedModelIds.contains(model.getId())) {
                            result.add(model);
                        }
                    }
                }
            }
        }

        // If /models didn't match, create entries from state info
        if (result.isEmpty()) {
            for (String modelId : downloadedModelIds) {
                ExoModelEntryDTO dto = new ExoModelEntryDTO();
                dto.setId(modelId);
                dto.setName(modelId.contains("/") ? modelId.substring(modelId.indexOf('/') + 1) : modelId);
                dto.setContextLength(0);
                result.add(dto);
            }
        }

        return result.toArray(new ExoModelEntryDTO[0]);
    }

    /**
     * Extracts model IDs that have DownloadCompleted status from /state.
     */
    private Set<String> getDownloadedModelIds(String baseUrl) throws IOException {
        Set<String> modelIds = new HashSet<>();
        JsonObject state = fetchState(baseUrl);
        JsonObject downloads = state.getAsJsonObject("downloads");
        if (downloads == null) return modelIds;

        for (var nodeEntry : downloads.entrySet()) {
            JsonArray nodeDownloads = nodeEntry.getValue().getAsJsonArray();
            for (JsonElement dl : nodeDownloads) {
                JsonObject dlObj = dl.getAsJsonObject();
                if (dlObj.has("DownloadCompleted")) {
                    JsonObject completed = dlObj.getAsJsonObject("DownloadCompleted");
                    String modelId = extractModelIdFromDownload(completed);
                    if (modelId != null) {
                        modelIds.add(modelId);
                    }
                }
            }
        }
        return modelIds;
    }

    private String extractModelIdFromDownload(JsonObject downloadInfo) {
        try {
            JsonObject shardMetadata = downloadInfo.getAsJsonObject("shardMetadata");
            if (shardMetadata == null) return null;
            for (var entry : shardMetadata.entrySet()) {
                JsonObject shard = entry.getValue().getAsJsonObject();
                if (shard.has("modelCard")) {
                    return shard.getAsJsonObject("modelCard").get("modelId").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Quick check if an instance for the given model is currently running and has ready runners.
     * Used to detect when exo has recycled/disconnected an instance.
     */
    public boolean isInstanceRunning(String modelId) {
        try {
            String baseUrl = getExoApiBaseUrl();
            JsonObject state = fetchState(baseUrl);
            JsonObject instances = state.getAsJsonObject("instances");
            if (instances == null || instances.entrySet().isEmpty()) return false;

            JsonObject runners = state.getAsJsonObject("runners");
            for (var entry : instances.entrySet()) {
                String instModelId = extractModelIdFromInstance(entry.getValue());
                if (modelId.equals(instModelId)) {
                    String instanceId = extractInstanceId(entry.getValue());
                    return instanceId != null && areInstanceRunnersReady(instances, runners, instanceId);
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Ensures a model instance is running on Exo.
     * Previews placements and creates an instance if none exists.
     */
    public void ensureInstance(String modelId) throws IOException {
        String baseUrl = getExoApiBaseUrl();

        // Check if an instance already exists for this model
        JsonObject state = fetchState(baseUrl);
        JsonObject instances = state.getAsJsonObject("instances");
        if (instances != null) {
            for (var entry : instances.entrySet()) {
                String instanceModelId = extractModelIdFromInstance(entry.getValue());
                if (modelId.equals(instanceModelId)) {
                    // Instance exists — check if its runners are ready
                    String instanceId = extractInstanceId(entry.getValue());
                    JsonObject runners = state.getAsJsonObject("runners");
                    if (instanceId != null && areInstanceRunnersReady(instances, runners, instanceId)) {
                        return; // Instance exists and is ready
                    }
                    // Instance exists but runners not ready — wait for them
                    waitForInstanceReady(baseUrl, modelId, 120);
                    return;
                }
            }
        }

        // Preview placements
        String previewUrl = baseUrl + "instance/previews?model_id=" + modelId;
        Request previewRequest = new Request.Builder().url(previewUrl).build();
        String previewBody;
        try (Response previewResponse = HttpClientProvider.getClient().newCall(previewRequest).execute()) {
            if (!previewResponse.isSuccessful() || previewResponse.body() == null) {
                throw new IOException("Failed to preview placements for model: " + modelId);
            }
            previewBody = previewResponse.body().string();
        }

        // Find first valid placement
        JsonObject previewObj = gson.fromJson(previewBody, JsonObject.class);
        JsonArray previews = previewObj.getAsJsonArray("previews");
        JsonElement chosenPreview = null;

        for (JsonElement p : previews) {
            JsonObject preview = p.getAsJsonObject();
            if (preview.get("error").isJsonNull() && preview.get("instance") != null && !preview.get("instance").isJsonNull()) {
                chosenPreview = p;
                break;
            }
        }

        if (chosenPreview == null) {
            throw new IOException("No valid placement found for model: " + modelId +
                    ". Ensure enough devices are connected in the Exo cluster.");
        }

        // Create instance
        String instanceUrl = baseUrl + "instance";
        RequestBody body = RequestBody.create(chosenPreview.toString(), JSON_TYPE);
        Request instanceRequest = new Request.Builder().url(instanceUrl).post(body).build();

        try (Response instanceResponse = HttpClientProvider.getClient().newCall(instanceRequest).execute()) {
            if (!instanceResponse.isSuccessful()) {
                String errorBody = instanceResponse.body() != null ? instanceResponse.body().string() : "unknown";
                throw new IOException("Failed to create Exo instance: " + errorBody);
            }
        }

        // Wait for the instance runners to be ready
        waitForInstanceReady(baseUrl, modelId, 120);
    }

    /**
     * Same as ensureInstance but reports progress to an IntelliJ ProgressIndicator.
     */
    public void ensureInstanceWithProgress(String modelId, ProgressIndicator indicator) throws IOException {
        String baseUrl = getExoApiBaseUrl();

        indicator.setText("Checking for existing instance...");
        indicator.setFraction(0.1);

        // Check if an instance already exists for this model
        JsonObject state = fetchState(baseUrl);
        JsonObject instances = state.getAsJsonObject("instances");
        if (instances != null) {
            for (var entry : instances.entrySet()) {
                String instanceModelId = extractModelIdFromInstance(entry.getValue());
                if (modelId.equals(instanceModelId)) {
                    String instanceId = extractInstanceId(entry.getValue());
                    JsonObject runners = state.getAsJsonObject("runners");
                    if (instanceId != null && areInstanceRunnersReady(instances, runners, instanceId)) {
                        indicator.setText("Instance already running");
                        indicator.setFraction(1.0);
                        return;
                    }
                    indicator.setText("Waiting for runners to warm up...");
                    indicator.setFraction(0.5);
                    waitForInstanceReadyWithProgress(baseUrl, modelId, 120, indicator);
                    return;
                }
            }
        }

        indicator.setText("Previewing placements across cluster...");
        indicator.setFraction(0.2);
        if (indicator.isCanceled()) return;

        // Preview placements
        String previewUrl = baseUrl + "instance/previews?model_id=" + modelId;
        Request previewRequest = new Request.Builder().url(previewUrl).build();
        String previewBody;
        try (Response previewResponse = HttpClientProvider.getClient().newCall(previewRequest).execute()) {
            if (!previewResponse.isSuccessful() || previewResponse.body() == null) {
                throw new IOException("Failed to preview placements for model: " + modelId);
            }
            previewBody = previewResponse.body().string();
        }

        JsonObject previewObj = gson.fromJson(previewBody, JsonObject.class);
        JsonArray previews = previewObj.getAsJsonArray("previews");
        JsonElement chosenPreview = null;
        for (JsonElement p : previews) {
            JsonObject preview = p.getAsJsonObject();
            if (preview.get("error").isJsonNull() && preview.get("instance") != null && !preview.get("instance").isJsonNull()) {
                chosenPreview = p;
                break;
            }
        }

        if (chosenPreview == null) {
            throw new IOException("No valid placement found for model: " + modelId +
                    ". Ensure enough devices are connected in the Exo cluster.");
        }

        indicator.setText("Creating instance...");
        indicator.setFraction(0.3);
        if (indicator.isCanceled()) return;

        // Create instance
        String instanceUrl = baseUrl + "instance";
        RequestBody body = RequestBody.create(chosenPreview.toString(), JSON_TYPE);
        Request instanceRequest = new Request.Builder().url(instanceUrl).post(body).build();

        try (Response instanceResponse = HttpClientProvider.getClient().newCall(instanceRequest).execute()) {
            if (!instanceResponse.isSuccessful()) {
                String errorBody = instanceResponse.body() != null ? instanceResponse.body().string() : "unknown";
                throw new IOException("Failed to create Exo instance: " + errorBody);
            }
        }

        indicator.setText("Loading model across cluster...");
        indicator.setFraction(0.4);

        waitForInstanceReadyWithProgress(baseUrl, modelId, 120, indicator);
    }

    private void waitForInstanceReadyWithProgress(String baseUrl, String modelId,
                                                   int timeoutSeconds, ProgressIndicator indicator) throws IOException {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() < deadline) {
            if (indicator.isCanceled()) return;

            JsonObject state = fetchState(baseUrl);
            JsonObject instances = state.getAsJsonObject("instances");
            JsonObject runners = state.getAsJsonObject("runners");

            if (instances != null) {
                for (var entry : instances.entrySet()) {
                    String instModelId = extractModelIdFromInstance(entry.getValue());
                    if (modelId.equals(instModelId)) {
                        String instanceId = extractInstanceId(entry.getValue());
                        if (instanceId != null && areInstanceRunnersReady(instances, runners, instanceId)) {
                            indicator.setText("Exo instance ready");
                            indicator.setFraction(1.0);
                            return;
                        }
                    }
                }
            }

            // Update progress (0.4 to 0.95 over the timeout period)
            double elapsed = (System.currentTimeMillis() - startTime) / (double) (timeoutSeconds * 1000L);
            indicator.setFraction(0.4 + elapsed * 0.55);
            indicator.setText("Warming up model runners...");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for Exo runners", e);
            }
        }
        throw new IOException("Timed out waiting for Exo runners to become ready");
    }

    private JsonObject fetchState(String baseUrl) throws IOException {
        String stateUrl = baseUrl + "state";
        Request request = new Request.Builder().url(stateUrl).build();
        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to fetch Exo state");
            }
            return gson.fromJson(response.body().string(), JsonObject.class);
        }
    }

    private String extractModelIdFromInstance(JsonElement instanceValue) {
        try {
            JsonObject inst = instanceValue.getAsJsonObject();
            for (var entry : inst.entrySet()) {
                JsonObject inner = entry.getValue().getAsJsonObject();
                JsonObject shardAssignments = inner.getAsJsonObject("shardAssignments");
                if (shardAssignments != null) {
                    return shardAssignments.get("modelId").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractInstanceId(JsonElement instanceValue) {
        try {
            JsonObject inst = instanceValue.getAsJsonObject();
            for (var entry : inst.entrySet()) {
                JsonObject inner = entry.getValue().getAsJsonObject();
                if (inner.has("instanceId")) {
                    return inner.get("instanceId").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Checks if the runners belonging to a specific instance are all ready.
     * Only looks at runners mapped to this instance via nodeToRunner, ignoring stale runners.
     */
    private boolean areInstanceRunnersReady(JsonObject instances, JsonObject runners, String instanceId) {
        if (runners == null || instances == null) return false;

        // Find runner IDs belonging to this instance
        Set<String> instanceRunnerIds = new HashSet<>();
        for (var entry : instances.entrySet()) {
            String iid = extractInstanceId(entry.getValue());
            if (instanceId.equals(iid)) {
                try {
                    JsonObject inst = entry.getValue().getAsJsonObject();
                    for (var inner : inst.entrySet()) {
                        JsonObject shardAssignments = inner.getValue().getAsJsonObject().getAsJsonObject("shardAssignments");
                        if (shardAssignments != null) {
                            JsonObject nodeToRunner = shardAssignments.getAsJsonObject("nodeToRunner");
                            if (nodeToRunner != null) {
                                for (var nr : nodeToRunner.entrySet()) {
                                    instanceRunnerIds.add(nr.getValue().getAsString());
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                break;
            }
        }

        if (instanceRunnerIds.isEmpty()) return false;

        // Check only those runners
        return instanceRunnerIds.stream().allMatch(runnerId -> {
            JsonElement runner = runners.get(runnerId);
            if (runner == null) return false;
            JsonObject r = runner.getAsJsonObject();
            return r.has("RunnerReady") || r.has("RunnerRunning");
        });
    }

    /**
     * Waits for the instance running the given model to have all its runners ready.
     */
    private void waitForInstanceReady(String baseUrl, String modelId, int timeoutSeconds) throws IOException {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            JsonObject state = fetchState(baseUrl);
            JsonObject instances = state.getAsJsonObject("instances");
            JsonObject runners = state.getAsJsonObject("runners");

            if (instances != null) {
                for (var entry : instances.entrySet()) {
                    String instModelId = extractModelIdFromInstance(entry.getValue());
                    if (modelId.equals(instModelId)) {
                        String instanceId = extractInstanceId(entry.getValue());
                        if (instanceId != null && areInstanceRunnersReady(instances, runners, instanceId)) {
                            return;
                        }
                    }
                }
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for Exo runners", e);
            }
        }
        throw new IOException("Timed out waiting for Exo runners to become ready");
    }
}
