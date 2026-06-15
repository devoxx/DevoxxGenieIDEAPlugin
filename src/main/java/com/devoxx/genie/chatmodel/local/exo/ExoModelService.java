package com.devoxx.genie.chatmodel.local.exo;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.model.exo.ExoModelDTO;
import com.devoxx.genie.model.exo.ExoModelEntryDTO;
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
    private static final String INSTANCES_KEY = "instances";
    private static final String RUNNERS_KEY = "runners";
    public static final String MODEL_CARD = "modelCard";
    public static final String INSTANCE = "instance";
    public static final String INSTANCE_ID = "instanceId";

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
        Set<String> downloadedModelIds = getDownloadedModelIds(baseUrl);

        if (downloadedModelIds.isEmpty()) {
            throw new IOException("No downloaded models found. Use the Exo dashboard to download models first.");
        }

        List<ExoModelEntryDTO> result = fetchDownloadedModelsFromApi(baseUrl, downloadedModelIds);
        if (result.isEmpty()) {
            result = createFallbackModelEntries(downloadedModelIds);
        }

        return result.toArray(new ExoModelEntryDTO[0]);
    }

    /**
     * Fetches full model metadata from /models and returns only those present in downloadedModelIds.
     */
    private List<ExoModelEntryDTO> fetchDownloadedModelsFromApi(String baseUrl,
                                                                Set<String> downloadedModelIds) throws IOException {
        List<ExoModelEntryDTO> result = new ArrayList<>();
        Request request = new Request.Builder().url(baseUrl + "models").build();
        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                ExoModelDTO dto = gson.fromJson(response.body().string(), ExoModelDTO.class);
                if (dto.getData() != null) {
                    for (ExoModelEntryDTO model : dto.getData()) {
                        if (downloadedModelIds.contains(model.getId())) {
                            result.add(model);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Creates minimal model entries from state info when /models returns no matching results.
     */
    private List<ExoModelEntryDTO> createFallbackModelEntries(Set<String> downloadedModelIds) {
        List<ExoModelEntryDTO> result = new ArrayList<>();
        for (String modelId : downloadedModelIds) {
            ExoModelEntryDTO dto = new ExoModelEntryDTO();
            dto.setId(modelId);
            dto.setName(modelId.contains("/") ? modelId.substring(modelId.indexOf('/') + 1) : modelId);
            dto.setContextLength(0);
            result.add(dto);
        }
        return result;
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
                if (shard.has(MODEL_CARD)) {
                    return shard.getAsJsonObject(MODEL_CARD).get("modelId").getAsString();
                }
            }
        } catch (Exception ignored) {
            // Ignore exception
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
            JsonObject instances = state.getAsJsonObject(INSTANCES_KEY);
            if (instances == null || instances.entrySet().isEmpty()) return false;

            JsonObject runners = state.getAsJsonObject(RUNNERS_KEY);
            for (var entry : instances.entrySet()) {
                String instModelId = extractModelIdFromInstance(entry.getValue());
                if (modelId.equals(instModelId)) {
                    String instanceId = extractInstanceId(entry.getValue());
                    return instanceId != null && areInstanceRunnersReady(instances, runners, instanceId);
                }
            }
        } catch (Exception ignored) {
            // Exo API may be unavailable or return unexpected JSON; treat as not ready
        }
        return false;
    }

    /**
     * Ensures a model instance is running on Exo.
     * Previews placements and creates an instance if none exists.
     */
    public void ensureInstance(String modelId) throws IOException {
        String baseUrl = getExoApiBaseUrl();
        if (!checkExistingInstance(baseUrl, modelId)) {
            createInstance(baseUrl, fetchChosenPreview(baseUrl, modelId));
            waitForInstanceReady(baseUrl, modelId);
        }
    }

    /**
     * Same as ensureInstance but reports progress to an IntelliJ ProgressIndicator.
     */
    public void ensureInstanceWithProgress(String modelId, ProgressIndicator indicator) throws IOException {
        String baseUrl = getExoApiBaseUrl();

        indicator.setText("Checking for existing instance...");
        indicator.setFraction(0.1);

        if (checkExistingInstanceWithProgress(baseUrl, modelId, indicator)) return;

        indicator.setText("Previewing placements across cluster...");
        indicator.setFraction(0.2);
        if (indicator.isCanceled()) return;

        JsonElement chosenPreview = fetchChosenPreview(baseUrl, modelId);

        indicator.setText("Creating instance...");
        indicator.setFraction(0.3);
        if (indicator.isCanceled()) return;

        createInstance(baseUrl, chosenPreview);

        indicator.setText("Loading model across cluster...");
        indicator.setFraction(0.4);

        waitForInstanceReadyWithProgress(baseUrl, modelId, indicator);
    }

    /**
     * Checks if an instance already exists for the given model, and waits for it if not yet ready.
     * Returns true if the instance was found (either ready or waited for), false if no instance exists.
     */
    private boolean checkExistingInstance(String baseUrl, String modelId) throws IOException {
        JsonObject state = fetchState(baseUrl);
        JsonObject instances = state.getAsJsonObject(INSTANCES_KEY);
        if (instances == null) return false;

        for (var entry : instances.entrySet()) {
            if (modelId.equals(extractModelIdFromInstance(entry.getValue()))) {
                String instanceId = extractInstanceId(entry.getValue());
                JsonObject runners = state.getAsJsonObject(RUNNERS_KEY);
                if (instanceId == null || !areInstanceRunnersReady(instances, runners, instanceId)) {
                    waitForInstanceReady(baseUrl, modelId);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an instance already exists for the given model, reporting progress.
     * Returns true if handled (instance found and ready or waited for), false if no instance exists.
     */
    private boolean checkExistingInstanceWithProgress(String baseUrl, String modelId,
                                                      ProgressIndicator indicator) throws IOException {
        JsonObject state = fetchState(baseUrl);
        JsonObject instances = state.getAsJsonObject(INSTANCES_KEY);
        if (instances == null) return false;

        for (var entry : instances.entrySet()) {
            if (modelId.equals(extractModelIdFromInstance(entry.getValue()))) {
                String instanceId = extractInstanceId(entry.getValue());
                JsonObject runners = state.getAsJsonObject(RUNNERS_KEY);
                if (instanceId != null && areInstanceRunnersReady(instances, runners, instanceId)) {
                    indicator.setText("Instance already running");
                    indicator.setFraction(1.0);
                } else {
                    indicator.setText("Waiting for runners to warm up...");
                    indicator.setFraction(0.5);
                    waitForInstanceReadyWithProgress(baseUrl, modelId, indicator);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches cluster placement previews and returns the first valid one, or throws if none found.
     */
    private JsonElement fetchChosenPreview(String baseUrl, String modelId) throws IOException {
        String previewUrl = baseUrl + "instance/previews?model_id=" + modelId;
        Request previewRequest = new Request.Builder().url(previewUrl).build();
        String previewBody;
        try (Response previewResponse = HttpClientProvider.getClient().newCall(previewRequest).execute()) {
            if (!previewResponse.isSuccessful()) {
                throw new IOException("Failed to preview placements for model: " + modelId);
            }
            previewBody = previewResponse.body().string();
        }

        JsonArray previews = gson.fromJson(previewBody, JsonObject.class).getAsJsonArray("previews");
        for (JsonElement p : previews) {
            JsonObject preview = p.getAsJsonObject();
            if (preview.get("error").isJsonNull() && preview.get(INSTANCE) != null && !preview.get(INSTANCE).isJsonNull()) {
                return p;
            }
        }
        throw new IOException("No valid placement found for model: " + modelId +
                ". Ensure enough devices are connected in the Exo cluster.");
    }

    /**
     * Posts the chosen preview to the instance endpoint to create a new Exo instance.
     */
    private void createInstance(String baseUrl, JsonElement chosenPreview) throws IOException {
        Request instanceRequest = new Request.Builder()
                .url(baseUrl + INSTANCE)
                .post(RequestBody.create(chosenPreview.toString(), JSON_TYPE))
                .build();
        try (Response instanceResponse = HttpClientProvider.getClient().newCall(instanceRequest).execute()) {
            if (!instanceResponse.isSuccessful()) {
                throw new IOException("Failed to create Exo instance: " + instanceResponse.body().string());
            }
        }
    }

    private void waitForInstanceReadyWithProgress(String baseUrl, String modelId,
                                                  ProgressIndicator indicator) throws IOException {
        long deadline = System.currentTimeMillis() + (120 * 1000L);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() < deadline) {
            if (indicator.isCanceled()) return;

            JsonObject state = fetchState(baseUrl);
            if (isModelInstanceReady(state, modelId)) {
                indicator.setText("Exo instance ready");
                indicator.setFraction(1.0);
                return;
            }

            double elapsed = (System.currentTimeMillis() - startTime) / (double) (120 * 1000L);
            indicator.setFraction(0.4 + elapsed * 0.55);
            indicator.setText("Warming up model runners...");

            sleepOrThrow();
        }
        throw new IOException("Timed out waiting for Exo runners to become ready");
    }

    private boolean isModelInstanceReady(JsonObject state, String modelId) {
        JsonObject instances = state.getAsJsonObject(INSTANCES_KEY);
        JsonObject runners = state.getAsJsonObject(RUNNERS_KEY);
        if (instances == null) return false;

        for (var entry : instances.entrySet()) {
            if (modelId.equals(extractModelIdFromInstance(entry.getValue()))) {
                String instanceId = extractInstanceId(entry.getValue());
                if (instanceId != null && areInstanceRunnersReady(instances, runners, instanceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sleepOrThrow() throws IOException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Exo runners", e);
        }
    }

    private JsonObject fetchState(String baseUrl) throws IOException {
        String stateUrl = baseUrl + "state";
        Request request = new Request.Builder().url(stateUrl).build();
        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
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
                if (inner.has(INSTANCE_ID)) {
                    return inner.get(INSTANCE_ID).getAsString();
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

        Set<String> instanceRunnerIds = collectInstanceRunnerIds(instances, instanceId);
        if (instanceRunnerIds.isEmpty()) return false;

        return instanceRunnerIds.stream().allMatch(runnerId -> isRunnerReady(runners, runnerId));
    }

    /**
     * Collects runner IDs from the nodeToRunner mapping for the given instanceId.
     */
    private Set<String> collectInstanceRunnerIds(JsonObject instances, String instanceId) {
        Set<String> runnerIds = new HashSet<>();
        for (var entry : instances.entrySet()) {
            if (!instanceId.equals(extractInstanceId(entry.getValue()))) continue;
            try {
                JsonObject inst = entry.getValue().getAsJsonObject();
                for (var inner : inst.entrySet()) {
                    collectRunnerIdsFromShard(inner.getValue(), runnerIds);
                }
            } catch (Exception ignored) {
                // Ignore exception
            }
            break;
        }
        return runnerIds;
    }

    /**
     * Adds runner IDs from a shard's nodeToRunner mapping into the provided set.
     */
    private void collectRunnerIdsFromShard(JsonElement shardElement, Set<String> runnerIds) {
        try {
            JsonObject shardAssignments = shardElement.getAsJsonObject().getAsJsonObject("shardAssignments");
            if (shardAssignments == null) return;
            JsonObject nodeToRunner = shardAssignments.getAsJsonObject("nodeToRunner");
            if (nodeToRunner == null) return;
            for (var nr : nodeToRunner.entrySet()) {
                runnerIds.add(nr.getValue().getAsString());
            }
        } catch (Exception ignored) {
            // Ignore exception
        }
    }

    /**
     * Returns true if the runner with the given ID is in a ready or running state.
     */
    private boolean isRunnerReady(JsonObject runners, String runnerId) {
        JsonElement runner = runners.get(runnerId);
        if (runner == null) return false;
        JsonObject r = runner.getAsJsonObject();
        return r.has("RunnerReady") || r.has("RunnerRunning");
    }

    /**
     * Waits for the instance running the given model to have all its runners ready.
     */
    private void waitForInstanceReady(String baseUrl, String modelId) throws IOException {
        long deadline = System.currentTimeMillis() + (120 * 1000L);

        while (System.currentTimeMillis() < deadline) {
            JsonObject state = fetchState(baseUrl);
            if (isModelInstanceReady(state, modelId)) {
                return;
            }
            sleepOrThrow();
        }
        throw new IOException("Timed out waiting for Exo runners to become ready");
    }
}
