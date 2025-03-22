package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.service.projectscanner.FileScanner;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class DevoxxGenieGenerator {

    private final Project project;
    private final VirtualFile baseDir;
    private final Boolean includeTree;
    private final Integer treeDepth;
    private final ProgressIndicator indicator;
    // Add FileScanner as a dependency
    private final FileScanner fileScanner;

    public DevoxxGenieGenerator(@NotNull Project project,
                                boolean includeTree,
                                int treeDepth,
                                ProgressIndicator indicator) {
        this.project = project;
        this.includeTree = includeTree;
        this.treeDepth = treeDepth;

        this.baseDir = project.getBaseDir();
        this.indicator = indicator;
        // Initialize FileScanner
        this.fileScanner = new FileScanner();
    }
    
    /**
     * Generates and appends a project tree using FileScanner.
     *
     * @param baseDir   The base directory
     * @param treeDepth The maximum tree depth
     */
    private void appendProjectTreeUsingFileScanner(VirtualFile baseDir, Integer treeDepth) {
        try {
            // Use FileScanner's tree generation capability
            String treeContent = fileScanner.generateSourceTreeRecursive(baseDir, 0);
            
            // Format the tree content for inclusion in the MD file
            StringBuilder formattedTree = new StringBuilder();
            formattedTree.append("\n\n### Project Tree\n\n");
            formattedTree.append("```\n");
            formattedTree.append(treeContent);
            formattedTree.append("\n```\n");
            
            // Append to the DEVOXXGENIE.md file
            VirtualFile devoxxGenieMdFile = baseDir.findChild("DEVOXXGENIE.md");
            
            if (devoxxGenieMdFile != null) {
                String currentContent = VfsUtil.loadText(devoxxGenieMdFile);
                
                // Check if it already has a project tree section
                if (currentContent.contains("### Project Tree")) {
                    // Remove existing project tree section
                    int sectionStart = currentContent.indexOf("### Project Tree");
                    int sectionEnd = currentContent.indexOf("```\n", sectionStart);
                    sectionEnd = currentContent.indexOf("\n```", sectionEnd + 4);
                    
                    if (sectionEnd > sectionStart) {
                        // Replace the existing section
                        String beforeSection = currentContent.substring(0, sectionStart - 2); // Account for newlines
                        String afterSection = sectionEnd + 4 < currentContent.length() ? 
                                               currentContent.substring(sectionEnd + 4) : "";
                        
                        currentContent = beforeSection + formattedTree.toString() + afterSection;
                    } else {
                        // If section is malformed, just append
                        currentContent += formattedTree.toString();
                    }
                } else {
                    // Append new section
                    currentContent += formattedTree.toString();
                }
                
                // Update the file with tree content appended
                final String updatedContent = currentContent;
                
                // Execute file write in a write action
                ApplicationManager.getApplication().invokeAndWait(() ->
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            VfsUtil.saveText(devoxxGenieMdFile, updatedContent);
                            log.info("Project tree appended to DEVOXXGENIE.md");
                        } catch (IOException e) {
                            log.error("Error appending project tree to DEVOXXGENIE.md", e);
                        }
                    })
                );
            }
        } catch (Exception e) {
            log.error("Error generating project tree", e);
        }
    }

    public void generate() {
        indicator.setText("Scanning project structure...");
        indicator.setIndeterminate(true);

        // Initialize FileScanner's gitignore parser
        fileScanner.initGitignoreParser(project, baseDir);

        // Get ProjectFileIndex for file traversal
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        // Use FileScanner to scan the project structure
        ScanContentResult scanResult = new ScanContentResult();
        List<VirtualFile> relevantFiles = fileScanner.scanDirectory(fileIndex, baseDir, scanResult);

        // Analyze the project using platform APIs
        ProjectAnalyzer scanner = new ProjectAnalyzer(project, baseDir);
        Map<String, Object> projectInfo = scanner.scanProject();

        indicator.setText("Analyzing project structure...");

        String prompt = createPromptFromProjectInfo(projectInfo);

        indicator.setText("Generating DEVOXXGENIE.md content...");

        // Generate a sample content (placeholder for AI-generated content)
        String generatedContent = generateSampleContent(prompt);

        indicator.setText("Writing DEVOXXGENIE.md file...");

        // Write the file
        writeDevoxxGenieMd(generatedContent);

        if (includeTree) {
            // Generate and append project tree to DEVOXXGENIE.md using FileScanner
            indicator.setText("Generating project tree...");
            appendProjectTreeUsingFileScanner(baseDir, treeDepth);
        }

        // Notify user
        NotificationUtil.sendNotification(project, "DEVOXXGENIE.md generated successfully in "
                + baseDir.getPath());
    }

    private @NotNull String createPromptFromProjectInfo(@NotNull Map<String, Object> projectInfo) {
        StringBuilder promptBuilder = new StringBuilder("Please create a DEVOXXGENIE.md file for a software project with the following details:\n\n");

        // Add build system info
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem != null) {
            promptBuilder.append("Build System: ").append(buildSystem.get("type")).append("\n");

            // Add build system version if available
            if (buildSystem.get("version") != null) {
                promptBuilder.append("Build System Version: ").append(buildSystem.get("version")).append("\n");
            }

            Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
            if (commands != null && !commands.isEmpty()) {
                promptBuilder.append("Build Commands:\n");
                for (Map.Entry<String, String> command : commands.entrySet()) {
                    promptBuilder.append("- ").append(command.getKey()).append(": `").append(command.getValue()).append("`\n");
                }
            }
        }

        // Add language info
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages != null) {
            promptBuilder.append("Primary Language: ").append(languages.get("primary")).append("\n");

            List<String> detected = (List<String>) languages.get("detected");
            if (detected != null && !detected.isEmpty()) {
                promptBuilder.append("Detected Languages: ").append(String.join(", ", detected)).append("\n");
            }
        }

        // Add framework info
        Map<String, Object> frameworks = (Map<String, Object>) projectInfo.get("frameworks");
        if (frameworks != null) {
            List<String> testingFrameworks = (List<String>) frameworks.get("testing");
            if (testingFrameworks != null && !testingFrameworks.isEmpty()) {
                promptBuilder.append("Testing Frameworks: ").append(String.join(", ", testingFrameworks)).append("\n");
            }

            List<String> webFrameworks = (List<String>) frameworks.get("web");
            if (webFrameworks != null && !webFrameworks.isEmpty()) {
                promptBuilder.append("Web Frameworks: ").append(String.join(", ", webFrameworks)).append("\n");
            }
        }

        // Add code style info
        Map<String, Object> codeStyle = (Map<String, Object>) projectInfo.get("codeStyle");
        if (codeStyle != null) {
            promptBuilder.append("Code Style Tools: ");
            List<String> styleTools = new ArrayList<>();
            if (codeStyle.containsKey("editorconfig")) styleTools.add("EditorConfig");
            if (codeStyle.containsKey("eslint")) styleTools.add("ESLint");
            if (codeStyle.containsKey("prettier")) styleTools.add("Prettier");
            if (codeStyle.containsKey("checkstyle")) styleTools.add("Checkstyle");
            if (codeStyle.containsKey("phpcs")) styleTools.add("PHP_CodeSniffer");
            if (codeStyle.containsKey("clang-format")) styleTools.add("ClangFormat");
            promptBuilder.append(String.join(", ", styleTools)).append("\n");

            if (codeStyle.containsKey("editorconfig")) {
                promptBuilder.append("EditorConfig Settings:\n```\n").append(codeStyle.get("editorconfig")).append("\n```\n");
            }
        }

        // Add dependencies info
        Map<String, Object> dependencies = (Map<String, Object>) projectInfo.get("dependencies");
        if (dependencies != null) {
            List<Map<String, String>> depList = (List<Map<String, String>>) dependencies.get("list");
            if (depList != null && !depList.isEmpty()) {
                promptBuilder.append("\nDependencies:\n");
                for (Map<String, String> dep : depList) {
                    String group = dep.get("group");
                    String name = dep.get("name");
                    String version = dep.get("version");
                    String scope = dep.get("scope");
                    String note = dep.get("note");

                    if (name != null) {
                        promptBuilder.append("- ");
                        if (group != null && version != null) {
                            promptBuilder.append(group).append(":").append(name).append(":").append(version);
                        } else {
                            promptBuilder.append(name);
                        }

                        if (scope != null) {
                            promptBuilder.append(" (").append(scope).append(")");
                        }

                        if (note != null) {
                            promptBuilder.append(" - ").append(note);
                        }

                        promptBuilder.append("\n");
                    }
                }
            }
        }

        // Add request for the generated content
        promptBuilder.append("\nThe file should include:\n").append("1. Comprehensive build/lint/test commands - especially for running a single test\n").append("2. Code style guidelines including imports, formatting, naming conventions, error handling, etc.\n").append("3. Any other relevant project-specific guidelines\n\n").append("The content should be formatted in Markdown and be about 20-30 lines long.\n").append("This file will be given to AI coding assistants to help them understand the project's standards.");

        return promptBuilder.toString();
    }

    private @NotNull String generateSampleContent(@NotNull String prompt) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("# DEVOXXGENIE.md\n\n");
        contentBuilder.append("## Project Guidelines\n\n");

        // Extract language info from the prompt
        String primaryLanguage = "";
        if (prompt.contains("Primary Language:")) {
            String[] parts = prompt.split("Primary Language:");
            if (parts.length > 1) {
                String[] languageParts = parts[1].split("\n");
                if (languageParts.length > 0) {
                    primaryLanguage = languageParts[0].trim();
                }
            }
        }

        // Extract build system info from the prompt
        if (prompt.contains("Build System:")) {
            contentBuilder.append("### Build Commands\n\n");

            // Add build commands based on the primary language
            // Check if build system version is available in the prompt
            String buildSystemVersion = "";
            if (prompt.contains("Build System Version:")) {
                String[] parts = prompt.split("Build System Version:");
                if (parts.length > 1) {
                    String[] versionParts = parts[1].split("\n");
                    if (versionParts.length > 0) {
                        buildSystemVersion = versionParts[0].trim();
                        // Add version info to the output
                        contentBuilder.append("Using " + buildSystemVersion + "\n\n");
                    }
                }
            }

            if ("Rust".equals(primaryLanguage)) {
                // Rust-specific build commands
                contentBuilder.append("- **Build:** `cargo build`\n");
                contentBuilder.append("- **Run:** `cargo run`\n");
                contentBuilder.append("- **Test:** `cargo test`\n");
                contentBuilder.append("- **Single Test:** `cargo test test_name`\n");
                contentBuilder.append("- **Release Build:** `cargo build --release`\n\n");
            } else if ("Go".equals(primaryLanguage)) {
                // Go-specific build commands
                contentBuilder.append("- **Build:** `go build`\n");
                contentBuilder.append("- **Run:** `go run .`\n");
                contentBuilder.append("- **Test:** `go test ./...`\n");
                contentBuilder.append("- **Single Test:** `go test -v -run=TestName`\n");
                contentBuilder.append("- **Benchmark:** `go test -bench=. ./...`\n");
                contentBuilder.append("- **Format:** `gofmt -w .`\n\n");
            } else if ("Python".equals(primaryLanguage)) {
                // Python-specific build commands
                contentBuilder.append("- **Install:** `pip install -r requirements.txt`\n");
                contentBuilder.append("- **Run:** `python main.py`\n");
                contentBuilder.append("- **Test:** `pytest`\n");
                contentBuilder.append("- **Single Test:** `pytest tests/test_name.py`\n");
                contentBuilder.append("- **Lint:** `flake8 .`\n");
                contentBuilder.append("- **Format:** `black .`\n\n");
            } else if ("PHP".equals(primaryLanguage)) {
                // PHP-specific build commands
                contentBuilder.append("- **Install:** `composer install`\n");
                contentBuilder.append("- **Run:** `php -S localhost:8000 -t public`\n");
                contentBuilder.append("- **Test:** `vendor/bin/phpunit`\n");
                contentBuilder.append("- **Single Test:** `vendor/bin/phpunit --filter=TestName`\n");
                contentBuilder.append("- **Lint:** `vendor/bin/phpcs`\n");
                contentBuilder.append("- **Fix:** `vendor/bin/php-cs-fixer fix`\n\n");
            } else if ("Kotlin".equals(primaryLanguage)) {
                // Kotlin-specific build commands
                contentBuilder.append("- **Build:** `./gradlew build`\n");
                contentBuilder.append("- **Run:** `./gradlew run`\n");
                contentBuilder.append("- **Test:** `./gradlew test`\n");
                contentBuilder.append("- **Single Test:** `./gradlew test --tests \"*TestName*\"`\n");
                contentBuilder.append("- **Lint:** `./gradlew ktlintCheck`\n");
                contentBuilder.append("- **Format:** `./gradlew ktlintFormat`\n\n");
            } else if ("JavaScript".equals(primaryLanguage) || "TypeScript".equals(primaryLanguage)) {
                // JavaScript/TypeScript-specific build commands
                contentBuilder.append("- **Install:** `npm install`\n");
                contentBuilder.append("- **Start:** `npm start`\n");
                contentBuilder.append("- **Build:** `npm run build`\n");
                contentBuilder.append("- **Test:** `npm test`\n");
                contentBuilder.append("- **Lint:** `npm run lint`\n");
                contentBuilder.append("- **Format:** `npm run format`\n\n");
            } else if ("C/C++".equals(primaryLanguage)) {
                // C/C++-specific build commands
                contentBuilder.append("- **Configure:** `cmake -B build`\n");
                contentBuilder.append("- **Build:** `cmake --build build`\n");
                contentBuilder.append("- **Test:** `cd build && ctest`\n");
                contentBuilder.append("- **Clean:** `cmake --build build --target clean`\n");
                contentBuilder.append("- **Format:** `clang-format -i src/**/*.cpp src/**/*.h`\n\n");
            } else if (prompt.contains("Build System: Cargo")) {
                // Rust-specific build commands as fallback detection
                contentBuilder.append("- **Build:** `cargo build`\n");
                contentBuilder.append("- **Run:** `cargo run`\n");
                contentBuilder.append("- **Test:** `cargo test`\n");
                contentBuilder.append("- **Single Test:** `cargo test test_name`\n");
                contentBuilder.append("- **Release Build:** `cargo build --release`\n\n");
            } else if (prompt.contains("Build System: Gradle")) {
                // Gradle build commands
                contentBuilder.append("- **Build:** `./gradlew build`\n");
                contentBuilder.append("- **Test:** `./gradlew test`\n");
                contentBuilder.append("- **Single Test:** `./gradlew test --tests ClassName.methodName`\n\n");
            } else if (prompt.contains("Build System: Maven")) {
                // Maven build commands
                contentBuilder.append("- **Build:** `mvn clean install`\n");
                contentBuilder.append("- **Test:** `mvn test`\n");
                contentBuilder.append("- **Single Test:** `mvn test -Dtest=ClassName#methodName`\n\n");
            } else {
                // Default/Java build commands as fallback
                contentBuilder.append("- **Build:** `./gradlew build`\n");
                contentBuilder.append("- **Test:** `./gradlew test`\n");
                contentBuilder.append("- **Single Test:** `./gradlew test --tests ClassName.methodName`\n\n");
            }
        }

        // Add code style section based on language
        contentBuilder.append("### Code Style\n\n");

        if ("Rust".equals(primaryLanguage)) {
            // Rust-specific code style guidance
            contentBuilder.append("- **Formatting:** `cargo fmt`\n");
            contentBuilder.append("- **Linting:** `cargo clippy`\n");
            contentBuilder.append("- **Documentation:** Use `///` for documentation comments\n");
            contentBuilder.append("- **Naming:** Follow Rust naming conventions:\n");
            contentBuilder.append("  - snake_case for variables and functions\n");
            contentBuilder.append("  - CamelCase for types and traits\n");
            contentBuilder.append("  - SCREAMING_SNAKE_CASE for constants\n\n");
        } else if ("Go".equals(primaryLanguage)) {
            // Go-specific code style guidance
            contentBuilder.append("- **Formatting:** `gofmt` automatically formats code\n");
            contentBuilder.append("- **Linting:** `golangci-lint` or `go vet`\n");
            contentBuilder.append("- **Documentation:** Comments preceding declarations become documentation\n");
            contentBuilder.append("- **Naming:**\n");
            contentBuilder.append("  - Use MixedCaps or mixedCaps rather than underscores\n");
            contentBuilder.append("  - Acronyms should be consistently upper or lower case (e.g., HTTPServer or httpServer)\n");
            contentBuilder.append("  - Exported names must begin with a capital letter\n\n");
        } else if ("Python".equals(primaryLanguage)) {
            // Python-specific code style guidance
            contentBuilder.append("- **Formatting:** `black` for consistent formatting\n");
            contentBuilder.append("- **Linting:** `flake8` for style guide enforcement\n");
            contentBuilder.append("- **Type Hints:** Use type annotations for function parameters and return values\n");
            contentBuilder.append("- **Naming:**\n");
            contentBuilder.append("  - Use snake_case for variables, functions, methods\n");
            contentBuilder.append("  - Use CamelCase for classes\n");
            contentBuilder.append("  - Use SCREAMING_SNAKE_CASE for constants\n");
            contentBuilder.append("- **Docstrings:** Use triple quotes for documentation\n\n");
        } else if ("PHP".equals(primaryLanguage)) {
            // PHP-specific code style guidance
            contentBuilder.append("- **Formatting:** Use PSR-12 coding standard\n");
            contentBuilder.append("- **Linting:** `PHP_CodeSniffer` for style enforcement\n");
            contentBuilder.append("- **Naming:**\n");
            contentBuilder.append("  - Use camelCase for methods and variables\n");
            contentBuilder.append("  - Use PascalCase for classes\n");
            contentBuilder.append("  - Use UPPER_CASE for constants\n");
            contentBuilder.append("- **Comments:** Use PHPDoc format for documentation\n\n");
        } else if ("Kotlin".equals(primaryLanguage)) {
            // Kotlin-specific code style guidance
            contentBuilder.append("- **Formatting:** Use ktlint for consistent style\n");
            contentBuilder.append("- **Naming:**\n");
            contentBuilder.append("  - Use camelCase for properties, variables, functions\n");
            contentBuilder.append("  - Use PascalCase for classes and interfaces\n");
            contentBuilder.append("  - Prefix interfaces with 'I' is discouraged\n");
            contentBuilder.append("- **Documentation:** Use KDoc format (similar to JavaDoc)\n");
            contentBuilder.append("- **Null Safety:** Leverage Kotlin's null safety features\n\n");
        } else if ("JavaScript".equals(primaryLanguage) || "TypeScript".equals(primaryLanguage)) {
            // JavaScript/TypeScript-specific code style guidance
            String lang = "TypeScript".equals(primaryLanguage) ? "TypeScript" : "JavaScript";
            contentBuilder.append("- **Formatting:** Use Prettier for consistent formatting\n");
            contentBuilder.append("- **Linting:** ESLint for code quality and style\n");
            contentBuilder.append("- **Naming:**\n");
            contentBuilder.append("  - Use camelCase for variables, functions\n");
            contentBuilder.append("  - Use PascalCase for classes and React components\n");
            contentBuilder.append("  - Use UPPER_CASE for constants\n");
            if ("TypeScript".equals(primaryLanguage)) {
                contentBuilder.append("- **Types:** Prefer explicit type annotations over 'any'\n");
                contentBuilder.append("- **Interfaces:** Use interfaces for object shapes\n");
            }
            contentBuilder.append("- **Documentation:** Use JSDoc format for comments\n\n");
        } else if ("C/C++".equals(primaryLanguage)) {
            // C/C++-specific code style guidance
            contentBuilder.append("- **Formatting:** Use clang-format with project's style configuration\n");
            contentBuilder.append("- **Naming:**\n");
            contentBuilder.append("  - Use snake_case for functions and variables\n");
            contentBuilder.append("  - Use PascalCase or camelCase for classes\n");
            contentBuilder.append("  - Use UPPER_CASE for macros and constants\n");
            contentBuilder.append("- **Headers:** Use include guards or #pragma once\n");
            contentBuilder.append("- **Memory:** Be explicit about ownership and resource management\n\n");
        }

        // Add dependencies section
        if (prompt.contains("Dependencies:")) {
            contentBuilder.append("### Dependencies\n\n");

            if ("Rust".equals(primaryLanguage)) {
                contentBuilder.append("- **Manage with:** `Cargo.toml`\n");
                contentBuilder.append("- **Update with:** `cargo update`\n\n");

                // Add Rust project organization section
                contentBuilder.append("### Project Organization\n\n");
                contentBuilder.append("- `src/main.rs`: Binary entrypoint\n");
                contentBuilder.append("- `src/lib.rs`: Library code\n");
                contentBuilder.append("- `src/bin/`: Additional binaries\n");
                contentBuilder.append("- `tests/`: Integration tests\n\n");
            } else {
                contentBuilder.append("The project uses the following main dependencies:\n\n");

                // Extract some sample dependencies from the prompt (in a real implementation this would parse them all)
                if (prompt.contains("langchain4j")) {
                    contentBuilder.append("- **LangChain4j** - Java library for LLM applications\n");
                }
                if (prompt.contains("junit") || prompt.contains("JUnit")) {
                    contentBuilder.append("- **JUnit** - Testing framework\n");
                }
                if (prompt.contains("mockito")) {
                    contentBuilder.append("- **Mockito** - Mocking framework for tests\n");
                }
                if (prompt.contains("retrofit")) {
                    contentBuilder.append("- **Retrofit** - HTTP client for API calls\n");
                }

                contentBuilder.append("\nSee build.gradle.kts or pom.xml for the complete dependency list.\n\n");
            }
        }

        return contentBuilder.toString();
    }

    private void writeDevoxxGenieMd(String content) {
        try {
            VirtualFile devoxxGenieMdFile = baseDir.findChild("DEVOXXGENIE.md");

            // Ensure write action runs on the EDT thread
            if (ApplicationManager.getApplication().isDispatchThread()) {
                // Already on EDT thread, execute directly
                executeWriteAction(devoxxGenieMdFile, content);
            } else {
                // Execute on EDT thread
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    executeWriteAction(devoxxGenieMdFile, content);
                });
            }
        } catch (Exception e) {
            log.error("Error in write action for DEVOXXGENIE.md", e);
            throw new RuntimeException(e);
        }
    }

    private void executeWriteAction(VirtualFile devoxxGenieMdFile, String content) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                if (devoxxGenieMdFile == null) {
                    // Create the file if it doesn't exist
                    VirtualFile file = baseDir.createChildData(this, "DEVOXXGENIE.md");
                    VfsUtil.saveText(file, content);
                } else {
                    // Update the existing file
                    VfsUtil.saveText(devoxxGenieMdFile, content);
                }
            } catch (IOException e) {
                log.error("Error writing DEVOXXGENIE.md", e);
                throw new RuntimeException(e);
            }
        });
    }
}