package com.devoxx.genie.service;

import com.intellij.openapi.vfs.VirtualFile;

public interface FileListObserver {
    void fileAdded(VirtualFile file);
}
