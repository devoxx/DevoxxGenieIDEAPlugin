package com.devoxx.genie.service.generator.content;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds prompts for content generation based on project information.
 */
@Slf4j
public class PromptBuilder {
    public static final String VERSION = "version";

    /**
     * Builds a comprehensive prompt from project information.
     * 
     * @param projectInfo The project information
     * @return A formatted prompt string
     */
    public @NotNull String buildPrompt(@NotNull Map<String, Object> projectInfo) {
        StringBuilder promptBuilder = new StringBuilder("Please create a DEVOXXGENIE.md file for a software project with the following details:\n\n");

        // Add project details
        appendBuildSystemInfo(promptBuilder, projectInfo);
        appendLanguageInfo(promptBuilder, projectInfo);
        appendFrameworkInfo(promptBuilder, projectInfo);
        appendCodeStyleInfo(promptBuilder, projectInfo);
        appendDependenciesInfo(promptBuilder, projectInfo);
        
        // Add instructions for the generated content
        appendGenerationInstructions(promptBuilder);

        return promptBuilder.toString();
    }
    
    /**
     * Appends build system information to the prompt.
     */
    private void appendBuildSystemInfo(StringBuilder promptBuilder, Map<String, Object> projectInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem == null) {
            return;
        }
        
        // Add build system type
        promptBuilder.append("Build System: ").append(buildSystem.get("type")).append("\n");

        // Add build system version if available
        if (buildSystem.get(VERSION) != null) {
            promptBuilder.append("Build System Version: ").append(buildSystem.get(VERSION)).append("\n");
        }

        // Add build commands if available
        appendBuildCommands(promptBuilder, buildSystem);
    }
    
    /**
     * Appends build commands to the prompt.
     */
    private void appendBuildCommands(StringBuilder promptBuilder, Map<String, Object> buildSystem) {
        Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        promptBuilder.append("Build Commands:\n");
        for (Map.Entry<String, String> command : commands.entrySet()) {
            promptBuilder.append("- ")
                         .append(command.getKey())
                         .append(": `")
                         .append(command.getValue())
                         .append("`\n");
        }
    }
    
    /**
     * Appends programming language information to the prompt.
     */
    private void appendLanguageInfo(StringBuilder promptBuilder, Map<String, Object> projectInfo) {
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null) {
            return;
        }
        
        // Add primary language
        promptBuilder.append("Primary Language: ").append(languages.get("primary")).append("\n");

        // Add detected languages if available
        List<String> detected = (List<String>) languages.get("detected");
        if (detected != null && !detected.isEmpty()) {
            promptBuilder.append("Detected Languages: ").append(String.join(", ", detected)).append("\n");
        }
    }
    
    /**
     * Appends framework information to the prompt.
     */
    private void appendFrameworkInfo(StringBuilder promptBuilder, Map<String, Object> projectInfo) {
        Map<String, Object> frameworks = (Map<String, Object>) projectInfo.get("frameworks");
        if (frameworks == null) {
            return;
        }
        
        // Add testing frameworks if available
        List<String> testingFrameworks = (List<String>) frameworks.get("testing");
        if (testingFrameworks != null && !testingFrameworks.isEmpty()) {
            promptBuilder.append("Testing Frameworks: ")
                         .append(String.join(", ", testingFrameworks))
                         .append("\n");
        }

        // Add web frameworks if available
        List<String> webFrameworks = (List<String>) frameworks.get("web");
        if (webFrameworks != null && !webFrameworks.isEmpty()) {
            promptBuilder.append("Web Frameworks: ")
                         .append(String.join(", ", webFrameworks))
                         .append("\n");
        }
    }
    
    /**
     * Appends code style information to the prompt.
     */
    private void appendCodeStyleInfo(StringBuilder promptBuilder, Map<String, Object> projectInfo) {
        Map<String, Object> codeStyle = (Map<String, Object>) projectInfo.get("codeStyle");
        if (codeStyle == null) {
            return;
        }
        
        // Add code style tools
        List<String> styleTools = collectCodeStyleTools(codeStyle);
        promptBuilder.append("Code Style Tools: ")
                     .append(String.join(", ", styleTools))
                     .append("\n");

        // Add EditorConfig settings if available
        if (codeStyle.containsKey("editorconfig")) {
            promptBuilder.append("EditorConfig Settings:\n```\n")
                         .append(codeStyle.get("editorconfig"))
                         .append("\n```\n");
        }
    }
    
    /**
     * Collects code style tools from the code style info map.
     */
    private List<String> collectCodeStyleTools(Map<String, Object> codeStyle) {
        List<String> styleTools = new ArrayList<>();
        if (codeStyle.containsKey("editorconfig")) styleTools.add("EditorConfig");
        if (codeStyle.containsKey("eslint")) styleTools.add("ESLint");
        if (codeStyle.containsKey("prettier")) styleTools.add("Prettier");
        if (codeStyle.containsKey("checkstyle")) styleTools.add("Checkstyle");
        if (codeStyle.containsKey("phpcs")) styleTools.add("PHP_CodeSniffer");
        if (codeStyle.containsKey("clang-format")) styleTools.add("ClangFormat");
        return styleTools;
    }
    
    /**
     * Appends dependencies information to the prompt.
     */
    private void appendDependenciesInfo(StringBuilder promptBuilder, Map<String, Object> projectInfo) {
        Map<String, Object> dependencies = (Map<String, Object>) projectInfo.get("dependencies");
        if (dependencies == null) {
            return;
        }
        
        List<Map<String, String>> depList = (List<Map<String, String>>) dependencies.get("list");
        if (depList == null || depList.isEmpty()) {
            return;
        }
        
        promptBuilder.append("\nDependencies:\n");
        for (Map<String, String> dep : depList) {
            appendDependency(promptBuilder, dep);
        }
    }
    
    /**
     * Appends a single dependency to the prompt.
     */
    private void appendDependency(StringBuilder promptBuilder, Map<String, String> dep) {
        String name = dep.get("name");
        if (name == null) {
            return;
        }
        
        promptBuilder.append("- ");
        
        // Format dependency coordinates if group and version are available
        String group = dep.get("group");
        String version = dep.get(VERSION);
        if (group != null && version != null) {
            promptBuilder.append(group).append(":").append(name).append(":").append(version);
        } else {
            promptBuilder.append(name);
        }

        // Add scope if available
        String scope = dep.get("scope");
        if (scope != null) {
            promptBuilder.append(" (").append(scope).append(")");
        }

        // Add note if available
        String note = dep.get("note");
        if (note != null) {
            promptBuilder.append(" - ").append(note);
        }

        promptBuilder.append("\n");
    }
    
    /**
     * Appends instructions for the generated content.
     */
    private void appendGenerationInstructions(StringBuilder promptBuilder) {
        promptBuilder.append("\nThe file should include:\n")
                    .append("1. Comprehensive build/lint/test commands - especially for running a single test\n")
                    .append("2. Code style guidelines including imports, formatting, naming conventions, error handling, etc.\n")
                    .append("3. Any other relevant project-specific guidelines\n\n")
                    .append("The content should be formatted in Markdown and be about 20-30 lines long.\n")
                    .append("This file will be given to AI coding assistants to help them understand the project's standards.");
    }
}
