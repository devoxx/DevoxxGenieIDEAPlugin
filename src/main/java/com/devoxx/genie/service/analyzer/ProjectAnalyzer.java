package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.service.analyzer.tools.GlobTool;
import com.devoxx.genie.service.analyzer.util.GitignoreParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Main class for scanning and analyzing project structure.
 * <p>
 * Features:
 * - Detects build systems, languages, code styles, frameworks, and dependencies
 * - Respects .gitignore rules to exclude directories and files that should be ignored
 * - Uses extension points for language-specific analysis
 * </p>
 */
public class ProjectAnalyzer {
    private final Project project;
    private final VirtualFile baseDir;
    private final GitignoreParser gitignoreParser;

    public ProjectAnalyzer(Project project, VirtualFile baseDir) {
        this.project = project;
        this.baseDir = baseDir;

        // Initialize the GitignoreParser with support for nested .gitignore files
//        ProjectScannerLogPanel.log("Initializing GitignoreParser with support for nested .gitignore files...");
        this.gitignoreParser = new GitignoreParser(baseDir);
//        ProjectScannerLogPanel.log("GitignoreParser initialization complete");
    }

    public Map<String, Object> scanProject() {
        // Run the entire scanning process in a read action
        return ReadAction.compute(() -> {
            Map<String, Object> projectInfo = new HashMap<>();

            // IDE-agnostic detection
            projectInfo.put("buildSystem", detectBuildSystem());
            projectInfo.put("languages", detectLanguages());
            projectInfo.put("codeStyle", detectCodeStyle());
            projectInfo.put("frameworks", detectFrameworks());
            projectInfo.put("dependencies", detectDependencies());

            // IDE-specific enhancements through extension points
            for (ProjectAnalyzerExtension extension : ProjectAnalyzerExtension.EP_NAME.getExtensions()) {
                extension.enhanceProjectInfo(project, projectInfo);
            }

            return projectInfo;
        });
    }

    private @NotNull Map<String, Object> detectBuildSystem() {
        Map<String, Object> buildInfo = new HashMap<>();

        // Check for common build files
        if (baseDir.findChild("pom.xml") != null) {
            buildInfo.put("type", "Maven");
            // Add basic Maven commands
            Map<String, String> commands = new HashMap<>();
            commands.put("build", "mvn clean install");
            commands.put("test", "mvn test");
            buildInfo.put("commands", commands);
        }
        else if (baseDir.findChild("build.gradle") != null || baseDir.findChild("build.gradle.kts") != null) {
            buildInfo.put("type", "Gradle");
            // Add basic Gradle commands
            Map<String, String> commands = new HashMap<>();
            commands.put("build", "./gradlew build");
            commands.put("test", "./gradlew test");
            buildInfo.put("commands", commands);
        }
        else if (baseDir.findChild("CMakeLists.txt") != null) {
            buildInfo.put("type", "CMake");
            Map<String, String> commands = new HashMap<>();
            commands.put("build", "cmake --build build");
            commands.put("test", "ctest");
            buildInfo.put("commands", commands);
        }
        else if (baseDir.findChild("package.json") != null) {
            buildInfo.put("type", "npm/yarn");
            Map<String, String> commands = new HashMap<>();
            commands.put("build", "npm run build");
            commands.put("test", "npm test");
            buildInfo.put("commands", commands);
        }
        else if (baseDir.findChild("composer.json") != null) {
            buildInfo.put("type", "Composer");
            Map<String, String> commands = new HashMap<>();
            commands.put("install", "composer install");
            commands.put("test", "composer test");
            buildInfo.put("commands", commands);
        }
        else if (baseDir.findChild("Cargo.toml") != null) {
            buildInfo.put("type", "Cargo");
            Map<String, String> commands = new HashMap<>();
            commands.put("build", "cargo build");
            commands.put("test", "cargo test");
            buildInfo.put("commands", commands);
        }

        return buildInfo;
    }

