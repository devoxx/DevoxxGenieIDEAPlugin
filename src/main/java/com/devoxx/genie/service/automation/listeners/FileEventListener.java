package com.devoxx.genie.service.automation.listeners;

import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import com.devoxx.genie.service.automation.EventAutomationService;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens for file open events and fires FILE_OPENED automations.
 */
@Slf4j
public class FileEventListener implements FileEditorManagerListener {

    private final Project project;

    public FileEventListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (project.isDisposed()) {
            return;
        }

        EventAutomationService automationService = EventAutomationService.getInstance();
        if (automationService == null) {
            return;
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("fileName", file.getName());
        metadata.put("fileType", file.getFileType().getName());
        metadata.put("extension", file.getExtension() != null ? file.getExtension() : "");

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.FILE_OPENED)
                .filePaths(List.of(file.getPath()))
                .metadata(metadata)
                .build();

        automationService.onEvent(project, ctx);
    }
}
