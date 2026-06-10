package com.devoxx.genie.ui.util;

import com.devoxx.genie.ui.settings.llm.LLMProvidersConfigurable;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.ex.ConfigurableVisitor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class SettingsDialogUtil {

    private SettingsDialogUtil() {
        /* This utility class should not be instantiated */
    }

    public static void showSettingsDialog(@Nullable Project project) {
        showSettingsDialog(project, groups -> ConfigurableVisitor.findByType(LLMProvidersConfigurable.class, groups));
    }

    public static void showSettingsDialog(@Nullable Project project, @NotNull String displayName) {
        showSettingsDialog(project, groups ->
                ConfigurableVisitor.find(configurable -> displayName.equals(configurable.getDisplayName()), groups));
    }

    private static void showSettingsDialog(@Nullable Project project,
                                           @NotNull Function<List<ConfigurableGroup>, Configurable> selector) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Collecting the configurable groups runs third-party configurables; in PyCharm the
            // Flask console configurable calls runBlockingCancellable, which is forbidden on the
            // EDT. Collect on a pooled thread, then show the dialog on the EDT with the result.
            List<ConfigurableGroup> groups = Arrays.asList(ShowSettingsUtilImpl.getConfigurableGroups(project, true));
            Configurable toSelect = selector.apply(groups);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project != null && project.isDisposed()) {
                    return;
                }
                ShowSettingsUtilImpl.showSettings(project, groups, toSelect);
            });
        });
    }
}