    private @NotNull Map<String, Object> detectLanguages() {
//        ProjectScannerLogPanel.log("Starting language detection for project: " + project.getName());
//        ProjectScannerLogPanel.log("Base directory: " + baseDir.getPath());

        Map<String, Object> languages = new HashMap<>();
        Set<String> detectedLanguages = new HashSet<>();
        Map<String, Integer> languageFileCount = new HashMap<>();

        // Check for language-specific project files first
//        ProjectScannerLogPanel.log("Checking for language-specific project files...");
        if (baseDir.findChild("Cargo.toml") != null) {
//            ProjectScannerLogPanel.log("Found Cargo.toml - Rust project detected");
            detectedLanguages.add("Rust");
            languageFileCount.put("Rust", 1);  // Start with 1 for the project file
        }
        if (baseDir.findChild("pom.xml") != null) {
//            ProjectScannerLogPanel.log("Found pom.xml - Java project detected");
            detectedLanguages.add("Java");
            languageFileCount.put("Java", 1);
        }
        if (baseDir.findChild("build.gradle") != null || baseDir.findChild("build.gradle.kts") != null) {
//            ProjectScannerLogPanel.log("Found Gradle build file - Java/Kotlin project detected");
            detectedLanguages.add("Java");
            detectedLanguages.add("Kotlin");
            languageFileCount.put("Java", languageFileCount.getOrDefault("Java", 0) + 1);
            languageFileCount.put("Kotlin", languageFileCount.getOrDefault("Kotlin", 0) + 1);
        }
        if (baseDir.findChild("go.mod") != null) {
//            ProjectScannerLogPanel.log("Found go.mod - Go project detected");
            detectedLanguages.add("Go");
            languageFileCount.put("Go", 1);
        }
        if (baseDir.findChild("package.json") != null) {
//            ProjectScannerLogPanel.log("Found package.json - JavaScript/TypeScript project detected");
            detectedLanguages.add("JavaScript/TypeScript");
            languageFileCount.put("JavaScript/TypeScript", 1);
        }
        if (baseDir.findChild("CMakeLists.txt") != null) {
//            ProjectScannerLogPanel.log("Found CMakeLists.txt - C/C++ project detected");
            detectedLanguages.add("C/C++");
            languageFileCount.put("C/C++", 1);
        }

        // File extension-based detection
//        ProjectScannerLogPanel.log("Starting file extension-based detection...");

        VfsUtil.visitChildrenRecursively(baseDir, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                // Check if the file should be ignored based on .gitignore rules
                String relativePath = getRelativePath(baseDir, file);
                if (relativePath != null && gitignoreParser.shouldIgnore(relativePath, file.isDirectory())) {
//                    if (file.isDirectory()) {
//                        ProjectScannerLogPanel.log("Skipping directory based on .gitignore: " + relativePath);
//                    }
                    return true; // Skip this file and its children
                }

                if (!file.isDirectory()) {
                    // Use ReadAction to safely access the ProjectFileIndex
                    boolean isInContent = ReadAction.compute(() ->
                            ProjectFileIndex.getInstance(project).isInContent(file));

                    if (isInContent) {
                        String extension = file.getExtension();
                        if (extension != null) {
                            switch (extension) {
                                case "java" -> {
                                    detectedLanguages.add("Java");
                                    languageFileCount.put("Java", languageFileCount.getOrDefault("Java", 0) + 1);
                                }
                                case "kt" -> {
                                    detectedLanguages.add("Kotlin");
                                    languageFileCount.put("Kotlin", languageFileCount.getOrDefault("Kotlin", 0) + 1);
                                }
                                case "php" -> {
                                    detectedLanguages.add("PHP");
                                    languageFileCount.put("PHP", languageFileCount.getOrDefault("PHP", 0) + 1);
                                }
                                case "py" -> {
                                    detectedLanguages.add("Python");
                                    languageFileCount.put("Python", languageFileCount.getOrDefault("Python", 0) + 1);
                                }
                                case "js", "ts" -> {
                                    detectedLanguages.add("JavaScript/TypeScript");
                                    languageFileCount.put("JavaScript/TypeScript", languageFileCount.getOrDefault("JavaScript/TypeScript", 0) + 1);
                                }
                                case "cpp", "h", "c" -> {
                                    detectedLanguages.add("C/C++");
                                    languageFileCount.put("C/C++", languageFileCount.getOrDefault("C/C++", 0) + 1);
                                }
                                case "rs" -> {
                                    detectedLanguages.add("Rust");
                                    languageFileCount.put("Rust", languageFileCount.getOrDefault("Rust", 0) + 1);
                                }
                                case "go" -> {
                                    detectedLanguages.add("Go");
                                    languageFileCount.put("Go", languageFileCount.getOrDefault("Go", 0) + 1);
                                }
                            }
                        }
                    }
                }
                return true;
            }
        });

        // Determine primary language based on file count
        if (!detectedLanguages.isEmpty()) {
            languages.put("detected", new ArrayList<>(detectedLanguages));

            // Log all detected languages with their file counts
//            ProjectScannerLogPanel.log("File counts by language:");
//            for (Map.Entry<String, Integer> entry : languageFileCount.entrySet()) {
//                ProjectScannerLogPanel.log("  - " + entry.getKey() + ": " + entry.getValue() + " files");
//            }

            // Find the language with the most files
            String primaryLanguage = detectedLanguages.iterator().next();
            int maxFiles = 0;

            for (Map.Entry<String, Integer> entry : languageFileCount.entrySet()) {
                if (entry.getValue() > maxFiles) {
                    maxFiles = entry.getValue();
                    primaryLanguage = entry.getKey();
                }
            }

            languages.put("primary", primaryLanguage);
//            ProjectScannerLogPanel.log("Primary language detected: " + primaryLanguage);
        }

        return languages;
    }

    private @NotNull Map<String, Object> detectCodeStyle() {
        Map<String, Object> styleInfo = new HashMap<>();

        // Check for language-agnostic code style files
        VirtualFile editorConfig = baseDir.findChild(".editorconfig");
        if (editorConfig != null) {
            try {
                styleInfo.put("editorconfig", VfsUtil.loadText(editorConfig));
            } catch (IOException ignored) {}
        }

        // Check for common code style tools
        if (baseDir.findChild(".eslintrc.js") != null || baseDir.findChild(".eslintrc.json") != null) {
            styleInfo.put("eslint", true);
        }
        if (baseDir.findChild(".prettierrc") != null || baseDir.findChild(".prettierrc.js") != null) {
            styleInfo.put("prettier", true);
        }
        if (baseDir.findChild("checkstyle.xml") != null) {
            styleInfo.put("checkstyle", true);
        }
        if (baseDir.findChild("phpcs.xml") != null) {
            styleInfo.put("phpcs", true);
        }
        if (baseDir.findChild(".clang-format") != null) {
            styleInfo.put("clang-format", true);
        }

        return styleInfo;
    }

    private @NotNull Map<String, Object> detectFrameworks() {
        Map<String, Object> frameworks = new HashMap<>();
        List<String> testingFrameworks = new ArrayList<>();
        List<String> webFrameworks = new ArrayList<>();
        List<String> otherFrameworks = new ArrayList<>();

        // Check for testing frameworks in various languages
        if (containsFile(baseDir, "**/*Test.java") || containsFile(baseDir, "**/JUnit*.java")) {
            testingFrameworks.add("JUnit");
        }
        if (containsFile(baseDir, "**/*Test.php") || containsFile(baseDir, "**/PHPUnit*.php")) {
            testingFrameworks.add("PHPUnit");
        }
        if (containsFile(baseDir, "**/*_test.py") || containsFile(baseDir, "**/pytest*.py")) {
            testingFrameworks.add("pytest");
        }
        if (containsFile(baseDir, "**/*_test.go")) {
            testingFrameworks.add("Go testing");
        }
        if (containsFile(baseDir, "**/*spec.js") || containsFile(baseDir, "**/jest.config.js")) {
            testingFrameworks.add("Jest");
        }
        if (containsFile(baseDir, "**/test_*.rs")) {
            testingFrameworks.add("Rust testing");
        }

        // Web frameworks detection
        if (containsFile(baseDir, "**/spring-boot*.jar") || containsFile(baseDir, "**/SpringBoot*.java")) {
            webFrameworks.add("Spring Boot");
        }
        if (containsFile(baseDir, "**/laravel*.php") || findInFile(baseDir, "composer.json", "laravel/framework")) {
            webFrameworks.add("Laravel");
        }
        if (containsFile(baseDir, "**/django/*.py") || findInFile(baseDir, "requirements.txt", "django")) {
            webFrameworks.add("Django");
        }
        if (containsFile(baseDir, "**/react.js") || findInFile(baseDir, "package.json", "react")) {
            webFrameworks.add("React");
        }

        frameworks.put("testing", testingFrameworks);
        frameworks.put("web", webFrameworks);
        frameworks.put("other", otherFrameworks);

        return frameworks;
    }

    private boolean containsFile(VirtualFile dir, String pattern) {
        // Use a direct approach without GlobalSearchScope for better reliability
        Pattern regex = Pattern.compile(GlobTool.convertGlobToRegex(pattern));

        final boolean[] found = {false};

        // Manually search through the directory structure
        VfsUtil.visitChildrenRecursively(dir, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                // Check if the file should be ignored based on .gitignore rules
                String relativePath = getRelativePath(dir, file);
                if (relativePath != null && gitignoreParser.shouldIgnore(relativePath, file.isDirectory())) {
                    return true; // Skip this file and its children
                }

                // Only check file contents, not directories
                if (!file.isDirectory()) {
                    // Check if the path matches our pattern
                    if (regex.matcher(file.getPath()).find()) {
                        found[0] = true;
                        return false; // Stop visiting once found
                    }
                }
                return true;
            }
        });

        return found[0];
    }

    /**
     * Gets the relative path of a file compared to a base directory
     *
     * @param baseDir The base directory
     * @param file The file to get the relative path for
     * @return The relative path, or null if the file is not under the base directory
     */
    private @Nullable String getRelativePath(@NotNull VirtualFile baseDir, @NotNull VirtualFile file) {
        String basePath = baseDir.getPath();
        String filePath = file.getPath();

        if (filePath.startsWith(basePath)) {
            // Remove the base path and any leading slash to get the relative path
            String relativePath = filePath.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }

        return null;
    }

    private boolean findInFile(@NotNull VirtualFile dir, String fileName, String content) {
        VirtualFile file = dir.findChild(fileName);
        if (file != null && file.exists() && !file.isDirectory()) {
            try {
                String text = VfsUtil.loadText(file);
                return text.contains(content);
            } catch (IOException e) {
                // Log error or handle exception
                return false;
            }
        }
        return false;
    }

    private @NotNull Map<String, Object> detectDependencies() {
        Map<String, Object> dependenciesInfo = new HashMap<>();
        List<Map<String, String>> dependencies = new ArrayList<>();

        // Check for Gradle dependencies (Kotlin DSL)
        VirtualFile buildGradleKts = baseDir.findChild("build.gradle.kts");
        if (buildGradleKts != null) {
            try {
                String content = VfsUtil.loadText(buildGradleKts);
                // Simple regex-based extraction for demonstration
                // A more robust parser would be better for production use
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "implementation\\(\"([^\"]+)\"\\)" +
                                "|implementation\\(\"([^:]+):([^:]+):([^\"]+)\"\\)" +
                                "|testImplementation\\(\"([^:]+):([^:]+):([^\"]+)\"\\)");

                java.util.regex.Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    Map<String, String> dep = new HashMap<>();
                    if (matcher.group(1) != null) {
                        // Full dependency string
                        String[] parts = matcher.group(1).split(":");
                        if (parts.length >= 3) {
                            dep.put("group", parts[0]);
                            dep.put("name", parts[1]);
                            dep.put("version", parts[2]);
                            dep.put("scope", "implementation");
                            dependencies.add(dep);
                        }
                    } else if (matcher.group(2) != null) {
                        // Implementation dependency
                        dep.put("group", matcher.group(2));
                        dep.put("name", matcher.group(3));
                        dep.put("version", matcher.group(4));
                        dep.put("scope", "implementation");
                        dependencies.add(dep);
                    } else if (matcher.group(5) != null) {
                        // Test dependency
                        dep.put("group", matcher.group(5));
                        dep.put("name", matcher.group(6));
                        dep.put("version", matcher.group(7));
                        dep.put("scope", "test");
                        dependencies.add(dep);
                    }
                }
            } catch (IOException e) {
                // Handle exception
            }
        }

        // Check for Gradle dependencies (Groovy DSL)
        VirtualFile buildGradle = baseDir.findChild("build.gradle");
        if (buildGradle != null) {
            try {
                String content = VfsUtil.loadText(buildGradle);
                // Similar pattern matching for Groovy DSL
                // TODO - Implement Gradle dependency extraction

            } catch (IOException e) {
                // Handle exception
            }
        }

        // Check for Maven dependencies
        VirtualFile pomXml = baseDir.findChild("pom.xml");
        if (pomXml != null) {
            try {
                String content = VfsUtil.loadText(pomXml);
                // Simple regex for Maven dependencies
                // A more robust XML parser would be better for production
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]+)</version>(\\s*<scope>([^<]+)</scope>)?");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    Map<String, String> dep = new HashMap<>();
                    dep.put("group", matcher.group(1));
                    dep.put("name", matcher.group(2));
                    dep.put("version", matcher.group(3));
                    dep.put("scope", matcher.group(5) != null ? matcher.group(5) : "compile");
                    dependencies.add(dep);
                }
            } catch (IOException e) {
                // Handle exception
            }
        }

        // Check for NPM dependencies
        VirtualFile packageJson = baseDir.findChild("package.json");
        if (packageJson != null) {
            try {
                String content = VfsUtil.loadText(packageJson);
                // Simple detection for demonstration
                // A proper JSON parser should be used in production
                if (content.contains("\"dependencies\"")) {
                    // Add a placeholder for npm projects
                    Map<String, String> dep = new HashMap<>();
                    dep.put("name", "npm-dependencies");
                    dep.put("note", "See package.json for details");
                    dependencies.add(dep);
                }
            } catch (IOException e) {
                // Handle exception
            }
        }

        // TODO Add more dependency detection for other build systems, like Cargo, Composer, etc.

        dependenciesInfo.put("list", dependencies);
        return dependenciesInfo;
    }
}