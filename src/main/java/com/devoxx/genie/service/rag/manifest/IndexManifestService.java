package com.devoxx.genie.service.rag.manifest;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Hands out the {@link IndexManifest} for a given project, caching one per
 * {@code project.getLocationHash()}. Manifests live alongside the per-project ChromaDB
 * volume — both under {@code {systemPath}/DevoxxGenie/} — so a clean re-index can be
 * performed by simply wiping that directory.
 */
@Service
public final class IndexManifestService {

    private final ConcurrentMap<String, IndexManifest> perProject = new ConcurrentHashMap<>();

    @NotNull
    public static IndexManifestService getInstance() {
        return ApplicationManager.getApplication().getService(IndexManifestService.class);
    }

    @NotNull
    public IndexManifest forProject(@NotNull Project project) {
        return perProject.computeIfAbsent(project.getLocationHash(),
                hash -> new JsonFileIndexManifest(manifestPath(hash)));
    }

    /** Visible for tests that want to swap in an {@link InMemoryIndexManifest}. */
    public void overrideForProject(@NotNull Project project, @NotNull IndexManifest manifest) {
        perProject.put(project.getLocationHash(), manifest);
    }

    public static Path manifestPath(@NotNull String projectLocationHash) {
        return Paths.get(PathManager.getSystemPath(), "DevoxxGenie",
                "index-manifest-" + projectLocationHash + ".json");
    }
}
