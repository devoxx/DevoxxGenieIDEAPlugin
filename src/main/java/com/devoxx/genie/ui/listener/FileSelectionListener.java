package com.devoxx.genie.ui.listener;

import com.intellij.openapi.vfs.VirtualFile;

public interface FileSelectionListener {
    void fileSelected(VirtualFile selectedFile);
}
