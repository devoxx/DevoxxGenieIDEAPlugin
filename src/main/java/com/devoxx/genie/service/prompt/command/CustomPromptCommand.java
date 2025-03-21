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
        
        // Check if any custom command matches
        return customPrompts.stream()
                .anyMatch(prompt1 -> trimmedPrompt.startsWith(COMMAND_PREFIX + prompt1.getName()));
    }

    @Override
    public Optional<String> process(@NotNull ChatMessageContext chatMessageContext, 
                                  @NotNull PromptOutputPanel promptOutputPanel) {
        String prompt = chatMessageContext.getUserPrompt().trim();
        
        // Get custom commands from settings
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();
        List<CustomPrompt> customPrompts = settings.getCustomPrompts();
        
        // Find the matching custom prompt
        Optional<CustomPrompt> matchingPrompt = customPrompts.stream()
                .filter(customPrompt -> prompt.startsWith(COMMAND_PREFIX + customPrompt.getName()))
                .findFirst();
                
        if (matchingPrompt.isEmpty()) {
            log.debug("No matching custom command found");
            return Optional.of(prompt);
        }
        
        CustomPrompt customPrompt = matchingPrompt.get();
        chatMessageContext.setCommandName(customPrompt.getName());
        
        // Extract user arguments and combine with custom prompt template
        String userArgs = prompt.substring(COMMAND_PREFIX.length() + customPrompt.getName().length()).trim();
        String processedPrompt = customPrompt.getPrompt() + " " + userArgs;
        
        return Optional.of(processedPrompt);
    }
}
