package com.devoxx.genie.service.automation.listeners;

import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import com.devoxx.genie.service.automation.EventAutomationService;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens for file save events and fires FILE_SAVED automations.
 * This is an application-level listener; it resolves the project from the file.
 */
@Slf4j
public class FileSaveListener implements FileDocumentManagerListener {

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        EventAutomationService automationService = EventAutomationService.getInstance();
        if (automationService == null) {
            return;
        }

        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }

        // Resolve which open project owns this file
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            String basePath = project.getBasePath();
            if (basePath != null && file.getPath().startsWith(basePath)) {
                Map<String, String> metadata = new LinkedHashMap<>();
                metadata.put("fileName", file.getName());
                metadata.put("lineCount", String.valueOf(document.getLineCount()));

                EventContext ctx = EventContext.builder()
                        .eventType(IdeEventType.FILE_SAVED)
                        .filePaths(List.of(file.getPath()))
                        .metadata(metadata)
                        .build();

                automationService.onEvent(project, ctx);
                return;
            }
        }
    }
}
