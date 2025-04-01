package com.devoxx.genie.service.generator.content;

import com.devoxx.genie.service.generator.content.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the content of the DEVOXXGENIE.md file based on the prompt.
 */
@Slf4j
public class ContentBuilder {
    
    // Language-specific generators
    private final Map<String, LanguageStyleGenerator> styleGenerators;
    private final Map<String, BuildCommandsGenerator> commandGenerators;
    
    /**
     * Initializes the ContentBuilder with language-specific generators.
     */
    public ContentBuilder() {
        this.styleGenerators = initializeStyleGenerators();
        this.commandGenerators = initializeBuildCommandGenerators();
    }
    
    /**
     * Builds the content based on the provided prompt and detected primary language.
     * 
     * @param prompt The prompt containing project information
     * @param primaryLanguage The detected primary language
     * @return The generated content
     */
    public @NotNull String buildContent(@NotNull String prompt, String primaryLanguage) {
        StringBuilder contentBuilder = new StringBuilder();
        
        // Add the basic headers
        initializeContentTemplate(contentBuilder);
        
        // Add build commands section if applicable
        addBuildCommandsSection(contentBuilder, prompt, primaryLanguage);
        
        // Add code style section
        addCodeStyleSection(contentBuilder, primaryLanguage);
        
        // Add dependencies section if applicable
        if (prompt.contains("Dependencies:")) {
            addDependenciesSection(contentBuilder, prompt, primaryLanguage);
        }
        
        return contentBuilder.toString();
    }
    
    /**
     * Extracts the primary language from the prompt.
     * 
     * @param prompt The prompt to parse
     * @return The extracted primary language or empty string if not found
     */
    public String extractPrimaryLanguage(String prompt) {
        if (!prompt.contains("Primary Language:")) {
            return "";
        }
        
        String[] parts = prompt.split("Primary Language:");
        if (parts.length <= 1) {
            return "";
        }
        
        String[] languageParts = parts[1].split("\n");
        return (languageParts.length > 0) ? languageParts[0].trim() : "";
    }
    
    /**
     * Initializes the basic document template.
     */
    private void initializeContentTemplate(StringBuilder contentBuilder) {
        contentBuilder.append("# DEVOXXGENIE.md\n\n");
        contentBuilder.append("## Project Guidelines\n\n");
    }
    
    /**
     * Adds the build commands section if applicable.
     */
    private void addBuildCommandsSection(StringBuilder contentBuilder, String prompt, String primaryLanguage) {
        if (!prompt.contains("Build System:")) {
            return;
        }
        
        contentBuilder.append("### Build Commands\n\n");
        
        // Add version info if available
        addBuildSystemVersion(contentBuilder, prompt);
        
        // Add language-specific build commands
        BuildCommandsGenerator generator = commandGenerators.getOrDefault(primaryLanguage, 
                commandGenerators.get("default"));
        
        if (generator != null) {
            generator.addBuildCommands(contentBuilder);
        } else {
            log.warn("No build commands generator found for language: {}", primaryLanguage);
            // Add fallback commands
            addFallbackBuildCommands(contentBuilder);
        }
    }
    
    /**
     * Adds build system version if available.
     */
    private void addBuildSystemVersion(StringBuilder contentBuilder, String prompt) {
        if (!prompt.contains("Build System Version:")) {
            return;
        }
        
        String[] parts = prompt.split("Build System Version:");
        if (parts.length <= 1) {
            return;
        }
        
        String[] versionParts = parts[1].split("\n");
        if (versionParts.length > 0) {
            contentBuilder.append("Using ").append(versionParts[0].trim()).append("\n\n");
        }
    }
    
    /**
     * Adds fallback build commands if no language-specific generator is found.
     */
    private void addFallbackBuildCommands(StringBuilder contentBuilder) {
        contentBuilder.append("- **Build:** `./gradlew build` or `mvn clean install`\n");
        contentBuilder.append("- **Test:** `./gradlew test` or `mvn test`\n");
        contentBuilder.append("- **Run:** `./gradlew run` or `java -jar target/application.jar`\n\n");
    }
    
