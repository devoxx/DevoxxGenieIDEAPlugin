package com.devoxx.genie.ui.listener;

import com.intellij.openapi.project.Project;

/**
 * Interface for listeners that want to be notified when a prompt is submitted.
 *
 * @see com.intellij.openapi.project.Project
 */
public interface PromptSubmissionListener {
    /**
     * Called when a prompt is submitted.
     *
     * @param project the current project
     * @param prompt  the submitted prompt
     */
    void onPromptSubmitted(Project project, String prompt);
}
