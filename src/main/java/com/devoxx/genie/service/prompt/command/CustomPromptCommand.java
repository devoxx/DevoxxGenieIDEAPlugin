package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static com.devoxx.genie.model.Constant.COMMAND_PREFIX;
import static com.devoxx.genie.model.Constant.HELP_COMMAND;

/**
 * Command processor for custom prompt commands.
 */
@Slf4j
public class CustomPromptCommand implements PromptCommand {

    private static final String ARGUMENT_PLACEHOLDER = "$ARGUMENT";

    @Override
    public boolean matches(@NotNull String prompt) {
        String trimmedPrompt = prompt.trim();
        
        // Check if it's a command but not the help command (which has its own processor)
        if (!trimmedPrompt.startsWith(COMMAND_PREFIX)) {
            return false;
        }
        
        if (trimmedPrompt.startsWith(COMMAND_PREFIX + HELP_COMMAND)) {
            return false;
        }
        
        // Get custom commands from settings
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();
        List<CustomPrompt> customPrompts = settings.getCustomPrompts();
        
        String commandName = extractCommandName(trimmedPrompt);
        if (commandName == null) {
            return false;
        }

        // Check if any custom command matches exactly (avoid prefix collisions)
        return customPrompts.stream()
                .anyMatch(prompt1 -> commandName.equalsIgnoreCase(prompt1.getName()));
    }

    @Override
    public Optional<String> process(@NotNull ChatMessageContext chatMessageContext, 
                                  @NotNull PromptOutputPanel promptOutputPanel) {

        // Get custom commands from settings
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();
        List<CustomPrompt> customPrompts = settings.getCustomPrompts();

        String prompt = chatMessageContext.getUserPrompt().trim();

        String commandName = extractCommandName(prompt);
        if (commandName == null) {
            return Optional.of(prompt);
        }

        // Find the matching custom prompt
        Optional<CustomPrompt> matchingPrompt = customPrompts.stream()
                .filter(customPrompt -> commandName.equalsIgnoreCase(customPrompt.getName()))
                .findFirst();
                
        if (matchingPrompt.isEmpty()) {
            log.debug("No matching custom command found");
            return Optional.of(prompt);
        }
        
        CustomPrompt customPrompt = matchingPrompt.get();
        chatMessageContext.setCommandName(customPrompt.getName());
        
        // Extract user arguments and apply them to the custom prompt template
        String userArgs = extractUserArguments(prompt, commandName);
        String processedPrompt = applyArguments(customPrompt.getPrompt(), userArgs);
        
        return Optional.of(processedPrompt);
    }

    private String applyArguments(@NotNull String template, @NotNull String userArgs) {
        if (template.contains(ARGUMENT_PLACEHOLDER)) {
            return template.replace(ARGUMENT_PLACEHOLDER, userArgs);
        }
        if (userArgs.isEmpty()) {
            return template;
        }
        return template + " " + userArgs;
    }

    private String extractUserArguments(@NotNull String prompt, @NotNull String commandName) {
        int start = COMMAND_PREFIX.length() + commandName.length();
        if (prompt.length() <= start) {
            return "";
        }
        return prompt.substring(start).trim();
    }

    private String extractCommandName(@NotNull String prompt) {
        if (!prompt.startsWith(COMMAND_PREFIX) || prompt.length() <= COMMAND_PREFIX.length()) {
            return null;
        }
        int firstSpace = prompt.indexOf(' ');
        if (firstSpace == -1) {
            return prompt.substring(COMMAND_PREFIX.length());
        }
        return prompt.substring(COMMAND_PREFIX.length(), firstSpace).trim();
    }
}
