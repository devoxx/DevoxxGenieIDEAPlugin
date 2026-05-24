package com.devoxx.genie.service.ap;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves candidate identifiers for the current IDE project, in priority order,
 * for matching against Agentic Platform project names.
 *
 * <p>{@link Project#getName()} can be overridden via {@code .idea/.name} and is sometimes
 * stale, so the base path's directory name is included as a stable fallback.</p>
 */
public final class ApProjectMatcher {

    private ApProjectMatcher() {}

    public static @NotNull List<String> candidateNames(@NotNull Project project) {
        List<String> names = new ArrayList<>(2);
        String ideName = project.getName();
        if (ideName != null && !ideName.isBlank()) {
            names.add(ideName);
        }
        String basePath = project.getBasePath();
        if (basePath != null && !basePath.isBlank()) {
            String dirName = Path.of(basePath).getFileName().toString();
            if (!dirName.isBlank() && !dirName.equalsIgnoreCase(ideName)) {
                names.add(dirName);
            }
        }
        return names;
    }
}
