package com.devoxx.genie.service.gitdiff;

import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.diff.DiffManagerEx;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContentImpl;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GitMergeService {

    private static final Logger LOG = Logger.getInstance(GitMergeService.class);

    @NotNull
    public static GitMergeService getInstance() {
        return ApplicationManager.getApplication().getService(GitMergeService.class);
    }

    /**
     * Git diff view
     * @param project the project
     */
    public void showDiffView(Project project, VirtualFile originalFile, String suggestedContent) {

        ApplicationManager.getApplication().runReadAction(() -> {
            if (originalFile == null) {
                NotificationUtil.sendNotification(project, "Files not found");
                return;
            }

            if (suggestedContent == null || suggestedContent.isEmpty()) {
                NotificationUtil.sendNotification(project, "Suggested content is empty");
                return;
            }
            Document originalContent = com.intellij.openapi.fileEditor.FileDocumentManager
                .getInstance()
                .getDocument(originalFile);

            if (originalContent == null) {
                NotificationUtil.sendNotification(project, "Error reading file: " + originalFile.getName());
                return;
            }

            DiffContent content1 = new DocumentContentImpl(originalContent);
            DiffContent content2 = new DocumentContentImpl(new DocumentImpl(suggestedContent));

            DiffRequest diffRequest = new SimpleDiffRequest(
                "Diff",
                List.of(content1, content2),
                List.of("Original code", "LLM suggested")
            );

            ApplicationManager.getApplication().invokeLater(() ->
                DiffManagerEx.getInstance().showDiff(project, diffRequest));
        });
    }
}
