package com.devoxx.genie.ui.listener;

import com.intellij.openapi.project.Project;

public interface PromptSubmissionListener {
    void onPromptSubmitted(Project project, String prompt);
}
