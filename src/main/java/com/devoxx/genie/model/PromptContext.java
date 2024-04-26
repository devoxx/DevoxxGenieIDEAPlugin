package com.devoxx.genie.model;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Setter
@Getter
public class PromptContext {
    private String prompt;
    private String language;
    private String text;
    private List<VirtualFile> files;
}
