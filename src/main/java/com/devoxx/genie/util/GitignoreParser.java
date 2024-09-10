package com.devoxx.genie.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
public class GitignoreParser {
    private final List<PathMatcher> matchers = new ArrayList<>();
    private final Path basePath;

    public GitignoreParser(@NotNull Path gitignorePath) throws IOException {
        this.basePath = gitignorePath.getParent();
        List<String> patterns = Files.readAllLines(gitignorePath);
        for (String pattern : patterns) {
            if (!pattern.trim().isEmpty() && !pattern.startsWith("#")) {
                String glob = "glob:" + pattern.trim();
                matchers.add(FileSystems.getDefault().getPathMatcher(glob));
            }
        }
    }

    public boolean matches(Path path) {
        Path relativePath = basePath.relativize(path);
        String pathToMatch = relativePath.toString().replace(File.separatorChar, '/');

        for (PathMatcher matcher : matchers) {
            if (matcher.matches(Paths.get(pathToMatch))) {
                return true;
            }
        }
        return false;
    }
}
