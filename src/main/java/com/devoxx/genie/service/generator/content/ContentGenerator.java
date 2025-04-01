package com.devoxx.genie.service.generator.content;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Generates the content for the DEVOXXGENIE.md file.
 */
@Slf4j
public class ContentGenerator {

    private final PromptBuilder promptBuilder;
    private final ContentBuilder contentBuilder;
    
    /**
     * Initializes the ContentGenerator with its dependencies.
     */
    public ContentGenerator() {
        this.promptBuilder = new PromptBuilder();
        this.contentBuilder = new ContentBuilder();
    }
    
    /**
     * Creates a prompt from project information.
     * 
     * @param projectInfo The project information
     * @return A formatted prompt string
     */
    public @NotNull String createPromptFromProjectInfo(@NotNull Map<String, Object> projectInfo) {
        log.debug("Creating prompt from project info");
        return promptBuilder.buildPrompt(projectInfo);
    }
    
    /**
     * Generates content based on the prompt.
     * 
     * @param prompt The prompt containing project information
     * @return The generated content
     */
    public @NotNull String generateContent(@NotNull String prompt) {
        log.debug("Generating content from prompt");
        
        // Extract primary language from prompt
        String primaryLanguage = contentBuilder.extractPrimaryLanguage(prompt);
        log.info("Detected primary language: {}", primaryLanguage);
        
        // Generate the content
        return contentBuilder.buildContent(prompt, primaryLanguage);
    }
}
