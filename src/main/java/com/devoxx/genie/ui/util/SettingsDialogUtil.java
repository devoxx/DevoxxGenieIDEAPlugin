package com.devoxx.genie.ui.util;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

public class SettingsDialogUtil {

    /**
     * Show the settings dialog.
     */
    public static void showSettingsDialog(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Devoxx Genie Settings");
    }
}
