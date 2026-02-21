package com.devoxx.genie.service.analyzer.languages.javascript;

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
 * Extension to enhance project scanning with JavaScript/TypeScript-specific details
 */
public class JavaScriptProjectScannerExtension implements ProjectAnalyzerExtension {
    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if JavaScript or TypeScript is detected as a language
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null || 
            (!languages.toString().contains("JavaScript") && 
             !languages.toString().contains("TypeScript"))) {
            return;
        }

        // Get project base directory
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            return;
        }

        // Process JavaScript/TypeScript-specific information
        try {
            Map<String, Object> jsInfo = new HashMap<>();
            
            // Check for package.json
            VirtualFile packageJson = baseDir.findChild("package.json");
            if (packageJson != null) {
                extractPackageJsonInfo(packageJson, jsInfo);
            }
            
            // Check for TypeScript configuration
            VirtualFile tsConfig = baseDir.findChild("tsconfig.json");
            if (tsConfig != null) {
                jsInfo.put("language", "TypeScript");
                extractTsConfigInfo(tsConfig, jsInfo);
            } else {
                jsInfo.put("language", "JavaScript");
            }
            
            // Check for common frameworks
            detectJsFrameworks(baseDir, packageJson, jsInfo);
            
            // Check for linting and formatting tools
            detectCodeQualityTools(baseDir, jsInfo);
            
            // Check for testing frameworks
            detectTestingFrameworks(baseDir, packageJson, jsInfo);
            
            // Check for bundlers
            detectBundlers(baseDir, packageJson, jsInfo);
            
            // Add JavaScript information to project info
            projectInfo.put("javascript", jsInfo);
            
            // Add JS specific build commands to build system
            enhanceBuildSystem(projectInfo, jsInfo);
            
        } catch (Exception e) {
            // Handle exception gracefully
        }
    }
    
    private void extractPackageJsonInfo(VirtualFile packageJson, Map<String, Object> jsInfo) {
        try {
            String content = VfsUtil.loadText(packageJson);
            
            // Extract project name
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(content);
            if (nameMatcher.find()) {
                jsInfo.put("name", nameMatcher.group(1));
            }
            
            // Extract version
            Pattern versionPattern = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                jsInfo.put("version", versionMatcher.group(1));
            }
            
            // Check for type (module/commonjs)
            Pattern typePattern = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
            Matcher typeMatcher = typePattern.matcher(content);
            if (typeMatcher.find()) {
                jsInfo.put("moduleType", typeMatcher.group(1));
            }
            
            // Check for package manager
            boolean hasYarn = new java.io.File(packageJson.getParent().getPath(), "yarn.lock").exists();
            boolean hasPnpm = new java.io.File(packageJson.getParent().getPath(), "pnpm-lock.yaml").exists();
            
            if (hasYarn) {
                jsInfo.put("packageManager", "yarn");
            } else if (hasPnpm) {
                jsInfo.put("packageManager", "pnpm");
            } else {
                jsInfo.put("packageManager", "npm");
            }
            
        } catch (IOException ignored) {}
    }
    
    private void extractTsConfigInfo(VirtualFile tsConfig, Map<String, Object> jsInfo) {
        try {
            String content = VfsUtil.loadText(tsConfig);
            
            // Extract target
            Pattern targetPattern = Pattern.compile("\"target\"\\s*:\\s*\"([^\"]+)\"");
            Matcher targetMatcher = targetPattern.matcher(content);
            if (targetMatcher.find()) {
                jsInfo.put("jsTarget", targetMatcher.group(1));
            }
            
            // Check for strict mode
            if (content.contains("\"strict\": true")) {
                jsInfo.put("strictMode", true);
            }
            
            // Check for JSX support
            if (content.contains("\"jsx\":") || content.contains("\"react\"")) {
                jsInfo.put("jsxSupport", true);
            }
            
        } catch (IOException ignored) {}
    }
    
    private void detectJsFrameworks(VirtualFile baseDir, VirtualFile packageJson, Map<String, Object> jsInfo) {
        if (packageJson == null) return;
        
        try {
            String content = VfsUtil.loadText(packageJson);
            
            // Check for React
            if (content.contains("\"react\"") || content.contains("\"react-dom\"")) {
                jsInfo.put("framework", "React");
                
                // Check for Next.js
                if (content.contains("\"next\"")) {
                    jsInfo.put("framework", "Next.js");
                }
            }
            // Check for Vue
            else if (content.contains("\"vue\"")) {
                jsInfo.put("framework", "Vue");
                
                // Check for Nuxt
                if (content.contains("\"nuxt\"")) {
                    jsInfo.put("framework", "Nuxt.js");
                }
            }
            // Check for Angular
            else if (content.contains("\"@angular/core\"")) {
                jsInfo.put("framework", "Angular");
            }
            // Check for Svelte
            else if (content.contains("\"svelte\"")) {
                jsInfo.put("framework", "Svelte");
                
                // Check for SvelteKit
                if (content.contains("\"@sveltejs/kit\"")) {
                    jsInfo.put("framework", "SvelteKit");
                }
            }
            // Check for Express.js (backend)
            else if (content.contains("\"express\"")) {
                jsInfo.put("framework", "Express.js");
            }
            // Check for NestJS (backend)
            else if (content.contains("\"@nestjs/core\"")) {
                jsInfo.put("framework", "NestJS");
            }
            
        } catch (IOException ignored) {}
    }
    
    private void detectCodeQualityTools(VirtualFile baseDir, Map<String, Object> jsInfo) {
        // Check for ESLint
        boolean hasEslint = baseDir.findChild(".eslintrc.js") != null ||
                            baseDir.findChild(".eslintrc.json") != null ||
                            baseDir.findChild(".eslintrc.yml") != null ||
                            baseDir.findChild(".eslintrc") != null;
        if (hasEslint) {
            jsInfo.put("linter", "ESLint");
        }
        
        // Check for Prettier
        boolean hasPrettier = baseDir.findChild(".prettierrc") != null ||
                              baseDir.findChild(".prettierrc.js") != null ||
                              baseDir.findChild(".prettierrc.json") != null ||
                              baseDir.findChild(".prettierrc.yml") != null ||
                              baseDir.findChild("prettier.config.js") != null;
        if (hasPrettier) {
            jsInfo.put("formatter", "Prettier");
        }
        
        // Check for StyleLint
        boolean hasStylelint = baseDir.findChild(".stylelintrc") != null ||
                              baseDir.findChild(".stylelintrc.js") != null ||
                              baseDir.findChild(".stylelintrc.json") != null ||
                              baseDir.findChild("stylelint.config.js") != null;
        if (hasStylelint) {
            jsInfo.put("cssLinter", "StyleLint");
        }
    }
    
    private void detectTestingFrameworks(VirtualFile baseDir, VirtualFile packageJson, Map<String, Object> jsInfo) {
        if (packageJson == null) return;
        
        try {
            String content = VfsUtil.loadText(packageJson);
            
            // Check for Jest
            if (content.contains("\"jest\"")) {
                jsInfo.put("testFramework", "Jest");
            }
            // Check for Mocha
            else if (content.contains("\"mocha\"")) {
                jsInfo.put("testFramework", "Mocha");
            }
            // Check for Jasmine
            else if (content.contains("\"jasmine\"")) {
                jsInfo.put("testFramework", "Jasmine");
            }
            // Check for Cypress
            else if (content.contains("\"cypress\"")) {
                jsInfo.put("e2eTestFramework", "Cypress");
            }
            // Check for Playwright
            else if (content.contains("\"@playwright/test\"")) {
                jsInfo.put("e2eTestFramework", "Playwright");
            }
            // Check for Testing Library
            if (content.contains("\"@testing-library/react\"") ||
                content.contains("\"@testing-library/vue\"") ||
                content.contains("\"@testing-library/angular\"") ||
                content.contains("\"@testing-library/svelte\"")) {
                jsInfo.put("testingLibrary", "Testing Library");
            }
            
        } catch (IOException ignored) {}
    }
    
    private void detectBundlers(VirtualFile baseDir, VirtualFile packageJson, Map<String, Object> jsInfo) {
        if (packageJson == null) return;
        
        try {
            String content = VfsUtil.loadText(packageJson);
            
            // Check for Webpack
            if (content.contains("\"webpack\"")) {
                jsInfo.put("bundler", "Webpack");
            }
            // Check for Vite
            else if (content.contains("\"vite\"")) {
                jsInfo.put("bundler", "Vite");
            }
            // Check for Rollup
            else if (content.contains("\"rollup\"")) {
                jsInfo.put("bundler", "Rollup");
            }
            // Check for Parcel
            else if (content.contains("\"parcel\"")) {
                jsInfo.put("bundler", "Parcel");
            }
            // Check for esbuild
            else if (content.contains("\"esbuild\"")) {
                jsInfo.put("bundler", "esbuild");
            }
            
        } catch (IOException ignored) {}
    }
    
    private void enhanceBuildSystem(Map<String, Object> projectInfo, Map<String, Object> jsInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem == null) {
            buildSystem = new HashMap<>();
            projectInfo.put("buildSystem", buildSystem);
        }

        Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
        if (commands == null) {
            commands = new HashMap<>();
            buildSystem.put("commands", commands);
        }

        String packageManager = (String) jsInfo.getOrDefault("packageManager", "npm");
        String runCmd = getRunCommand(packageManager);

        commands.put("install", getInstallCommand(packageManager));
        commands.put("start", runCmd + " start");
        commands.put("build", runCmd + " build");

        addTestCommands(commands, jsInfo, runCmd);
        addE2eTestCommands(commands, jsInfo, runCmd);
        addLintFormatCommands(commands, jsInfo, runCmd);
        addFrameworkCommands(commands, jsInfo, runCmd, packageManager);
    }

    private String getRunCommand(String packageManager) {
        if ("yarn".equals(packageManager)) return "yarn";
        if ("pnpm".equals(packageManager)) return "pnpm";
        return "npm run";
    }

    private String getInstallCommand(String packageManager) {
        if ("yarn".equals(packageManager)) return "yarn";
        if ("pnpm".equals(packageManager)) return "pnpm install";
        return "npm install";
    }

    private void addTestCommands(Map<String, String> commands, Map<String, Object> jsInfo, String runCmd) {
        if (!jsInfo.containsKey("testFramework")) return;

        String testFramework = (String) jsInfo.get("testFramework");
        commands.put("test", runCmd + " test");

        if ("Jest".equals(testFramework)) {
            commands.put("testWatch", runCmd + " test -- --watch");
            commands.put("singleTest", runCmd + " test -- -t \"Test Name\"");
        }
    }

    private void addE2eTestCommands(Map<String, String> commands, Map<String, Object> jsInfo, String runCmd) {
        if (!jsInfo.containsKey("e2eTestFramework")) return;

        String e2eFramework = (String) jsInfo.get("e2eTestFramework");
        if ("Cypress".equals(e2eFramework)) {
            commands.put("e2e", runCmd + " cypress:open");
            commands.put("e2eHeadless", runCmd + " cypress:run");
        } else if ("Playwright".equals(e2eFramework)) {
            commands.put("e2e", runCmd + " playwright test");
            commands.put("e2eUI", runCmd + " playwright test --ui");
        }
    }

    private void addLintFormatCommands(Map<String, String> commands, Map<String, Object> jsInfo, String runCmd) {
        if (jsInfo.containsKey("linter") && "ESLint".equals(jsInfo.get("linter"))) {
            commands.put("lint", runCmd + " lint");
            commands.put("lintFix", runCmd + " lint -- --fix");
        }

        if (jsInfo.containsKey("formatter") && "Prettier".equals(jsInfo.get("formatter"))) {
            commands.put("format", runCmd + " format");
        }
    }

    private void addFrameworkCommands(Map<String, String> commands, Map<String, Object> jsInfo,
                                      String runCmd, String packageManager) {
        if (!jsInfo.containsKey("framework")) return;

        String framework = (String) jsInfo.get("framework");
        if ("Next.js".equals(framework)) {
            commands.put("dev", runCmd + " dev");
            commands.put("start", runCmd + " start");
            commands.put("export", runCmd + " export");
        } else if ("Nuxt.js".equals(framework)) {
            commands.put("dev", runCmd + " dev");
            commands.put("generate", runCmd + " generate");
        } else if ("Angular".equals(framework)) {
            commands.put("serve", runCmd + " serve");
            commands.put("generate", "yarn".equals(packageManager) ? "yarn ng generate" : "npx ng generate");
        }
    }
}
