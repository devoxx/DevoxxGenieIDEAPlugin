package com.devoxx.genie.service.trace.hf;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * A Hugging Face dataset to upload traces to, {@code namespace/name} (task-280). Accepts the
 * forms {@code user/name}, {@code datasets/user/name}, {@code hf://datasets/user/name} and a
 * {@code https://huggingface.co/datasets/user/name} URL. Buckets are not supported yet.
 */
public record HfDatasetTarget(@NotNull String namespace, @NotNull String name) {

    /** A namespace or name: letters, digits, "_", "." and "-", but not only dots ("." or ".."). */
    private static final Pattern PART = Pattern.compile("(?!\\.+$)[\\w.-]+");

    public static @Nullable HfDatasetTarget parse(@Nullable String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim()
                .replaceFirst("^hf://", "")
                .replaceFirst("^https?://(www\\.)?huggingface\\.co/", "")
                .replaceFirst("/+$", "");
        if (s.startsWith("buckets/")) {
            return null;
        }
        s = s.replaceFirst("^datasets/", "");
        String[] parts = s.split("/");
        if (parts.length != 2 || !PART.matcher(parts[0]).matches() || !PART.matcher(parts[1]).matches()) {
            return null;
        }
        return new HfDatasetTarget(parts[0], parts[1]);
    }

    /** {@code namespace/name}, as the Hub API expects it. */
    public @NotNull String repoId() {
        return namespace + "/" + name;
    }

    public @NotNull String browserUrl(@NotNull String hubUrl) {
        return stripSlash(hubUrl) + "/datasets/" + repoId();
    }

    /** Link to one file of the dataset on the Hub (the page offers the trace viewer for STS files). */
    public @NotNull String fileUrl(@NotNull String hubUrl, @NotNull String path) {
        return browserUrl(hubUrl) + "/blob/main/" + path;
    }

    @Override
    public String toString() {
        return "datasets/" + repoId();
    }

    static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
