package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.service.analyzer.tools.GlobTool;
import com.devoxx.genie.service.analyzer.util.GitignoreParser;
import com.devoxx.genie.util.ReadAccess;
import com.intellij.openapi.progress.ProgressIndicator;
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
    private final ProgressIndicator indicator;

    public ProjectAnalyzer(Project project, VirtualFile baseDir) {
        this(project, baseDir, null);
    }

    public ProjectAnalyzer(Project project, VirtualFile baseDir, @Nullable ProgressIndicator indicator) {
        this.project = project;
        this.baseDir = baseDir;
        this.indicator = indicator;

        // Initialize the GitignoreParser with support for nested .gitignore files
        this.gitignoreParser = new GitignoreParser(baseDir);
    }

    public Map<String, Object> scanProject() {
        // Run the entire scanning process in a read action
        return ReadAccess.compute(() -> {
            Map<String, Object> projectInfo = new HashMap<>();

            // IDE-agnostic detection
            setStep("Detecting build system...");
            projectInfo.put("buildSystem", detectBuildSystem());

            setStep("Detecting code style...");
            projectInfo.put("codeStyle", detectCodeStyle());

            // A single walk of the project tree feeds both language and framework detection,
            // instead of one walk for languages plus ~10 more (one per framework pattern).
            setStep("Scanning source files...");
            SourceScanResult scan = scanSourceTree();
            projectInfo.put("languages", scan.toLanguagesMap());
            projectInfo.put("frameworks", buildFrameworks(scan));

            setStep("Detecting dependencies...");
            projectInfo.put("dependencies", detectDependencies());

            // IDE-specific enhancements through extension points
            for (ProjectAnalyzerExtension extension : ProjectAnalyzerExtension.EP_NAME.getExtensions()) {
                extension.enhanceProjectInfo(project, projectInfo);
            }

            return projectInfo;
        });
    }

    /** Updates the main progress label, when an indicator is available. */
    private void setStep(@NotNull String text) {
        if (indicator != null) {
            indicator.setText(text);
        }
    }

    /**
     * Reports the file/dir currently being read and honours cancellation
     * (throws {@link com.intellij.openapi.progress.ProcessCanceledException} when the user cancels).
     */
    private void reportProgress(@NotNull VirtualFile file) {
        if (indicator != null) {
            indicator.checkCanceled();
            indicator.setText2(file.getPath());
        }
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

    /**
     * Walks the project tree exactly once, collecting both language statistics and framework
     * matches. Gitignored files and directories are skipped (their subtrees are not descended).
     */
    private @NotNull SourceScanResult scanSourceTree() {
        SourceScanResult result = new SourceScanResult(createFrameworkMatchers());
        seedLanguagesFromProjectFiles(result);

        VfsUtil.visitChildrenRecursively(baseDir, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                // Skip ignored files and do not descend into ignored directories.
                String relativePath = getRelativePath(baseDir, file);
                if (relativePath != null && gitignoreParser.shouldIgnore(relativePath, file.isDirectory())) {
                    return false;
                }

                reportProgress(file);

                if (!file.isDirectory()) {
                    countLanguageForFile(file, result);
                    matchFrameworks(file, result);
                }
                return true;
            }
        });

        return result;
    }

    /** Seeds language counts from language-specific project files (pom.xml, Cargo.toml, ...). */
    private void seedLanguagesFromProjectFiles(@NotNull SourceScanResult result) {
        if (baseDir.findChild(CARGO_TOML) != null) {
            result.addLanguage(RUST);
        }
        if (baseDir.findChild(POM_XML) != null) {
            result.addLanguage(JAVA);
        }
        if (baseDir.findChild(BUILD_GRADLE) != null || baseDir.findChild(BUILD_GRADLE_KTS) != null) {
            result.addLanguage(JAVA);
            result.addLanguage(KOTLIN);
        }
        if (baseDir.findChild(GO_MOD) != null) {
            result.addLanguage(GO);
        }
        if (baseDir.findChild(PACKAGE_JSON) != null) {
            result.addLanguage(JAVA_SCRIPT_TYPE_SCRIPT);
        }
        if (baseDir.findChild(CMAKE_LISTS_TXT) != null) {
            result.addLanguage(C_C_PLUS_PLUS);
        }
    }

    /** Counts a single file towards its language, if it is part of the project content. */
    private void countLanguageForFile(@NotNull VirtualFile file, @NotNull SourceScanResult result) {
        String extension = file.getExtension();
        if (extension == null) {
            return;
        }

        boolean isInContent = ReadAccess.compute(() ->
                ProjectFileIndex.getInstance(project).isInContent(file));
        if (!isInContent) {
            return;
        }

        String language = languageForExtension(extension);
        if (language != null) {
            result.addLanguage(language);
        }
    }

    @Nullable
    private static String languageForExtension(@NotNull String extension) {
        return switch (extension) {
            case "java" -> JAVA;
            case "kt" -> KOTLIN;
            case "php" -> PHP;
            case "py" -> PYTHON;
            case "js", "ts" -> JAVA_SCRIPT_TYPE_SCRIPT;
            case "cpp", "h", "c" -> C_C_PLUS_PLUS;
            case "rs" -> RUST;
            case "go" -> GO;
            default -> null;
        };
    }

    /** Marks any framework whose path pattern matches this file as found. */
    private void matchFrameworks(@NotNull VirtualFile file, @NotNull SourceScanResult result) {
        String path = file.getPath();
        for (FrameworkMatcher matcher : result.frameworkMatchers) {
            if (!matcher.found && matcher.matches(path)) {
                matcher.found = true;
            }
        }
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

    /** The framework path patterns evaluated during the single source-tree walk. */
    private @NotNull List<FrameworkMatcher> createFrameworkMatchers() {
        List<FrameworkMatcher> matchers = new ArrayList<>();
        // Testing frameworks
        matchers.add(new FrameworkMatcher(J_UNIT, TESTING, TEST_JAVA_PATTERN, "**/JUnit*.java"));
        matchers.add(new FrameworkMatcher(PHP_UNIT, TESTING, TEST_PHP_PATTERN, "**/PHPUnit*.php"));
        matchers.add(new FrameworkMatcher(PYTEST, TESTING, TEST_PY_PATTERN, "**/pytest*.py"));
        matchers.add(new FrameworkMatcher(GO_TESTING, TESTING, TEST_GO_PATTERN));
        matchers.add(new FrameworkMatcher(JEST, TESTING, SPEC_JS_PATTERN, "**/jest.config.js"));
        matchers.add(new FrameworkMatcher(RUST_TESTING, TESTING, TEST_RS_PATTERN));
        // Web frameworks
        matchers.add(new FrameworkMatcher(SPRING_BOOT, WEB, SPRING_BOOT_JAR, "**/SpringBoot*.java"));
        matchers.add(new FrameworkMatcher(LARAVEL, WEB, LARAVEL_PHP));
        matchers.add(new FrameworkMatcher(DJANGO, WEB, DJANGO_PY));
        matchers.add(new FrameworkMatcher(REACT, WEB, REACT_JS));
        return matchers;
    }

    /**
     * Builds the frameworks map from the single walk's matches, plus a few content-based checks
     * that only read direct child files (no tree walk required).
     */
    private @NotNull Map<String, Object> buildFrameworks(@NotNull SourceScanResult scan) {
        Map<String, Object> frameworks = new HashMap<>();
        List<String> testingFrameworks = new ArrayList<>();
        List<String> webFrameworks = new ArrayList<>();

        for (FrameworkMatcher matcher : scan.frameworkMatchers) {
            if (matcher.found) {
                (TESTING.equals(matcher.category) ? testingFrameworks : webFrameworks).add(matcher.name);
            }
        }

        // Content-based detection (direct child reads, not tree walks).
        addIfAbsent(webFrameworks, LARAVEL, findInFile(baseDir, "composer.json", "laravel/framework"));
        addIfAbsent(webFrameworks, DJANGO, findInFile(baseDir, "requirements.txt", "django"));
        addIfAbsent(webFrameworks, REACT, findInFile(baseDir, PACKAGE_JSON, "react"));

        frameworks.put(TESTING, testingFrameworks);
        frameworks.put(WEB, webFrameworks);
        frameworks.put(OTHER, new ArrayList<>());

        return frameworks;
    }

    private static void addIfAbsent(@NotNull List<String> list, @NotNull String value, boolean condition) {
        if (condition && !list.contains(value)) {
            list.add(value);
        }
    }

    /** Accumulates language statistics and framework matches gathered during a single tree walk. */
    private static final class SourceScanResult {
        private final Set<String> detectedLanguages = new HashSet<>();
        private final Map<String, Integer> languageFileCount = new HashMap<>();
        private final List<FrameworkMatcher> frameworkMatchers;

        SourceScanResult(@NotNull List<FrameworkMatcher> frameworkMatchers) {
            this.frameworkMatchers = frameworkMatchers;
        }

        void addLanguage(@NotNull String language) {
            detectedLanguages.add(language);
            languageFileCount.merge(language, 1, Integer::sum);
        }

        @NotNull Map<String, Object> toLanguagesMap() {
            Map<String, Object> languages = new HashMap<>();
            if (detectedLanguages.isEmpty()) {
                return languages;
            }

            languages.put("detected", new ArrayList<>(detectedLanguages));

            // Primary language = the one with the most files.
            String primaryLanguage = detectedLanguages.iterator().next();
            int maxFiles = 0;
            for (Map.Entry<String, Integer> entry : languageFileCount.entrySet()) {
                if (entry.getValue() > maxFiles) {
                    maxFiles = entry.getValue();
                    primaryLanguage = entry.getKey();
                }
            }
            languages.put("primary", primaryLanguage);
            return languages;
        }
    }

    /** A framework and the path patterns that, when matched by any project file, indicate its use. */
    private static final class FrameworkMatcher {
        private final String name;
        private final String category;
        private final List<Pattern> patterns;
        private boolean found;

        FrameworkMatcher(@NotNull String name, @NotNull String category, @NotNull String... globs) {
            this.name = name;
            this.category = category;
            this.patterns = new ArrayList<>(globs.length);
            for (String glob : globs) {
                patterns.add(Pattern.compile(GlobTool.convertGlobToRegex(glob)));
            }
        }

        boolean matches(@NotNull String path) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(path).find()) {
                    return true;
                }
            }
            return false;
        }
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
