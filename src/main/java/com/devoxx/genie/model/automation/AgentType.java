package com.devoxx.genie.model.automation;

import lombok.Getter;

/**
 * Built-in agent types that can be triggered by IDE events.
 * Users can also create custom agents via the settings panel.
 */
@Getter
public enum AgentType {

    CODE_REVIEW("Code Review Agent",
            "Reviews code changes for bugs, security issues, and style violations",
            "Review the following code changes and identify potential bugs, security issues, " +
            "style violations, and suggest improvements. Be specific about line numbers and provide fixes."),

    BUILD_FIX("Build Fix Agent",
            "Analyzes build failures and proposes fixes",
            "Analyze the following build errors. For each error, identify the root cause, " +
            "explain why it happened, and provide a concrete fix. Prioritize by severity."),

    DEBUG("Debug Agent",
            "Analyzes test failures and exceptions to find root causes",
            "Analyze the following test failure or exception. Read the stack trace, " +
            "identify the root cause, check recent changes that might have caused it, " +
            "and propose a fix with explanation."),

    TEST_GENERATOR("Test Generator Agent",
            "Generates unit tests for new or changed code",
            "Generate comprehensive unit tests for the following code. Include edge cases, " +
            "boundary conditions, and error scenarios. Use the project's existing test framework and conventions."),

    EXPLAINER("Code Explainer Agent",
            "Explains unfamiliar code files and their dependencies",
            "Provide a brief summary of this file: its purpose, key methods/classes, " +
            "dependencies, and how it fits into the broader architecture. Keep it concise."),

    SCAFFOLDER("Scaffold Agent",
            "Generates boilerplate for new files matching project conventions",
            "Generate appropriate boilerplate for this new file based on its location, type, " +
            "and the project's existing conventions. Include standard imports, class structure, " +
            "and license headers if applicable."),

    DEPENDENCY_CHECK("Dependency Check Agent",
            "Checks for vulnerable or outdated dependencies",
            "Check the project dependencies for known vulnerabilities (CVEs), available updates, " +
            "and unused dependencies. Prioritize critical security issues."),

    ONBOARDING("Onboarding Agent",
            "Provides project overview and guidance when opening a project",
            "Scan the project structure and provide a brief overview: tech stack, key entry points, " +
            "build system, test status, and any configuration files that need attention. " +
            "Suggest next steps for getting started."),

    CUSTOM("Custom Agent",
            "User-defined agent with custom prompt",
            "");

    private final String displayName;
    private final String description;
    private final String defaultPrompt;

    AgentType(String displayName, String description, String defaultPrompt) {
        this.displayName = displayName;
        this.description = description;
        this.defaultPrompt = defaultPrompt;
    }
}
