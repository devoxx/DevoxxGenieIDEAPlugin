package com.devoxx.genie.service.analyzer.languages.php;

import com.devoxx.genie.service.analyzer.ProjectAnalyzerExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension to enhance project scanning with PHP-specific details
 */
public class PhpProjectScannerExtension implements ProjectAnalyzerExtension {
    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if PHP is detected as a language
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null || !languages.toString().contains("PHP")) {
            return;
        }

        // Get project base directory
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return;
        }

        // Process PHP-specific information
        try {
            Map<String, Object> phpInfo = new HashMap<>();
            
            // Check for Composer
            VirtualFile composerJson = baseDir.findChild("composer.json");
            if (composerJson != null) {
                phpInfo.put("dependencyManager", "Composer");
                extractComposerInfo(composerJson, phpInfo);
            }
            
            // Check for common PHP frameworks
            if (isLaravelProject(baseDir)) {
                phpInfo.put("framework", "Laravel");
            } else if (isSymfonyProject(baseDir)) {
                phpInfo.put("framework", "Symfony");
            } else if (isCodeIgniterProject(baseDir)) {
                phpInfo.put("framework", "CodeIgniter");
            } else if (isYiiProject(baseDir)) {
                phpInfo.put("framework", "Yii");
            } else if (isWordPressProject(baseDir)) {
                phpInfo.put("framework", "WordPress");
            }
            
            // Check for PHPUnit
            boolean hasPhpUnit = baseDir.findChild("phpunit.xml") != null 
                || baseDir.findChild("phpunit.xml.dist") != null;
            if (hasPhpUnit) {
                phpInfo.put("testingFramework", "PHPUnit");
            }
            
            // Check for code quality tools
            if (baseDir.findChild("phpcs.xml") != null || baseDir.findChild("phpcs.xml.dist") != null) {
                phpInfo.put("codingStandard", "PHP_CodeSniffer");
            }
            
            if (baseDir.findChild("phpmd.xml") != null || baseDir.findChild("phpmd.xml.dist") != null) {
                phpInfo.put("messDetector", "PHP Mess Detector");
            }
            
            if (baseDir.findChild(".php_cs") != null || baseDir.findChild(".php_cs.dist") != null 
                || baseDir.findChild(".php-cs-fixer.php") != null 
                || baseDir.findChild(".php-cs-fixer.dist.php") != null) {
                phpInfo.put("formatter", "PHP-CS-Fixer");
            }
            
            // Add PHP information to project info
            projectInfo.put("php", phpInfo);
            
            // Add PHP specific build commands to build system
            enhanceBuildSystem(projectInfo, phpInfo);
            
        } catch (Exception e) {
            // Handle exception gracefully
        }
    }
    
    private void extractComposerInfo(VirtualFile composerJson, Map<String, Object> phpInfo) {
        try {
            String content = VfsUtil.loadText(composerJson);
            
            // Extract project name
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(content);
            if (nameMatcher.find()) {
                phpInfo.put("name", nameMatcher.group(1));
            }
            
            // Extract PHP version requirement
            Pattern phpVersionPattern = Pattern.compile("\"php\"\\s*:\\s*\"([^\"]+)\"");
            Matcher phpVersionMatcher = phpVersionPattern.matcher(content);
            if (phpVersionMatcher.find()) {
                phpInfo.put("phpVersion", phpVersionMatcher.group(1));
            }
            
            // Check for autoloading
            if (content.contains("\"autoload\"") || content.contains("\"autoload-dev\"")) {
                phpInfo.put("hasAutoloading", true);
            }
            
            // Check for scripts
            if (content.contains("\"scripts\"")) {
                phpInfo.put("hasComposerScripts", true);
            }
            
        } catch (IOException ignored) {}
    }
    
    private boolean isLaravelProject(VirtualFile baseDir) {
        return baseDir.findChild("artisan") != null || 
               directoryExists(baseDir, "app/Http/Controllers");
    }
    
    private boolean isSymfonyProject(VirtualFile baseDir) {
        return directoryExists(baseDir, "src/Controller") || 
               directoryExists(baseDir, "src/Bundle") ||
               directoryExists(baseDir, "var/cache");
    }
    
    private boolean isCodeIgniterProject(VirtualFile baseDir) {
        return directoryExists(baseDir, "application/controllers") ||
               directoryExists(baseDir, "app/Controllers");
    }
    
    private boolean isYiiProject(VirtualFile baseDir) {
        return directoryExists(baseDir, "protected/controllers") ||
               directoryExists(baseDir, "protected/views");
    }
    
    private boolean isWordPressProject(VirtualFile baseDir) {
        return baseDir.findChild("wp-config.php") != null ||
               directoryExists(baseDir, "wp-content/themes");
    }
    
    private boolean directoryExists(VirtualFile parent, String path) {
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
    
    private void enhanceBuildSystem(Map<String, Object> projectInfo, Map<String, Object> phpInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem == null) {
            buildSystem = new HashMap<>();
            projectInfo.put("buildSystem", buildSystem);
        }
        
        // Update or add PHP-specific commands
        Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
        if (commands == null) {
            commands = new HashMap<>();
            buildSystem.put("commands", commands);
        }
        
        // Add Composer commands if detected
        if (phpInfo.containsKey("dependencyManager") && "Composer".equals(phpInfo.get("dependencyManager"))) {
            commands.put("install", "composer install");
            commands.put("update", "composer update");
            
            if (phpInfo.containsKey("hasComposerScripts") && (Boolean)phpInfo.get("hasComposerScripts")) {
                commands.put("scripts", "composer run-script");
            }
        }
        
        // Add testing commands if PHPUnit is detected
        if (phpInfo.containsKey("testingFramework") && "PHPUnit".equals(phpInfo.get("testingFramework"))) {
            commands.put("test", "vendor/bin/phpunit");
            commands.put("singleTest", "vendor/bin/phpunit --filter=TestName");
        }
        
        // Add framework-specific commands
        if (phpInfo.containsKey("framework")) {
            String framework = (String) phpInfo.get("framework");
            
            if ("Laravel".equals(framework)) {
                commands.put("serve", "php artisan serve");
                commands.put("migrate", "php artisan migrate");
                commands.put("tinker", "php artisan tinker");
            } else if ("Symfony".equals(framework)) {
                commands.put("serve", "symfony server:start");
                commands.put("console", "php bin/console");
            }
        }
        
        // Add code quality commands
        if (phpInfo.containsKey("codingStandard") && "PHP_CodeSniffer".equals(phpInfo.get("codingStandard"))) {
            commands.put("lint", "vendor/bin/phpcs");
        }
        
        if (phpInfo.containsKey("formatter") && "PHP-CS-Fixer".equals(phpInfo.get("formatter"))) {
            commands.put("format", "vendor/bin/php-cs-fixer fix");
        }
        
        if (phpInfo.containsKey("messDetector") && "PHP Mess Detector".equals(phpInfo.get("messDetector"))) {
            commands.put("messDetector", "vendor/bin/phpmd src text phpmd.xml");
        }
    }
}
