package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Snapshot of the Hugging Face upload settings (task-280). {@code target} is null until set up. */
public record HfUploadSettings(@Nullable HfDatasetTarget target, boolean isPrivate, @NotNull String pathInRepo,
                               boolean includeProjectName, boolean confirmBeforeUpload, @NotNull String hubUrl) {

    public static final String DEFAULT_DATASET_NAME = "devoxxgenie-traces";
    public static final String DEFAULT_PATH_IN_REPO = "devoxxgenie";

    public HfUploadSettings {
        pathInRepo = normalisePath(pathInRepo);
        hubUrl = hubUrl.isBlank() ? HuggingFaceHubClient.DEFAULT_HUB_URL : hubUrl.trim();
    }

    public static @NotNull HfUploadSettings fromState(@NotNull DevoxxGenieStateService s) {
        return new HfUploadSettings(
                HfDatasetTarget.parse(s.getAgentTraceHfTarget()),
                !Boolean.FALSE.equals(s.getAgentTraceHfPrivate()),
                s.getAgentTraceHfPathInRepo() == null ? DEFAULT_PATH_IN_REPO : s.getAgentTraceHfPathInRepo(),
                !Boolean.FALSE.equals(s.getAgentTraceHfIncludeProjectName()),
                !Boolean.FALSE.equals(s.getAgentTraceHfConfirmBeforeUpload()),
                s.getAgentTraceHfHubUrl() == null ? "" : s.getAgentTraceHfHubUrl());
    }

    public @NotNull HfUploadSettings withTarget(@NotNull HfDatasetTarget newTarget, boolean newPrivate) {
        return new HfUploadSettings(newTarget, newPrivate, pathInRepo, includeProjectName, confirmBeforeUpload, hubUrl);
    }

    /** Path of a conversation's file inside the dataset. */
    public @NotNull String filePath(@NotNull String conversationId) {
        String safeId = conversationId.replaceAll("[^A-Za-z0-9._-]", "_");
        return (pathInRepo.isEmpty() ? "" : pathInRepo + "/") + safeId + ".jsonl";
    }

    static @NotNull String normalisePath(@Nullable String path) {
        if (path == null) {
            return "";
        }
        String p = path.trim().replace('\\', '/').replaceAll("/{2,}", "/");
        p = p.replaceAll("^/+", "").replaceAll("/+$", "");
        // No parent-directory segments in a repo path.
        return p.replaceAll("(^|/)\\.\\.(?=/|$)", "$1_");
    }
}
