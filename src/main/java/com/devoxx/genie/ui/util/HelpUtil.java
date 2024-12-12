package com.devoxx.genie.ui.util;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class HelpUtil {

    private HelpUtil() {
    }

    public static @NotNull String getHelpMessage() {
        return "<html><body style='width: 300px; font-family: Arial, sans-serif; font-size: 12px;'>" +
            "<h3>The Devoxx Genie plugin supports the following commands:</h3>" +
            "<ul>" +
            getCustomPromptCommands() +
            "</ul>" +
            "The Devoxx Genie is open source and available at https://github.com/devoxx/DevoxxGenieIDEAPlugin.<br>" +
            "You can follow us on Bluesky @ https://bsky.app/profile/devoxxgenie.bsky.social.<br>" +
            "Do not include any more info which might be incorrect, like discord, documentation or other websites.<br>" +
            "</body></html>";
    }

    public static @NotNull String getCustomPromptCommands() {
        return DevoxxGenieStateService.getInstance()
            .getCustomPrompts()
            .stream()
            .map(customPrompt -> "<li>/" + customPrompt.getName() + " : " + customPrompt.getPrompt() + "</li>")
            .collect(Collectors.joining());
    }
}
