package com.devoxx.genie.ui.util;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

public class SettingsDialogUtil {

    private SettingsDialogUtil() {
        /* This utility class should not be instantiated */
    }

    public static void showSettingsDialog(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "DevoxxGenie");
    }
}
