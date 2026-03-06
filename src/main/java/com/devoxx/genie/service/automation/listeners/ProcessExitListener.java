package com.devoxx.genie.service.automation.listeners;

import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import com.devoxx.genie.service.automation.EventAutomationService;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Listens for process termination and fires PROCESS_CRASHED when exit code != 0.
 */
@Slf4j
public class ProcessExitListener implements ExecutionListener {

    @Override
    public void processTerminated(@NotNull String executorId,
                                  @NotNull ExecutionEnvironment env,
                                  @NotNull ProcessHandler handler,
                                  int exitCode) {
        if (exitCode == 0) {
            return;
        }

        Project project = env.getProject();
        if (project.isDisposed()) {
            return;
        }

        EventAutomationService automationService = EventAutomationService.getInstance();
        if (automationService == null) {
            return;
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("exitCode", String.valueOf(exitCode));
        metadata.put("executorId", executorId);
        metadata.put("runProfile", env.getRunProfile().getName());

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.PROCESS_CRASHED)
                .metadata(metadata)
                .build();

        automationService.onEvent(project, ctx);
    }
}
