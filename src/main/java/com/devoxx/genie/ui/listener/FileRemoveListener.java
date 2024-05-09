package com.devoxx.genie.ui.listener;

import com.intellij.openapi.vfs.VirtualFile;

public interface FileRemoveListener {

    void onFileRemoved(VirtualFile file);
}


