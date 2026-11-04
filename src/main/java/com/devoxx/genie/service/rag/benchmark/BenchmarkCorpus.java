package com.devoxx.genie.service.rag.benchmark;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chooses which repository files the benchmark indexes, approximating the plugin's project
 * scanner outside the IDE: included extensions and excluded directory names (by default the
 * plugin's own factory defaults), plus dataset-specific path prefixes to leave out.
 *
 * <p>The input is a list of repo-relative paths, normally from {@code git ls-files}, which
 * already honours {@code .gitignore} the way the plugin's scanner does by default.
 */
public final class BenchmarkCorpus {

    private BenchmarkCorpus() {}

    public static @NotNull List<String> select(@NotNull List<String> repoRelativePaths,
                                               @NotNull List<String> includedExtensions,
                                               @NotNull List<String> excludedDirectories,
                                               @NotNull List<String> excludedPathPrefixes) {
        Set<String> extensions = includedExtensions.stream()
                .map(e -> e.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> dirs = Set.copyOf(excludedDirectories);

        List<String> kept = new ArrayList<>();
        for (String path : repoRelativePaths) {
            String normalized = path.replace('\\', '/');
            if (!extensions.contains(extensionOf(normalized))) continue;
            if (inExcludedDirectory(normalized, dirs)) continue;
            if (excludedPathPrefixes.stream().anyMatch(normalized::startsWith)) continue;
            kept.add(path);
        }
        return kept;
    }

    private static boolean inExcludedDirectory(String path, Set<String> dirs) {
        String[] segments = path.split("/");
        // The last segment is the file name, not a directory.
        for (int i = 0; i < segments.length - 1; i++) {
            if (dirs.contains(segments[i])) return true;
        }
        return false;
    }

    private static String extensionOf(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
