package com.devoxx.genie.service.generator.content;

/**
 * Interface for language-specific code style generators.
 */
public interface LanguageStyleGenerator {
    
    /**
     * Adds language-specific code style guidelines to the content builder.
     * 
     * @param contentBuilder The content builder to append to
     */
    void addCodeStyle(StringBuilder contentBuilder);
}
