package com.devoxx.genie.service.gitdiff;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffManagerImpl;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.contents.DocumentContentImpl;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.requests.TextMergeRequestImpl;
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
     * Shows a three-way merge view with:
     * - Left: Original code
     * - Center: Merge result (initially empty)
     * - Right: LLM's modified version
     */
    public void showMerge(@NotNull Project project,
                          @NotNull String originalContent,
                          @NotNull String modifiedContent,
                          @NotNull String title,
                          @NotNull Document targetDocument) {
        DevoxxGenieStateService instance = DevoxxGenieStateService.getInstance();
        if (!instance.getUseSimpleDiff() && !instance.getUseDiffMerge()) {
            LOG.info("Diff view is disabled");
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            DiffContentFactory factory = DiffContentFactory.getInstance();

            // Create contents for three-way merge
            DocumentContent originalContentDoc = factory.create(project, originalContent);

            DocumentContent modifiedContent1 = factory.create(project, modifiedContent);
            DocumentContent targetDocumentContent = factory.create(project, targetDocument);

            Document originalDocument = originalContentDoc.getDocument();

            TextMergeRequestImpl request = new TextMergeRequestImpl(
                project,
                targetDocumentContent,
                originalDocument.getCharsSequence(),
                List.of(originalContentDoc, modifiedContent1, targetDocumentContent),
                title,
                List.of("Original Code", "LLM Modified Code", "Merge Result")
            );

            // Show the merge dialog
            DiffManager.getInstance().showMerge(project, request);
        });
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
                DiffManagerImpl.getInstance().showDiff(project, diffRequest));
        });
    }
}
