package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.service.analyzer.tools.BashTool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Scans a project directory to gather information about the build system,
 * frameworks, and tools used.
 */
public class EnhancedProjectAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectAnalyzer.class);
    private final String workingDir;
    private final BashTool bashTool;

    public EnhancedProjectAnalyzer(String workingDir) {
        this.workingDir = workingDir;
        this.bashTool = new BashTool();
    }

    /**
     * Scans the project and returns a map of project information.
     *
     * @return Map containing project information
     */
    public Map<String, Object> scanProject() {
        Map<String, Object> projectInfo = new HashMap<>();

        try {
            // Detect build system
            projectInfo.put("buildSystem", detectBuildSystem());

            // Detect frameworks
            projectInfo.put("frameworks", detectFrameworks());

            // Detect code style guidelines
            projectInfo.put("codeStyle", detectCodeStyle());

            // Detect IDE settings
            projectInfo.put("ideSettings", detectIdeSettings());

            // Detect languages used
            projectInfo.put("languages", detectLanguages());

            // Get basic project structure
            projectInfo.put("structure", getProjectStructure());

        } catch (Exception e) {
            LOG.error("Error scanning project: {}", e.getMessage(), e);
            projectInfo.put("error", e.getMessage());
        }

        return projectInfo;
    }

    /**
     * Detects the build system used in the project.
     *
     * @return Map containing build system information
     */
    @NotNull
    private Map<String, Object> detectBuildSystem() {
        Map<String, Object> buildInfo = new HashMap<>();

        // Check for Maven
        Path pomPath = Paths.get(workingDir, "pom.xml");
        if (Files.exists(pomPath)) {
            buildInfo.put("type", "Maven");
            buildInfo.put("file", "pom.xml");

            try {
                // Try to extract Maven version
                Map<String, Object> mvnVersionResult = bashTool.bash("mvn --version", 5000);
                if (mvnVersionResult.containsKey("stdout") && mvnVersionResult.get("stdout") != null) {
                    String mvnVersionOutput = (String) mvnVersionResult.get("stdout");
                    buildInfo.put("version", extractVersion(mvnVersionOutput, "Apache Maven"));
                }

                // Extract basic commands
                buildInfo.put("commands", detectMavenCommands());
            } catch (Exception e) {
                LOG.error("Error getting Maven info: {}", e.getMessage());
            }

            return buildInfo;
        }

        // Check for Gradle
        Path gradlePath = Paths.get(workingDir, "build.gradle");
        Path gradleKtsPath = Paths.get(workingDir, "build.gradle.kts");
        if (Files.exists(gradlePath) || Files.exists(gradleKtsPath)) {
            buildInfo.put("type", "Gradle");
            buildInfo.put("file", Files.exists(gradlePath) ? "build.gradle" : "build.gradle.kts");

            try {
                // Try to extract Gradle version
                Map<String, Object> gradleVersionResult = bashTool.bash("gradle --version", 5000);
                if (gradleVersionResult.containsKey("stdout") && gradleVersionResult.get("stdout") != null) {
                    String gradleVersionOutput = (String) gradleVersionResult.get("stdout");
                    buildInfo.put("version", extractVersion(gradleVersionOutput, "Gradle"));
                }

                // Extract basic commands
                buildInfo.put("commands", detectGradleCommands());
            } catch (Exception e) {
                LOG.error("Error getting Gradle info: {}", e.getMessage());
            }

            return buildInfo;
        }

        // Check for npm
        Path packageJsonPath = Paths.get(workingDir, "package.json");
        if (Files.exists(packageJsonPath)) {
            buildInfo.put("type", "npm");
            buildInfo.put("file", "package.json");

            try {
                // Try to extract npm version
                Map<String, Object> npmVersionResult = bashTool.bash("npm --version", 5000);
                if (npmVersionResult.containsKey("stdout") && npmVersionResult.get("stdout") != null) {
                    String npmVersionOutput = (String) npmVersionResult.get("stdout");
                    buildInfo.put("version", npmVersionOutput.trim());
                }

                // Extract scripts from package.json
                buildInfo.put("commands", detectNpmCommands(packageJsonPath));
            } catch (Exception e) {
                LOG.error("Error getting npm info: {}", e.getMessage());
            }

            return buildInfo;
        }

        // If no specific build system is detected
        buildInfo.put("type", "Unknown");
        return buildInfo;
    }

    /**
     * Detects common frameworks used in the project.
     *
     * @return Map containing framework information
     */
    @NotNull
    private Map<String, Object> detectFrameworks() {
        Map<String, Object> frameworkInfo = new HashMap<>();

        // Detect test frameworks
        frameworkInfo.put("testing", detectTestFrameworks());

        // Detect web frameworks
        frameworkInfo.put("web", detectWebFrameworks());

        // Detect other frameworks
        frameworkInfo.put("other", detectOtherFrameworks());

        return frameworkInfo;
    }

    /**
     * Detects test frameworks used in the project.
     *
     * @return List of detected test frameworks
     */
    @NotNull
    private List<String> detectTestFrameworks() {
        List<String> testFrameworks = new ArrayList<>();

        try {
            // Look for JUnit
            boolean hasJUnitFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasJUnitFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import org.junit.") || content.contains("import org.junit5.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasJUnitFiles) {
                // Determine JUnit version
                boolean isJUnit5 = false;
                try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                    isJUnit5 = walk
                            .filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .anyMatch(p -> {
                                try {
                                    String content = Files.readString(p);
                                    return content.contains("import org.junit.jupiter.") ||
                                            content.contains("@ExtendWith") ||
                                            content.contains("@ParameterizedTest");
                                } catch (IOException e) {
                                    return false;
                                }
                            });
                }

                testFrameworks.add(isJUnit5 ? "JUnit 5" : "JUnit 4");
            }

            // Look for TestNG
            boolean hasTestNGFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasTestNGFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import org.testng.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasTestNGFiles) {
                testFrameworks.add("TestNG");
            }

            // Look for Mockito
            boolean hasMockitoFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasMockitoFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import org.mockito.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasMockitoFiles) {
                testFrameworks.add("Mockito");
            }

        } catch (Exception e) {
            LOG.error("Error detecting test frameworks: {}", e.getMessage());
        }

        return testFrameworks;
    }

    /**
     * Detects web frameworks used in the project.
     *
     * @return List of detected web frameworks
     */
    @NotNull
    private List<String> detectWebFrameworks() {
        List<String> webFrameworks = new ArrayList<>();

        try {
            // Look for Spring
            boolean hasSpringFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasSpringFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import org.springframework.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasSpringFiles) {
                boolean isSpringBoot = false;
                try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                    isSpringBoot = walk
                            .filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .anyMatch(p -> {
                                try {
                                    String content = Files.readString(p);
                                    return content.contains("import org.springframework.boot.") ||
                                            content.contains("@SpringBootApplication");
                                } catch (IOException e) {
                                    return false;
                                }
                            });
                }

                webFrameworks.add(isSpringBoot ? "Spring Boot" : "Spring Framework");
            }

            // Look for Jakarta EE / JavaEE
            boolean hasJakartaEEFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasJakartaEEFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import jakarta.") ||
                                        content.contains("import javax.servlet.") ||
                                        content.contains("import javax.ejb.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasJakartaEEFiles) {
                webFrameworks.add("Jakarta EE / JavaEE");
            }

            // Look for Angular
            Path angularJsonPath = Paths.get(workingDir, "angular.json");
            if (Files.exists(angularJsonPath)) {
                webFrameworks.add("Angular");
            }

            // Look for React
            boolean hasReactFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasReactFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".jsx") || p.toString().endsWith(".tsx"))
                        .anyMatch(p -> true);
            }

            if (hasReactFiles || fileContainsText(Paths.get(workingDir, "package.json"), "react")) {
                webFrameworks.add("React");
            }

        } catch (Exception e) {
            LOG.error("Error detecting web frameworks: {}", e.getMessage());
        }

        return webFrameworks;
    }

    /**
     * Detects other frameworks used in the project.
     *
     * @return List of detected frameworks
     */
    @NotNull
    private List<String> detectOtherFrameworks() {
        List<String> otherFrameworks = new ArrayList<>();

        try {
            // Look for Hibernate
            boolean hasHibernateFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasHibernateFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import org.hibernate.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasHibernateFiles) {
                otherFrameworks.add("Hibernate");
            }

            // Look for Lombok
            boolean hasLombokFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasLombokFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import lombok.") ||
                                        content.contains("@Getter") ||
                                        content.contains("@Setter") ||
                                        content.contains("@Data");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasLombokFiles) {
                otherFrameworks.add("Lombok");
            }

            // Look for Apache Commons
            boolean hasCommonsFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasCommonsFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import org.apache.commons.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasCommonsFiles) {
                otherFrameworks.add("Apache Commons");
            }

            // Look for Guava
            boolean hasGuavaFiles = false;
            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                hasGuavaFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(p -> {
                            try {
                                String content = Files.readString(p);
                                return content.contains("import com.google.common.");
                            } catch (IOException e) {
                                return false;
                            }
                        });
            }

            if (hasGuavaFiles) {
                otherFrameworks.add("Google Guava");
            }

        } catch (Exception e) {
            LOG.error("Error detecting other frameworks: {}", e.getMessage());
        }

        return otherFrameworks;
    }

    /**
     * Detects code style guidelines used in the project.
     *
     * @return Map containing code style information
     */
    @NotNull
    private Map<String, Object> detectCodeStyle() {
        Map<String, Object> codeStyleInfo = new HashMap<>();

        try {
            // Check for .editorconfig
            Path editorConfigPath = Paths.get(workingDir, ".editorconfig");
            if (Files.exists(editorConfigPath)) {
                codeStyleInfo.put("editorconfig", Files.readString(editorConfigPath));
            }

            // Check for checkstyle.xml
            Path checkstylePath = Paths.get(workingDir, "checkstyle.xml");
            if (!Files.exists(checkstylePath)) {
                checkstylePath = Paths.get(workingDir, "config/checkstyle/checkstyle.xml");
            }
            if (Files.exists(checkstylePath)) {
                codeStyleInfo.put("checkstyle", true);
            }

            // Check for PMD
            Path pmdPath = Paths.get(workingDir, "pmd-ruleset.xml");
            if (!Files.exists(pmdPath)) {
                pmdPath = Paths.get(workingDir, "config/pmd/ruleset.xml");
            }
            if (Files.exists(pmdPath)) {
                codeStyleInfo.put("pmd", true);
            }

            // Check for SpotBugs
            Path spotbugsPath = Paths.get(workingDir, "spotbugs-exclude.xml");
            if (!Files.exists(spotbugsPath)) {
                spotbugsPath = Paths.get(workingDir, "config/spotbugs/exclude.xml");
            }
            if (Files.exists(spotbugsPath)) {
                codeStyleInfo.put("spotbugs", true);
            }

            // Check for ESLint
            Path eslintPath = Paths.get(workingDir, ".eslintrc.js");
            if (!Files.exists(eslintPath)) {
                eslintPath = Paths.get(workingDir, ".eslintrc.json");
            }
            if (!Files.exists(eslintPath)) {
                eslintPath = Paths.get(workingDir, ".eslintrc");
            }
            if (Files.exists(eslintPath)) {
                codeStyleInfo.put("eslint", true);
            }

            // Check for Prettier
            Path prettierPath = Paths.get(workingDir, ".prettierrc");
            if (!Files.exists(prettierPath)) {
                prettierPath = Paths.get(workingDir, ".prettierrc.js");
            }
            if (!Files.exists(prettierPath)) {
                prettierPath = Paths.get(workingDir, ".prettierrc.json");
            }
            if (Files.exists(prettierPath)) {
                codeStyleInfo.put("prettier", true);
            }

        } catch (Exception e) {
            LOG.error("Error detecting code style: {}", e.getMessage());
        }

        return codeStyleInfo;
    }

    /**
     * Detects IDE settings used in the project.
     *
     * @return Map containing IDE settings information
     */
    @NotNull
    private Map<String, Object> detectIdeSettings() {
        Map<String, Object> ideSettingsInfo = new HashMap<>();

        try {
            // Check for IntelliJ IDEA
            Path ideaPath = Paths.get(workingDir, ".idea");
            if (Files.exists(ideaPath) && Files.isDirectory(ideaPath)) {
                ideSettingsInfo.put("intellij", true);

                // Check for code style settings
                Path codeStylePath = Paths.get(workingDir, ".idea/codeStyles");
                if (Files.exists(codeStylePath) && Files.isDirectory(codeStylePath)) {
                    ideSettingsInfo.put("intellijCodeStyle", true);
                }
            }

            // Check for Eclipse
            Path eclipsePath = Paths.get(workingDir, ".project");
            if (Files.exists(eclipsePath)) {
                ideSettingsInfo.put("eclipse", true);

                // Check for code style settings
                Path codeStylePath = Paths.get(workingDir, ".settings/org.eclipse.jdt.core.prefs");
                if (Files.exists(codeStylePath)) {
                    ideSettingsInfo.put("eclipseCodeStyle", true);
                }
            }

            // Check for VS Code
            Path vscodePath = Paths.get(workingDir, ".vscode");
            if (Files.exists(vscodePath) && Files.isDirectory(vscodePath)) {
                ideSettingsInfo.put("vscode", true);
            }

        } catch (Exception e) {
            LOG.error("Error detecting IDE settings: {}", e.getMessage());
        }

        return ideSettingsInfo;
    }

    /**
     * Detects programming languages used in the project.
     *
     * @return Map containing language information
     */
    @NotNull
    private Map<String, Object> detectLanguages() {
        Map<String, Object> languageInfo = new HashMap<>();

        try {
            // Count files by extension
            Map<String, Integer> extensionCounts = new HashMap<>();

            try (Stream<Path> walk = Files.walk(Paths.get(workingDir))) {
                List<Path> files = walk.filter(Files::isRegularFile).toList();

                for (Path file : files) {
                    String fileName = file.getFileName().toString();
                    int lastDotIndex = fileName.lastIndexOf('.');
                    if (lastDotIndex > 0) {
                        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
                        extensionCounts.put(extension, extensionCounts.getOrDefault(extension, 0) + 1);
                    }
                }
            }

            // Map extensions to languages
            Map<String, Integer> languageCounts = new HashMap<>();

            for (Map.Entry<String, Integer> entry : extensionCounts.entrySet()) {
                String extension = entry.getKey();
                Integer count = entry.getValue();

                String language = mapExtensionToLanguage(extension);
                if (language != null) {
                    languageCounts.put(language, languageCounts.getOrDefault(language, 0) + count);
                }
            }

            languageInfo.put("counts", languageCounts);

            // Determine primary language
            String primaryLanguage = null;
            int maxCount = 0;

            for (Map.Entry<String, Integer> entry : languageCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    primaryLanguage = entry.getKey();
                }
            }

            languageInfo.put("primary", primaryLanguage);

        } catch (Exception e) {
            LOG.error("Error detecting languages: {}", e.getMessage());
        }

        return languageInfo;
    }

    /**
     * Gets a basic overview of the project structure.
     *
     * @return String containing project structure
     */
    @NotNull
    private String getProjectStructure() {
        StringBuilder structure = new StringBuilder();

        try {
            // Get top-level directories
            try (Stream<Path> paths = Files.list(Paths.get(workingDir))) {
                paths.filter(Files::isDirectory)
                        .forEach(path ->
                                structure.append("- ")
                                        .append(path.getFileName())
                                        .append("\n"));
            }
        } catch (Exception e) {
            LOG.error("Error getting project structure: {}", e.getMessage());
        }

        return structure.toString();
    }

    // Helper methods

    /**
     * Extracts Maven commands from the project.
     *
     * @return Map containing Maven commands
     */
    @NotNull
    private Map<String, String> detectMavenCommands() {
        Map<String, String> commands = new HashMap<>();

        commands.put("build", "mvn clean install");
        commands.put("test", "mvn test");
        commands.put("single-test", "mvn test -Dtest=TestClass#testMethod");
        commands.put("run", "mvn exec:java -Dexec.mainClass=\"com.example.MainClass\"");

        // Look for specific plugins
        Path pomPath = Paths.get(workingDir, "pom.xml");
        try {
            String pomContent = Files.readString(pomPath);

            // Check for Spring Boot
            if (pomContent.contains("spring-boot-starter") ||
                    pomContent.contains("spring-boot-maven-plugin")) {
                commands.put("run", "mvn spring-boot:run");
            }

            // Check for Spotless
            if (pomContent.contains("spotless-maven-plugin")) {
                commands.put("format", "mvn spotless:apply");
                commands.put("check-format", "mvn spotless:check");
            }
        } catch (IOException e) {
            LOG.error("Error reading pom.xml: {}", e.getMessage());
        }

        return commands;
    }

    /**
     * Extracts Gradle commands from the project.
     *
     * @return Map containing Gradle commands
     */
    @NotNull
    private Map<String, String> detectGradleCommands() {
        Map<String, String> commands = new HashMap<>();

        commands.put("build", "./gradlew build");
        commands.put("test", "./gradlew test");
        commands.put("single-test", "./gradlew test --tests \"com.example.TestClass.testMethod\"");
        commands.put("run", "./gradlew run");

        // Look for specific plugins
        Path buildGradlePath = Paths.get(workingDir, "build.gradle");
        if (!Files.exists(buildGradlePath)) {
            buildGradlePath = Paths.get(workingDir, "build.gradle.kts");
        }

        if (Files.exists(buildGradlePath)) {
            try {
                String buildContent = Files.readString(buildGradlePath);

                // Check for Spring Boot
                if (buildContent.contains("org.springframework.boot") ||
                        buildContent.contains("spring-boot-gradle-plugin")) {
                    commands.put("run", "./gradlew bootRun");
                }

                // Check for Spotless
                if (buildContent.contains("spotless")) {
                    commands.put("format", "./gradlew spotlessApply");
                    commands.put("check-format", "./gradlew spotlessCheck");
                }
            } catch (IOException e) {
                LOG.error("Error reading build.gradle: {}", e.getMessage());
            }
        }

        return commands;
    }

    /**
     * Extracts npm commands from package.json.
     *
     * @param packageJsonPath Path to package.json
     * @return Map containing npm commands
     */
    @NotNull
    private Map<String, String> detectNpmCommands(Path packageJsonPath) {
        Map<String, String> commands = new HashMap<>();

        commands.put("install", "npm install");
        commands.put("build", "npm run build");
        commands.put("test", "npm test");
        commands.put("start", "npm start");

        // Read scripts from package.json
        try {
            String packageJsonContent = Files.readString(packageJsonPath);

            // Extract test and lint commands
            if (packageJsonContent.contains("\"test\":")) {
                commands.put("test", "npm test");
            }

            if (packageJsonContent.contains("\"lint\":")) {
                commands.put("lint", "npm run lint");
            }

            if (packageJsonContent.contains("\"format\":") || packageJsonContent.contains("\"prettier\":")) {
                commands.put("format", "npm run format");
            }

        } catch (IOException e) {
            LOG.error("Error reading package.json: {}", e.getMessage());
        }

        return commands;
    }

    /**
     * Maps a file extension to a programming language.
     *
     * @param extension File extension
     * @return Language name or null if unknown
     */
    private String mapExtensionToLanguage(String extension) {
        switch (extension.toLowerCase()) {
            case "java":
                return "Java";
            case "kt":
            case "kts":
                return "Kotlin";
            case "groovy":
                return "Groovy";
            case "js":
                return "JavaScript";
            case "ts":
                return "TypeScript";
            case "jsx":
                return "JavaScript/React";
            case "tsx":
                return "TypeScript/React";
            case "py":
                return "Python";
            case "rb":
                return "Ruby";
            case "php":
                return "PHP";
            case "go":
                return "Go";
            case "rs":
                return "Rust";
            case "c":
                return "C";
            case "cpp":
            case "cc":
            case "cxx":
                return "C++";
            case "cs":
                return "C#";
            case "swift":
                return "Swift";
            case "html":
            case "htm":
                return "HTML";
            case "css":
                return "CSS";
            case "scss":
            case "sass":
                return "SASS/SCSS";
            case "xml":
                return "XML";
            case "json":
                return "JSON";
            case "yml":
            case "yaml":
                return "YAML";
            case "md":
            case "markdown":
                return "Markdown";
            default:
                return null;
        }
    }

    /**
     * Extracts a version number from a command output.
     *
     * @param output Command output
     * @param prefix Version prefix
     * @return Extracted version or null
     */
    private String extractVersion(String output, String prefix) {
        if (output == null || prefix == null) {
            return null;
        }

        // Find the line containing the prefix
        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            if (line.contains(prefix)) {
                // Extract the version number
                int prefixIndex = line.indexOf(prefix);
                if (prefixIndex >= 0) {
                    String remainder = line.substring(prefixIndex + prefix.length()).trim();
                    // Try to extract version format x.y.z
                    String versionPattern = "\\d+(\\.\\d+)+";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(versionPattern);
                    java.util.regex.Matcher matcher = pattern.matcher(remainder);
                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if a file contains specific text.
     *
     * @param path File path
     * @param text Text to search for
     * @return True if the file contains the text
     */
    private boolean fileContainsText(Path path, String text) {
        if (!Files.exists(path)) {
            return false;
        }

        try {
            String content = Files.readString(path);
            return content.contains(text);
        } catch (IOException e) {
            LOG.error("Error reading file: {}", e.getMessage());
            return false;
        }
    }
}