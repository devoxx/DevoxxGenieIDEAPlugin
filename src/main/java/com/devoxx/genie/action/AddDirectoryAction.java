package com.devoxx.genie.action;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.devoxx.genie.ui.util.WindowPluginUtil.ensureToolWindowVisible;

public class AddDirectoryAction extends DumbAwareAction {

    private static final String ADD_TO_CONTEXT = "AddDirectoryToContextWindow";
    private static final String COPY_TO_CLIPBOARD = "CopyDirectoryToClipboard";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ensureToolWindowVisible(project);

        VirtualFile selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedDir == null || !selectedDir.isDirectory()) {
            NotificationUtil.sendNotification(project, "Please select a directory");
            return;
        }

        String actionId = e.getActionManager().getId(this);
        if (ADD_TO_CONTEXT.equals(actionId)) {
            addDirectoryToContext(project, selectedDir);
        } else if (COPY_TO_CLIPBOARD.equals(actionId)) {
            copyDirectoryToClipboard(project, selectedDir);
        }
    }

    private void addDirectoryToContext(Project project, @NotNull VirtualFile directory) {
        FileListManager fileListManager = FileListManager.getInstance();
        List<VirtualFile> filesToAdd = new ArrayList<>();
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();

        addFilesRecursively(directory, fileListManager, filesToAdd, settings);

        if (!filesToAdd.isEmpty()) {
            fileListManager.addFiles(filesToAdd);

            ModelProvider selectedProvider = ModelProvider.fromString(settings.getSelectedProvider(project.getLocationHash())); //todo test

            ProjectContentService.getInstance()
                .getDirectoryContentAndTokens(directory, false, selectedProvider)
                .thenAccept(result -> {
                    int fileCount = filesToAdd.size();
                    int tokenCount = result.getTokenCount();
                    NotificationUtil.sendNotification(project,
                        String.format("Added %d files from directory: %s (approx. %s tokens in total using %s tokenizer)%s",
                            fileCount,
                            directory.getName(),
                            WindowContextFormatterUtil.format(tokenCount),
                            selectedProvider.getName(),
                            result.getSkippedFileCount() > 0 ? " Skipped " + result.getSkippedFileCount() + " files" : ""));
                });
        }
    }

    private void addFilesRecursively(@NotNull VirtualFile directory, FileListManager fileListManager,
                                     List<VirtualFile> filesToAdd, DevoxxGenieSettingsService settings) {
        VirtualFile[] children = directory.getChildren();
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                if (!settings.getExcludedDirectories().contains(child.getName())) {
                    addFilesRecursively(child, fileListManager, filesToAdd, settings);
                }
            } else if (shouldIncludeFile(child, settings) && !fileListManager.contains(child)) {
                filesToAdd.add(child);
            }
        }
    }

    private void copyDirectoryToClipboard(Project project, VirtualFile directory) {
        // Because we copy the content to the clipboard, we can set the limit to a high number
        CompletableFuture<ScanContentResult> contentFuture = ProjectContentService.getInstance()
            .getDirectoryContent(project, directory, 1_000_000, false);

        contentFuture.thenAccept(content ->
            NotificationUtil.sendNotification(project, "Directory content added to clipboard: " + directory.getName()));
    }

    private boolean shouldIncludeFile(@NotNull VirtualFile file, @NotNull DevoxxGenieSettingsService settings) {
        String extension = file.getExtension();
        return extension != null && settings.getIncludedFileExtensions().contains(extension.toLowerCase());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(file != null && file.isDirectory());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
