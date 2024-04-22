package com.devoxx.genie.listeners;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ApplicationListener implements FileEditorManagerListener {

    private Map<String, VirtualFile> openFiles = new HashMap<>();

    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        openFiles.put(file.getName(), file);
    }

    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        openFiles.remove(file.getName());
    }
}
