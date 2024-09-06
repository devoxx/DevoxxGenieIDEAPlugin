package com.devoxx.genie.service;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

public interface FileListObserver {
    void fileAdded(VirtualFile file);

    void filesAdded(List<VirtualFile> files);

    void allFilesRemoved();
}

