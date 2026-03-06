package com.devoxx.genie.service.automation.listeners;

import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import com.devoxx.genie.service.automation.EventAutomationService;
import com.intellij.compiler.CompilerMessageImpl;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Listens for build completion events and fires BUILD_FAILED or BUILD_SUCCEEDED automations.
 */
@Slf4j
public class BuildCompilationListener implements CompilationStatusListener {

    private final Project project;

    public BuildCompilationListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        if (aborted || project.isDisposed()) {
            return;
        }

        EventAutomationService automationService = EventAutomationService.getInstance();
        if (automationService == null) {
            return;
        }

        if (errors > 0) {
            handleBuildFailed(compileContext, errors, warnings, automationService);
        } else {
            handleBuildSucceeded(warnings, automationService);
        }
    }

    private void handleBuildFailed(@NotNull CompileContext compileContext,
                                   int errors, int warnings,
                                   @NotNull EventAutomationService automationService) {
        CompilerMessage[] errorMessages = compileContext.getMessages(CompilerMessageCategory.ERROR);

        StringBuilder content = new StringBuilder();
        List<String> filePaths = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();

        for (CompilerMessage msg : errorMessages) {
            content.append("ERROR: ").append(msg.getMessage()).append('\n');
            VirtualFile file = msg.getVirtualFile();
            if (file != null && seenPaths.add(file.getPath())) {
                filePaths.add(file.getPath());
            }
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("errorCount", String.valueOf(errors));
        metadata.put("warningCount", String.valueOf(warnings));

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.BUILD_FAILED)
                .content(content.toString().trim())
                .filePaths(filePaths)
                .metadata(metadata)
                .build();

        automationService.onEvent(project, ctx);
    }

    private void handleBuildSucceeded(int warnings,
                                      @NotNull EventAutomationService automationService) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("warningCount", String.valueOf(warnings));

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.BUILD_SUCCEEDED)
                .metadata(metadata)
                .build();

        automationService.onEvent(project, ctx);
    }
}
