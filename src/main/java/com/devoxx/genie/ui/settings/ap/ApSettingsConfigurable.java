package com.devoxx.genie.ui.settings.ap;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * IDE Settings entry for the Docker Agentic Platform CLI integration.
 * Registered in {@code plugin.xml} under the DevoxxGenie parent group.
 */
public class ApSettingsConfigurable implements Configurable {

    private final Project project;
    private final MessageBus messageBus;
    private ApSettingsComponent component;

    public ApSettingsConfigurable(@NotNull Project project) {
        this.project = project;
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Docker Agentic Platform (PREVIEW)";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        component = new ApSettingsComponent();
        return component.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return component != null && component.isModified();
    }

    @Override
    public void apply() {
        if (component != null) {
            component.apply();
            messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);
        }
    }

    @Override
    public void reset() {
        if (component != null) {
            component.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        component = null;
    }
}
