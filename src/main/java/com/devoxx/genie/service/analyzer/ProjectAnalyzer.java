package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.service.analyzer.tools.GlobTool;
import com.devoxx.genie.service.analyzer.util.GitignoreParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
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

    public static final String POM_XML = "pom.xml";
    public static final String BUILD = "build";
    public static final String COMMANDS = "commands";
    public static final String BUILD_GRADLE = "build.gradle";
    public static final String BUILD_GRADLE_KTS = "build.gradle.kts";
    public static final String PACKAGE_JSON = "package.json";
    public static final String KOTLIN = "Kotlin";
    public static final String JAVA = "Java";
    public static final String JAVA_SCRIPT_TYPE_SCRIPT = "JavaScript/TypeScript";
    public static final String CMAKE_LISTS_TXT = "CMakeLists.txt";
    public static final String C_C_PLUS_PLUS = "C/C++";
    public static final String PRETTIERRC = ".prettierrc";
    public static final String ESLINTRC_JS = ".eslintrc.js";
    public static final String CHECKSTYLE_XML = "checkstyle.xml";
    public static final String PHPCS_XML = "phpcs.xml";
    public static final String CLANG_FORMAT = ".clang-format";
    public static final String RUST = "Rust";
    public static final String CARGO_TOML = "Cargo.toml";
    public static final String GO_MOD = "go.mod";
    public static final String GO = "Go";
    public static final String PYTHON = "Python";
    public static final String PHP = "PHP";
    public static final String ESLINT = "eslint";
    public static final String PRETTIER = "prettier";
    public static final String CHECKSTYLE = "checkstyle";
    public static final String PHPCS = "phpcs";
    public static final String CLANG_FORMAT1 = "clang-format";
    public static final String TEST_JAVA_PATTERN = "**/*Test.java";
    public static final String J_UNIT = "JUnit";
    public static final String TEST_PHP_PATTERN = "**/*Test.php";
    public static final String PHP_UNIT = "PHPUnit";
    public static final String TEST_PY_PATTERN = "**/*_test.py";
    public static final String PYTEST = "pytest";
    public static final String TEST_GO_PATTERN = "**/*_test.go";
    public static final String GO_TESTING = "Go testing";
    public static final String SPEC_JS_PATTERN = "**/*spec.js";
    public static final String JEST = "Jest";
    public static final String TEST_RS_PATTERN = "**/test_*.rs";
    public static final String RUST_TESTING = "Rust testing";
    public static final String SPRING_BOOT_JAR = "**/spring-boot*.jar";
    public static final String SPRING_BOOT = "Spring Boot";
    public static final String LARAVEL_PHP = "**/laravel*.php";
    public static final String LARAVEL = "Laravel";
    public static final String DJANGO_PY = "**/django/*.py";
    public static final String DJANGO = "Django";
    public static final String REACT_JS = "**/react.js";
    public static final String REACT = "React";
    public static final String TESTING = "testing";
    public static final String WEB = "web";
    public static final String OTHER = "other";

    private final Project project;
    private final VirtualFile baseDir;
    private final GitignoreParser gitignoreParser;

    public ProjectAnalyzer(Project project, VirtualFile baseDir) {
        this.project = project;
        this.baseDir = baseDir;

        // Initialize the GitignoreParser with support for nested .gitignore files
        this.gitignoreParser = new GitignoreParser(baseDir);
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
        if (baseDir.findChild(POM_XML) != null) {
            buildInfo.put("type", "Maven");
            // Add basic Maven commands
            Map<String, String> commands = new HashMap<>();
            commands.put(BUILD, "mvn clean install");
            commands.put("test", "mvn test");
            buildInfo.put(COMMANDS, commands);
        }
        else if (baseDir.findChild(BUILD_GRADLE) != null || baseDir.findChild(BUILD_GRADLE_KTS) != null) {
            buildInfo.put("type", "Gradle");
            // Add basic Gradle commands
            Map<String, String> commands = new HashMap<>();
            commands.put(BUILD, "./gradlew build");
            commands.put("test", "./gradlew test");
            buildInfo.put(COMMANDS, commands);
        }
        else if (baseDir.findChild(CMAKE_LISTS_TXT) != null) {
            buildInfo.put("type", "CMake");
            Map<String, String> commands = new HashMap<>();
            commands.put(BUILD, "cmake --build build");
            commands.put("test", "ctest");
            buildInfo.put(COMMANDS, commands);
        }
        else if (baseDir.findChild(PACKAGE_JSON) != null) {
            buildInfo.put("type", "npm/yarn");
            Map<String, String> commands = new HashMap<>();
            commands.put(BUILD, "npm run build");
            commands.put("test", "npm test");
            buildInfo.put(COMMANDS, commands);
        }
        else if (baseDir.findChild("composer.json") != null) {
            buildInfo.put("type", "Composer");
            Map<String, String> commands = new HashMap<>();
            commands.put("install", "composer install");
            commands.put("test", "composer test");
            buildInfo.put(COMMANDS, commands);
        }
        else if (baseDir.findChild(CARGO_TOML) != null) {
            buildInfo.put("type", "Cargo");
            Map<String, String> commands = new HashMap<>();
            commands.put(BUILD, "cargo build");
            commands.put("test", "cargo test");
            buildInfo.put(COMMANDS, commands);
        }

        return buildInfo;
    }

    private @NotNull Map<String, Object> detectLanguages() {
        Map<String, Object> languages = new HashMap<>();
        Set<String> detectedLanguages = new HashSet<>();
        Map<String, Integer> languageFileCount = new HashMap<>();

        // Check for language-specific project files first
        if (baseDir.findChild(CARGO_TOML) != null) {
            detectedLanguages.add(RUST);
            languageFileCount.put(RUST, 1);  // Start with 1 for the project file
        }
        if (baseDir.findChild(POM_XML) != null) {
            detectedLanguages.add(JAVA);
            languageFileCount.put(JAVA, 1);
        }
        if (baseDir.findChild(BUILD_GRADLE) != null || baseDir.findChild(BUILD_GRADLE_KTS) != null) {
            detectedLanguages.add(JAVA);
            detectedLanguages.add(KOTLIN);
            languageFileCount.put(JAVA, languageFileCount.getOrDefault(JAVA, 0) + 1);
            languageFileCount.put(KOTLIN, languageFileCount.getOrDefault(KOTLIN, 0) + 1);
        }
        if (baseDir.findChild(GO_MOD) != null) {
            detectedLanguages.add(GO);
            languageFileCount.put(GO, 1);
        }
        if (baseDir.findChild(PACKAGE_JSON) != null) {
            detectedLanguages.add(JAVA_SCRIPT_TYPE_SCRIPT);
            languageFileCount.put(JAVA_SCRIPT_TYPE_SCRIPT, 1);
        }
        if (baseDir.findChild(CMAKE_LISTS_TXT) != null) {
            detectedLanguages.add(C_C_PLUS_PLUS);
            languageFileCount.put(C_C_PLUS_PLUS, 1);
        }

        VfsUtil.visitChildrenRecursively(baseDir, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                // Check if the file should be ignored based on .gitignore rules
                String relativePath = getRelativePath(baseDir, file);
                if (relativePath != null && gitignoreParser.shouldIgnore(relativePath, file.isDirectory())) {
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
                                    detectedLanguages.add(JAVA);
                                    languageFileCount.put(JAVA, languageFileCount.getOrDefault(JAVA, 0) + 1);
                                }
                                case "kt" -> {
                                    detectedLanguages.add(KOTLIN);
                                    languageFileCount.put(KOTLIN, languageFileCount.getOrDefault(KOTLIN, 0) + 1);
                                }
                                case "php" -> {
                                    detectedLanguages.add(PHP);
                                    languageFileCount.put(PHP, languageFileCount.getOrDefault(PHP, 0) + 1);
                                }
                                case "py" -> {
                                    detectedLanguages.add(PYTHON);
                                    languageFileCount.put(PYTHON, languageFileCount.getOrDefault(PYTHON, 0) + 1);
                                }
                                case "js", "ts" -> {
                                    detectedLanguages.add(JAVA_SCRIPT_TYPE_SCRIPT);
                                    languageFileCount.put(JAVA_SCRIPT_TYPE_SCRIPT, languageFileCount.getOrDefault(JAVA_SCRIPT_TYPE_SCRIPT, 0) + 1);
                                }
                                case "cpp", "h", "c" -> {
                                    detectedLanguages.add(C_C_PLUS_PLUS);
                                    languageFileCount.put(C_C_PLUS_PLUS, languageFileCount.getOrDefault(C_C_PLUS_PLUS, 0) + 1);
                                }
                                case "rs" -> {
                                    detectedLanguages.add(RUST);
                                    languageFileCount.put(RUST, languageFileCount.getOrDefault(RUST, 0) + 1);
                                }
                                case "go" -> {
                                    detectedLanguages.add(GO);
                                    languageFileCount.put(GO, languageFileCount.getOrDefault(GO, 0) + 1);
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
            } catch (IOException ignored) {
                // ignore
            }
        }

        // Check for common code style tools
        if (baseDir.findChild(ESLINTRC_JS) != null || baseDir.findChild(".eslintrc.json") != null) {
            styleInfo.put(ESLINT, true);
        }
        if (baseDir.findChild(PRETTIERRC) != null || baseDir.findChild(".prettierrc.js") != null) {
            styleInfo.put(PRETTIER, true);
        }
        if (baseDir.findChild(CHECKSTYLE_XML) != null) {
            styleInfo.put(CHECKSTYLE, true);
        }
        if (baseDir.findChild(PHPCS_XML) != null) {
            styleInfo.put(PHPCS, true);
        }
        if (baseDir.findChild(CLANG_FORMAT) != null) {
            styleInfo.put(CLANG_FORMAT1, true);
        }

        return styleInfo;
    }

    private @NotNull Map<String, Object> detectFrameworks() {
        Map<String, Object> frameworks = new HashMap<>();
        List<String> testingFrameworks = new ArrayList<>();
        List<String> webFrameworks = new ArrayList<>();
        List<String> otherFrameworks = new ArrayList<>();

        // Check for testing frameworks in various languages
        if (containsFile(baseDir, TEST_JAVA_PATTERN) || containsFile(baseDir, "**/JUnit*.java")) {
            testingFrameworks.add(J_UNIT);
        }
        if (containsFile(baseDir, TEST_PHP_PATTERN) || containsFile(baseDir, "**/PHPUnit*.php")) {
            testingFrameworks.add(PHP_UNIT);
        }
        if (containsFile(baseDir, TEST_PY_PATTERN) || containsFile(baseDir, "**/pytest*.py")) {
            testingFrameworks.add(PYTEST);
        }
        if (containsFile(baseDir, TEST_GO_PATTERN)) {
            testingFrameworks.add(GO_TESTING);
        }
        if (containsFile(baseDir, SPEC_JS_PATTERN) || containsFile(baseDir, "**/jest.config.js")) {
            testingFrameworks.add(JEST);
        }
        if (containsFile(baseDir, TEST_RS_PATTERN)) {
            testingFrameworks.add(RUST_TESTING);
        }

        // Web frameworks detection
        if (containsFile(baseDir, SPRING_BOOT_JAR) || containsFile(baseDir, "**/SpringBoot*.java")) {
            webFrameworks.add(SPRING_BOOT);
        }
        if (containsFile(baseDir, LARAVEL_PHP) || findInFile(baseDir, "composer.json", "laravel/framework")) {
            webFrameworks.add(LARAVEL);
        }
        if (containsFile(baseDir, DJANGO_PY) || findInFile(baseDir, "requirements.txt", "django")) {
            webFrameworks.add(DJANGO);
        }
        if (containsFile(baseDir, REACT_JS) || findInFile(baseDir, PACKAGE_JSON, "react")) {
            webFrameworks.add(REACT);
        }

        frameworks.put(TESTING, testingFrameworks);
        frameworks.put(WEB, webFrameworks);
        frameworks.put(OTHER, otherFrameworks);

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
                String text = VfsUtilCore.loadText(file);
                return text.contains(content);
            } catch (IOException e) {
                // Log error or handle exception
                return false;
            }
        }
        return false;
    }

    /**
     * Detects dependencies from build files.
     * 
     * @return A map with dependency information
     */
    private @NotNull Map<String, Object> detectDependencies() {
        Map<String, Object> dependenciesInfo = new HashMap<>();
        List<Map<String, String>> dependencies = new ArrayList<>();

        // Extract dependencies from different build systems
        extractGradleKotlinDependencies(dependencies);
        extractMavenDependencies(dependencies);
        extractNpmDependencies(dependencies);
        
        dependenciesInfo.put("list", dependencies);
        return dependenciesInfo;
    }
    
    /**
     * Extracts dependencies from a Gradle Kotlin DSL build file.
     * 
     * @param dependencies List to add the found dependencies to
     */
    private void extractGradleKotlinDependencies(List<Map<String, String>> dependencies) {
        VirtualFile buildFile = baseDir.findChild(BUILD_GRADLE_KTS);
        if (buildFile == null) {
            return;
        }
        
        String content = readFileContent(buildFile);
        if (content.isEmpty()) {
            return;
        }
        
        // Create pattern for different dependency formats
        java.util.regex.Pattern pattern = createGradleKotlinDependencyPattern();
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            processGradleKotlinMatch(matcher, dependencies);
        }
    }
    
    /**
     * Creates regex pattern for Gradle Kotlin DSL dependencies.
     */
    private java.util.regex.Pattern createGradleKotlinDependencyPattern() {
        return java.util.regex.Pattern.compile(
                "implementation\\\\(\\\"([^\\\"]+)\\\"\\\\)" +
                "|implementation\\\\(\\\"([^:]+):([^:]+):([^\\\"]+)\\\"\\\\)" +
                "|testImplementation\\\\(\\\"([^:]+):([^:]+):([^\\\"]+)\\\"\\\\)");
    }
    
    /**
     * Processes a match from the Gradle Kotlin DSL dependency pattern.
     */
    private void processGradleKotlinMatch(java.util.regex.Matcher matcher, List<Map<String, String>> dependencies) {
        if (matcher.group(1) != null) {
            // Process full string format (e.g., "group:name:version")
            processDependencyString(matcher.group(1), dependencies, "implementation");
        } else if (matcher.group(2) != null) {
            // Process implementation dependency
            addDependency(dependencies, matcher.group(2), matcher.group(3), 
                    matcher.group(4), "implementation");
        } else if (matcher.group(5) != null) {
            // Process test dependency
            addDependency(dependencies, matcher.group(5), matcher.group(6), 
                    matcher.group(7), "test");
        }
    }
    
    /**
     * Processes a dependency string in format "group:name:version".
     */
    private void processDependencyString(String depString, List<Map<String, String>> dependencies, String scope) {
        String[] parts = depString.split(":");
        if (parts.length < 3) {
            return;
        }
        
        addDependency(dependencies, parts[0], parts[1], parts[2], scope);
    }
    
    /**
     * Adds a dependency with the given details.
     */
    private void addDependency(List<Map<String, String>> dependencies, 
                               String group, String name, String version, String scope) {
        Map<String, String> dep = new HashMap<>();
        dep.put("group", group);
        dep.put("name", name);
        dep.put("version", version);
        dep.put("scope", scope);
        dependencies.add(dep);
    }
    
    /**
     * Extracts dependencies from a Maven POM file.
     * 
     * @param dependencies List to add the found dependencies to
     */
    private void extractMavenDependencies(List<Map<String, String>> dependencies) {
        VirtualFile pomFile = baseDir.findChild(POM_XML);
        if (pomFile == null) {
            return;
        }
        
        String content = readFileContent(pomFile);
        if (content.isEmpty()) {
            return;
        }
        
        java.util.regex.Pattern pattern = createMavenDependencyPattern();
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            processMavenDependencyMatch(matcher, dependencies);
        }
    }
    
    /**
     * Creates regex pattern for Maven dependencies.
     */
    private java.util.regex.Pattern createMavenDependencyPattern() {
        return java.util.regex.Pattern.compile(
                "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)" +
                "</artifactId>\\s*<version>([^<]+)</version>(\\s*<scope>([^<]+)</scope>)?");
    }
    
    /**
     * Processes a match from the Maven dependency pattern.
     */
    private void processMavenDependencyMatch(java.util.regex.Matcher matcher, List<Map<String, String>> dependencies) {
        String scope = matcher.group(5) != null ? matcher.group(5) : "compile";
        addDependency(dependencies, matcher.group(1), matcher.group(2), matcher.group(3), scope);
    }
    
    /**
     * Extracts dependencies from a package.json file.
     * 
     * @param dependencies List to add the found dependencies to
     */
    private void extractNpmDependencies(List<Map<String, String>> dependencies) {
        VirtualFile packageFile = baseDir.findChild(PACKAGE_JSON);
        if (packageFile == null) {
            return;
        }
        
        String content = readFileContent(packageFile);
        if (content.isEmpty() || !content.contains("\"dependencies\"")) {
            return;
        }
        
        // Add a placeholder - we'd need a proper JSON parser for detailed extraction
        Map<String, String> dep = new HashMap<>();
        dep.put("name", "npm-dependencies");
        dep.put("note", "See package.json for details");
        dependencies.add(dep);
    }
    
    /**
     * Reads content from a file, handling exceptions.
     */
    private String readFileContent(VirtualFile file) {
        try {
            return VfsUtilCore.loadText(file);
        } catch (IOException e) {
            return "";
        }
    }
}
