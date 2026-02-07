package com.devoxx.genie.service.analyzer.languages.kotlin;

import com.devoxx.genie.service.analyzer.ProjectAnalyzerExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension to enhance project scanning with Kotlin-specific details
 */
public class KotlinProjectScannerExtension implements ProjectAnalyzerExtension {
    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if Kotlin is detected as a language
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null || !languages.toString().contains("Kotlin")) {
            return;
        }

        // Get project base directory
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            return;
        }

        // Process Kotlin-specific information
        try {
            Map<String, Object> kotlinInfo = new HashMap<>();
            
            // Check for Gradle/Maven
            VirtualFile buildGradleKts = baseDir.findChild("build.gradle.kts");
            VirtualFile buildGradle = baseDir.findChild("build.gradle");
            VirtualFile pomXml = baseDir.findChild("pom.xml");
            
            if (buildGradleKts != null) {
                kotlinInfo.put("buildSystem", "Gradle (Kotlin DSL)");
                extractGradleKtsInfo(buildGradleKts, kotlinInfo);
            } else if (buildGradle != null) {
                kotlinInfo.put("buildSystem", "Gradle (Groovy DSL)");
                extractGradleInfo(buildGradle, kotlinInfo);
            } else if (pomXml != null) {
                kotlinInfo.put("buildSystem", "Maven");
                extractMavenInfo(pomXml, kotlinInfo);
            }
            
            // Check for Kotlin frameworks
            detectKotlinFrameworks(baseDir, kotlinInfo);
            
            // Check for Kotlin Multiplatform
            boolean isKmp = findInFile(baseDir, "build.gradle.kts", "kotlin(\"multiplatform\")") ||
                           findInFile(baseDir, "build.gradle", "kotlin(\"multiplatform\")") ||
                           findInFile(baseDir, "build.gradle.kts", "multiplatform") ||
                           findInFile(baseDir, "build.gradle", "multiplatform");
                           
            if (isKmp) {
                kotlinInfo.put("isMultiplatform", true);
            }
            
            // Check for Kotlin/Native
            boolean isNative = findInFile(baseDir, "build.gradle.kts", "kotlin(\"native\")") ||
                              findInFile(baseDir, "build.gradle", "kotlin(\"native\")");
                              
            if (isNative) {
                kotlinInfo.put("isNative", true);
            }
            
            // Check for Kotlin/JS
            boolean isJs = findInFile(baseDir, "build.gradle.kts", "kotlin(\"js\")") ||
                          findInFile(baseDir, "build.gradle", "kotlin(\"js\")");
                          
            if (isJs) {
                kotlinInfo.put("isJs", true);
            }
            
            // Check for Kotlin style configuration
            detectKotlinCodeStyle(baseDir, kotlinInfo);
            
            // Check for testing frameworks
            detectTestingFrameworks(baseDir, kotlinInfo);
            
            // Add Kotlin information to project info
            projectInfo.put("kotlin", kotlinInfo);
            
            // Add Kotlin specific build commands to build system
            enhanceBuildSystem(projectInfo, kotlinInfo);
            
        } catch (Exception e) {
            // Handle exception gracefully
        }
    }
    
    private void extractGradleKtsInfo(VirtualFile buildGradleKts, Map<String, Object> kotlinInfo) {
        try {
            String content = VfsUtil.loadText(buildGradleKts);
            
            // Extract Kotlin version
            Pattern versionPattern = Pattern.compile("kotlin[\"']?\\s*version\\s*[\"']\\s*([^\"']+)[\"']");
            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                kotlinInfo.put("kotlinVersion", versionMatcher.group(1));
            }
            
            // Check if it's Android project
            boolean isAndroid = content.contains("kotlin(\"android\")") || 
                               content.contains("id(\"com.android.application\")") || 
                               content.contains("id(\"com.android.library\")") ||
                               content.contains("apply plugin: 'com.android");
            
            if (isAndroid) {
                kotlinInfo.put("isAndroid", true);
            }
            
            // Check for common plugins
            if (content.contains("kotlin(\"plugin.spring\")") || content.contains("org.springframework.boot")) {
                kotlinInfo.put("framework", "Spring Boot");
            } else if (content.contains("kotlin(\"plugin.jpa\")")) {
                kotlinInfo.put("hasJpa", true);
            } else if (content.contains("ktor")) {
                kotlinInfo.put("framework", "Ktor");
            } else if (content.contains("ktorx")) {
                kotlinInfo.put("framework", "KtorX");
            } else if (content.contains("compose")) {
                kotlinInfo.put("usesCompose", true);
            }
            
        } catch (IOException ignored) {}
    }
    
    private void extractGradleInfo(VirtualFile buildGradle, Map<String, Object> kotlinInfo) {
        try {
            String content = VfsUtil.loadText(buildGradle);
            
            // Extract Kotlin version from Groovy DSL
            Pattern versionPattern = Pattern.compile("kotlin[\"']?\\s*version\\s*[\"']\\s*([^\"']+)[\"']");
            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                kotlinInfo.put("kotlinVersion", versionMatcher.group(1));
            }
            
            // Same checks as for Gradle KTS, but with Groovy syntax
            boolean isAndroid = content.contains("kotlin('android')") || 
                               content.contains("id 'com.android.application'") || 
                               content.contains("id 'com.android.library'") ||
                               content.contains("apply plugin: 'com.android");
            
            if (isAndroid) {
                kotlinInfo.put("isAndroid", true);
            }
            
            // Check for common plugins in Groovy DSL
            if (content.contains("kotlin('plugin.spring')") || content.contains("org.springframework.boot")) {
                kotlinInfo.put("framework", "Spring Boot");
            } else if (content.contains("kotlin('plugin.jpa')")) {
                kotlinInfo.put("hasJpa", true);
            } else if (content.contains("ktor")) {
                kotlinInfo.put("framework", "Ktor");
            } else if (content.contains("ktorx")) {
                kotlinInfo.put("framework", "KtorX");
            } else if (content.contains("compose")) {
                kotlinInfo.put("usesCompose", true);
            }
            
        } catch (IOException ignored) {}
    }
    
    private void extractMavenInfo(VirtualFile pomXml, Map<String, Object> kotlinInfo) {
        try {
            String content = VfsUtil.loadText(pomXml);
            
            // Extract Kotlin version from Maven POM
            Pattern versionPattern = Pattern.compile("<kotlin.version>([^<]+)</kotlin.version>");
            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                kotlinInfo.put("kotlinVersion", versionMatcher.group(1));
            }
            
            // Check for Spring Boot
            if (content.contains("spring-boot") || content.contains("org.springframework.boot")) {
                kotlinInfo.put("framework", "Spring Boot");
            }
            
            // Check for Ktor
            if (content.contains("io.ktor")) {
                kotlinInfo.put("framework", "Ktor");
            }
            
            // Check for JPA
            if (content.contains("javax.persistence") || content.contains("jakarta.persistence")) {
                kotlinInfo.put("hasJpa", true);
            }
            
        } catch (IOException ignored) {}
    }
    
    private void detectKotlinFrameworks(VirtualFile baseDir, @NotNull Map<String, Object> kotlinInfo) {
        // If already detected framework from build file, skip
        if (kotlinInfo.containsKey("framework")) {
            return;
        }
        
        // Check for Kotlin frameworks based on directory structure
        boolean hasKtorRoutes = directoryExists(baseDir, "src/main/kotlin/routes") || 
                               fileExists(baseDir, "src/main/kotlin", "Application.kt");
        boolean hasSpringConfig = directoryExists(baseDir, "src/main/kotlin/config") || 
                                 fileExists(baseDir, "src/main/kotlin", "Application.kt") ||
                                 fileExists(baseDir, "src/main/kotlin", "*.kt", "SpringBootApplication");
        boolean hasComposeUI = directoryExists(baseDir, "src/main/kotlin/ui") ||
                              directoryExists(baseDir, "src/main/kotlin/composables");
        
        if (hasKtorRoutes) {
            kotlinInfo.put("framework", "Ktor");
        } else if (hasSpringConfig) {
            kotlinInfo.put("framework", "Spring Boot");
        } else if (hasComposeUI) {
            kotlinInfo.put("usesCompose", true);
        }
    }
    
    private void detectKotlinCodeStyle(@NotNull VirtualFile baseDir, Map<String, Object> kotlinInfo) {
        // Check for ktlint
        boolean hasKtlint = baseDir.findChild(".editorconfig") != null &&
                           findInFile(baseDir, ".editorconfig", "ktlint");
        if (hasKtlint) {
            kotlinInfo.put("linter", "ktlint");
        }
        
        // Check for Detekt
        boolean hasDetekt = baseDir.findChild("detekt.yml") != null ||
                           baseDir.findChild("config/detekt/detekt.yml") != null;
        if (hasDetekt) {
            kotlinInfo.put("staticAnalysis", "Detekt");
        }
    }
    
    private void detectTestingFrameworks(VirtualFile baseDir, Map<String, Object> kotlinInfo) {
        // Check for common Kotlin testing frameworks
        boolean hasKotest = findInFile(baseDir, "build.gradle.kts", "kotest") ||
                          findInFile(baseDir, "build.gradle", "kotest") ||
                          findInFile(baseDir, "pom.xml", "kotest");
                          
        boolean hasMockk = findInFile(baseDir, "build.gradle.kts", "mockk") ||
                         findInFile(baseDir, "build.gradle", "mockk") ||
                         findInFile(baseDir, "pom.xml", "mockk");
        
        if (hasKotest) {
            kotlinInfo.put("testFramework", "Kotest");
        } else {
            kotlinInfo.put("testFramework", "JUnit"); // Default is JUnit for Kotlin
        }
        
        if (hasMockk) {
            kotlinInfo.put("mockingFramework", "MockK");
        }
    }
    
    private boolean directoryExists(VirtualFile parent, @NotNull String path) {
        String[] parts = path.split("/");
        VirtualFile current = parent;
        
        for (String part : parts) {
            current = current.findChild(part);
            if (current == null || !current.isDirectory()) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean fileExists(VirtualFile parent, @NotNull String directoryPath, String filename) {
        String[] parts = directoryPath.split("/");
        VirtualFile current = parent;
        
        for (String part : parts) {
            current = current.findChild(part);
            if (current == null || !current.isDirectory()) {
                return false;
            }
        }
        
        return current.findChild(filename) != null;
    }
    
    private boolean fileExists(VirtualFile parent, @NotNull String directoryPath, String filePattern, String contentPattern) {
        String[] parts = directoryPath.split("/");
        VirtualFile current = parent;
        
        for (String part : parts) {
            current = current.findChild(part);
            if (current == null || !current.isDirectory()) {
                return false;
            }
        }
        
        // Convert file pattern to regex
        Pattern pattern = createGlobPattern(filePattern);
        
        // Check if any file in the directory matches both the name pattern and contains the content
        for (VirtualFile file : current.getChildren()) {
            if (!file.isDirectory() && pattern.matcher(file.getName()).matches()) {
                try {
                    String content = VfsUtil.loadText(file);
                    if (content.contains(contentPattern)) {
                        return true;
                    }
                } catch (IOException ignored) {}
            }
        }
        
        return false;
    }
    
    private @NotNull Pattern createGlobPattern(@NotNull String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    regex.append("\\");
                    regex.append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }
    
    private boolean findInFile(@NotNull VirtualFile baseDir, String fileName, String content) {
        VirtualFile file = baseDir.findChild(fileName);
        if (file != null && !file.isDirectory()) {
            try {
                String text = VfsUtil.loadText(file);
                return text.contains(content);
            } catch (IOException ignored) {}
        }
        return false;
    }
    
    private void enhanceBuildSystem(@NotNull Map<String, Object> projectInfo, Map<String, Object> kotlinInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem == null) {
            buildSystem = new HashMap<>();
            projectInfo.put("buildSystem", buildSystem);
        }
        
        // Update or add Kotlin-specific commands
        Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
        if (commands == null) {
            commands = new HashMap<>();
            buildSystem.put("commands", commands);
        }
        
        // Determine build system type
        String buildSystemType = (String) kotlinInfo.getOrDefault("buildSystem", "Gradle (Kotlin DSL)");
        boolean isGradleKts = buildSystemType.contains("Kotlin DSL");
        boolean isGradle = buildSystemType.contains("Gradle");
        boolean isMaven = buildSystemType.contains("Maven");
        
        String buildCommand;
        String testCommand;
        String runCommand;
        String cleanCommand;
        String singleTestCommand;
        
        if (isGradle) {
            buildCommand = "./gradlew build";
            testCommand = "./gradlew test";
            runCommand = "./gradlew run";
            cleanCommand = "./gradlew clean";
            singleTestCommand = "./gradlew test --tests \"*TestName*\"";
        } else if (isMaven) {
            buildCommand = "mvn clean package";
            testCommand = "mvn test";
            runCommand = "mvn exec:java";
            cleanCommand = "mvn clean";
            singleTestCommand = "mvn test -Dtest=TestName";
        } else {
            // Default fallback
            buildCommand = "kotlin build";
            testCommand = "kotlin test";
            runCommand = "kotlin run";
            cleanCommand = "kotlin clean";
            singleTestCommand = "kotlin test TestName";
        }
        
        // Add common commands
        commands.put("build", buildCommand);
        commands.put("test", testCommand);
        commands.put("run", runCommand);
        commands.put("clean", cleanCommand);
        commands.put("singleTest", singleTestCommand);
        
        // Add framework-specific commands
        if (kotlinInfo.containsKey("framework")) {
            String framework = (String) kotlinInfo.get("framework");
            
            if ("Spring Boot".equals(framework)) {
                if (isGradle) {
                    commands.put("bootRun", "./gradlew bootRun");
                } else if (isMaven) {
                    commands.put("bootRun", "mvn spring-boot:run");
                }
            } else if ("Ktor".equals(framework)) {
                // Commands are mostly the same for Ktor
            }
        }
        
        // Add Android-specific commands
        if (kotlinInfo.containsKey("isAndroid") && (Boolean) kotlinInfo.get("isAndroid")) {
            commands.put("assembleDebug", "./gradlew assembleDebug");
            commands.put("assembleRelease", "./gradlew assembleRelease");
            commands.put("installDebug", "./gradlew installDebug");
            commands.put("connectedAndroidTest", "./gradlew connectedAndroidTest");
        }
        
        // Add Multiplatform-specific commands
        if (kotlinInfo.containsKey("isMultiplatform") && (Boolean) kotlinInfo.get("isMultiplatform")) {
            commands.put("jsBrowserRun", "./gradlew jsBrowserRun");
            commands.put("iosX64Test", "./gradlew iosX64Test");
            commands.put("allTests", "./gradlew allTests");
        }
        
        // Add code quality commands
        if (kotlinInfo.containsKey("linter") && "ktlint".equals(kotlinInfo.get("linter"))) {
            if (isGradle) {
                commands.put("lint", "./gradlew ktlintCheck");
                commands.put("format", "./gradlew ktlintFormat");
            } else {
                commands.put("lint", "ktlint");
                commands.put("format", "ktlint -F");
            }
        }
        
        if (kotlinInfo.containsKey("staticAnalysis") && "Detekt".equals(kotlinInfo.get("staticAnalysis"))) {
            if (isGradle) {
                commands.put("detekt", "./gradlew detekt");
            } else {
                commands.put("detekt", "detekt");
            }
        }
    }
}
