package com.devoxx.genie.model.request;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class EditorInfo {

    private String language;
    private String selectedText;
    private List<VirtualFile> selectedFiles;

    public EditorInfo() {
    }

    public EditorInfo(List<VirtualFile> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
}
