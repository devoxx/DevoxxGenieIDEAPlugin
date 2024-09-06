package com.devoxx.genie.service.projectscanner;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Getter
public class UniqueDirectoryScannerService {

    private final Map<String, VirtualFile> uniqueDirectories = new HashMap<>();

    public void addDirectory(@NotNull VirtualFile directoryPath) {
        Path normalizedPath = Paths.get(directoryPath.getPath()).normalize().toAbsolutePath();
        uniqueDirectories.put(normalizedPath.toString(), directoryPath);
    }

    public Optional<VirtualFile> getHighestCommonRoot() {
        Set<String> strings = uniqueDirectories.keySet();
        if (strings.isEmpty()) {
            return Optional.empty();
        }
        List<String> sorted = new ArrayList<>(strings);
        sorted.sort(Comparator.comparingInt(String::length));
        String first = sorted.get(0);
        return Optional.of(uniqueDirectories.get(first));
    }
}
