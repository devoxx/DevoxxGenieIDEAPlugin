package com.devoxx.genie.ui.util;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HelpUtil {

    private HelpUtil() {
    }

    public static @NotNull String getHelpMessage(@NotNull ResourceBundle resourceBundle) {
        return "<html><body style='width: 300px; font-family: Arial, sans-serif; font-size: 12px;'>" +
            "<h3>Available commands:</h3>" +
            "<ul>" +
            "<li>" + resourceBundle.getString("command.test") + "</li>" +
            "<li>" + resourceBundle.getString("command.review") + "</li>" +
            "<li>" + resourceBundle.getString("command.explain") + "</li>" +
            getCustomPromptCommands() +
            "</ul></body></html>";
    }

    public static @NotNull String getCustomPromptCommands() {
        return DevoxxGenieStateService.getInstance()
            .getCustomPrompts()
            .stream()
            .map(customPrompt -> "<li>/" + customPrompt.getName() + " : custom command</li>")
            .collect(Collectors.joining());
    }
}
