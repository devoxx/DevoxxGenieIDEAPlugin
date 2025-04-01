package com.devoxx.genie.service.generator.content;

/**
 * Interface for language-specific build commands generators.
 */
public interface BuildCommandsGenerator {
    
    /**
     * Adds language-specific build commands to the content builder.
     * 
     * @param contentBuilder The content builder to append to
     */
    void addBuildCommands(StringBuilder contentBuilder);
}
