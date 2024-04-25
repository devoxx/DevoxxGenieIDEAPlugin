package com.devoxx.genie.ui.component;

import com.intellij.openapi.vfs.VirtualFile;

public interface FileRemoveListener {
    void onFileRemoved(VirtualFile file);
}