    /**
     * Adds the code style section.
     */
    private void addCodeStyleSection(StringBuilder contentBuilder, String primaryLanguage) {
        contentBuilder.append("### Code Style\n\n");
        
        LanguageStyleGenerator generator = styleGenerators.getOrDefault(primaryLanguage, 
                styleGenerators.get("default"));
        
        if (generator != null) {
            generator.addCodeStyle(contentBuilder);
        } else {
            log.warn("No style generator found for language: {}", primaryLanguage);
            // Add fallback style guidelines
            addFallbackCodeStyle(contentBuilder);
        }
    }
    
    /**
     * Adds fallback code style guidelines if no language-specific generator is found.
     */
    private void addFallbackCodeStyle(StringBuilder contentBuilder) {
        contentBuilder.append("- **Formatting:** Follow project's code style configuration\n");
        contentBuilder.append("- **Naming:** Use consistent naming conventions\n");
        contentBuilder.append("- **Documentation:** Document public APIs\n");
        contentBuilder.append("- **Error Handling:** Handle exceptions appropriately\n\n");
    }
    
    /**
     * Adds the dependencies section.
     */
    private void addDependenciesSection(StringBuilder contentBuilder, String prompt, String primaryLanguage) {
        contentBuilder.append("### Dependencies\n\n");
        
        if ("Rust".equals(primaryLanguage)) {
            addRustDependenciesInfo(contentBuilder);
        } else {
            addGenericDependenciesInfo(contentBuilder, prompt);
        }
    }
    
    /**
     * Adds Rust-specific dependencies and project organization info.
     */
    private void addRustDependenciesInfo(StringBuilder contentBuilder) {
        contentBuilder.append("- **Manage with:** `Cargo.toml`\n");
        contentBuilder.append("- **Update with:** `cargo update`\n\n");

        // Add Rust project organization section
        contentBuilder.append("### Project Organization\n\n");
        contentBuilder.append("- `src/main.rs`: Binary entrypoint\n");
        contentBuilder.append("- `src/lib.rs`: Library code\n");
        contentBuilder.append("- `src/bin/`: Additional binaries\n");
        contentBuilder.append("- `tests/`: Integration tests\n\n");
    }
    
    /**
     * Adds generic dependencies information.
     */
    private void addGenericDependenciesInfo(StringBuilder contentBuilder, String prompt) {
        contentBuilder.append("The project uses the following main dependencies:\n\n");
        
        // Add commonly found dependencies
        if (prompt.contains("langchain4j")) {
            contentBuilder.append("- **LangChain4j** - Java library for LLM applications\n");
        }
        if (prompt.contains("junit") || prompt.contains("JUnit")) {
            contentBuilder.append("- **JUnit** - Testing framework\n");
        }
        if (prompt.contains("mockito")) {
            contentBuilder.append("- **Mockito** - Mocking framework for tests\n");
        }
        if (prompt.contains("springframework") || prompt.contains("Spring")) {
            contentBuilder.append("- **Spring Framework** - Application framework\n");
        }
        if (prompt.contains("lombok")) {
            contentBuilder.append("- **Lombok** - Code generation library\n");
        }
        
        contentBuilder.append("\nSee build.gradle.kts or pom.xml for the complete dependency list.\n\n");
    }
    
    /**
     * Initializes the map of language-specific style generators.
     */
    private Map<String, LanguageStyleGenerator> initializeStyleGenerators() {
        Map<String, LanguageStyleGenerator> generators = new HashMap<>();
        
        // Add language-specific style generators
        generators.put("Rust", new RustStyleGenerator());
        generators.put("Java", new JavaStyleGenerator());
        // Default style generator
        generators.put("default", new JavaStyleGenerator());
        
        // Add more languages as needed...
        
        return generators;
    }
    
    /**
     * Initializes the map of language-specific build command generators.
     */
    private Map<String, BuildCommandsGenerator> initializeBuildCommandGenerators() {
        Map<String, BuildCommandsGenerator> generators = new HashMap<>();
        
        // Add language-specific command generators
        generators.put("Rust", new RustCommandsGenerator());
        generators.put("Java", new GradleCommandsGenerator());
        // Default command generator
        generators.put("default", new GradleCommandsGenerator());
        
        // Add more languages as needed...
        
        return generators;
    }
}
