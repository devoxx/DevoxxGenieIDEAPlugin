package com.devoxx.genie.service.automation;

import com.devoxx.genie.model.automation.EventAgentMapping;
import com.devoxx.genie.model.automation.EventAutomationSettings;
import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Central dispatch service for event-driven automations.
 * <p>
 * When an IDE event fires, the corresponding listener calls
 * {@link #onEvent(Project, EventContext)} which:
 * <ol>
 *   <li>Checks if event automations are globally enabled</li>
 *   <li>Finds all enabled mappings for the event type</li>
 *   <li>Renders the prompt template with runtime context</li>
 *   <li>Either auto-submits the prompt or shows a confirmation balloon</li>
 * </ol>
 */
@Slf4j
public class EventAutomationService {

    public static EventAutomationService getInstance() {
        return ApplicationManager.getApplication().getService(EventAutomationService.class);
    }

    /**
     * Called by IDE event listeners when an event fires.
     */
    public void onEvent(@NotNull Project project, @NotNull EventContext context) {
        if (project.isDisposed()) {
            return;
        }

        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        if (!Boolean.TRUE.equals(state.getEventAutomationEnabled())) {
            log.debug("Event automations disabled, ignoring {}", context.getEventType());
            return;
        }

        EventAutomationSettings settings = state.getEventAutomationSettings();
        if (settings == null) {
            return;
        }

        List<EventAgentMapping> matchingMappings = settings.getMappings().stream()
                .filter(EventAgentMapping::isEnabled)
                .filter(m -> m.getEventType().equals(context.getEventType().name()))
                .toList();

        if (matchingMappings.isEmpty()) {
            log.debug("No enabled mappings for event {}", context.getEventType());
            return;
        }

        for (EventAgentMapping mapping : matchingMappings) {
            String renderedPrompt = PromptTemplateRenderer.render(mapping.getPrompt(), context);
            log.info("Event {} triggered agent '{}' (autoRun={})",
                    context.getEventType(), mapping.getAgentType(), mapping.isAutoRun());

            if (mapping.isAutoRun()) {
                submitPrompt(project, renderedPrompt);
            } else {
                showConfirmationNotification(project, mapping, renderedPrompt);
            }
        }
    }

    /**
     * Submits a rendered prompt to the DevoxxGenie chat via the message bus.
     */
    private void submitPrompt(@NotNull Project project, @NotNull String prompt) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            project.getMessageBus()
                    .syncPublisher(com.devoxx.genie.ui.topic.AppTopics.PROMPT_SUBMISSION_TOPIC)
                    .onPromptSubmitted(project, prompt);
        });
    }

    /**
     * Shows a balloon notification allowing the user to review and confirm the prompt.
     */
    private void showConfirmationNotification(@NotNull Project project,
                                              @NotNull EventAgentMapping mapping,
                                              @NotNull String renderedPrompt) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }

            String agentLabel = mapping.getCustomAgentName() != null && !mapping.getCustomAgentName().isEmpty()
                    ? mapping.getCustomAgentName()
                    : mapping.getAgentType();

            IdeEventType eventType = IdeEventType.valueOf(mapping.getEventType());

            NotificationGroupManager.getInstance()
                    .getNotificationGroup("com.devoxx.genie.notifications")
                    .createNotification(
                            "DevoxxGenie: " + eventType.getDisplayName(),
                            agentLabel + " agent is ready to run. Click to submit.",
                            NotificationType.INFORMATION
                    )
                    .addAction(new com.intellij.notification.NotificationAction("Run Agent") {
                        @Override
                        public void actionPerformed(
                                @NotNull com.intellij.openapi.actionSystem.AnActionEvent e,
                                @NotNull com.intellij.notification.Notification notification) {
                            notification.expire();
                            submitPrompt(project, renderedPrompt);
                        }
                    })
                    .addAction(new com.intellij.notification.NotificationAction("Dismiss") {
                        @Override
                        public void actionPerformed(
                                @NotNull com.intellij.openapi.actionSystem.AnActionEvent e,
                                @NotNull com.intellij.notification.Notification notification) {
                            notification.expire();
                        }
                    })
                    .notify(project);
        });
    }
}
