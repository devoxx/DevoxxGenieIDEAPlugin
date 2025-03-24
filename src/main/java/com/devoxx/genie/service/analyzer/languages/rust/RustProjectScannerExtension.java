package com.devoxx.genie.service.analyzer.languages.rust;

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
 * Extension to enhance project scanning with Rust-specific details
 */
public class RustProjectScannerExtension implements ProjectAnalyzerExtension {

    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if Rust is detected as a language
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null || !languages.toString().contains("Rust")) {
            return;
        }

        // Get project base directory
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return;
        }

        // Check for Cargo.toml
        VirtualFile cargoToml = baseDir.findChild("Cargo.toml");
        if (cargoToml == null) {
            return;
        }

        // Process Rust-specific information
        try {
            Map<String, Object> rustInfo = new HashMap<>();
            
            // Extract data from Cargo.toml
            String cargoContent = VfsUtil.loadText(cargoToml);
            rustInfo.put("cargoToml", extractCargoInfo(cargoContent));
            
            // Add Rust specific build commands to build system
            enhanceBuildSystem(projectInfo);
            
            // Check for Rust-specific folders, warn if they're missing from .gitignore
            // Get project attributes to check for proper .gitignore configuration
            // Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
            
            // Add Rust information to project info
            projectInfo.put("rust", rustInfo);
            
        } catch (IOException e) {
            // Handle exception gracefully
        }
    }
    
    private @NotNull Map<String, Object> extractCargoInfo(String cargoContent) {
        Map<String, Object> cargoInfo = new HashMap<>();
        
        // Extract package name
        Pattern namePattern = Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");
        Matcher nameMatcher = namePattern.matcher(cargoContent);
        if (nameMatcher.find()) {
            cargoInfo.put("name", nameMatcher.group(1));
        }
        
        // Extract package version
        Pattern versionPattern = Pattern.compile("version\\s*=\\s*\"([^\"]+)\"");
        Matcher versionMatcher = versionPattern.matcher(cargoContent);
        if (versionMatcher.find()) {
            cargoInfo.put("version", versionMatcher.group(1));
        }
        
        // Check if it's a library or binary
        boolean isLib = cargoContent.contains("[lib]");
        boolean isBin = cargoContent.contains("[[bin]]");
        if (isLib) {
            cargoInfo.put("type", isLib && isBin ? "mixed" : "library");
        } else {
            cargoInfo.put("type", "binary");
        }
        
        // Extract dependencies (simple approach)
        Pattern depPattern = Pattern.compile("\\[dependencies](.*?)\\[", Pattern.DOTALL);
        Matcher depMatcher = depPattern.matcher(cargoContent + "\n[end]");
        if (depMatcher.find()) {
            cargoInfo.put("hasDependencies", true);
        }
        
        return cargoInfo;
    }
    
    private void enhanceBuildSystem(Map<String, Object> projectInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem != null) {
            // Update or add Rust-specific commands
            Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
            if (commands == null) {
                commands = new HashMap<>();
                buildSystem.put("commands", commands);
            }
            
            // Ensure we have the correct Rust commands
            commands.put("build", "cargo build");
            commands.put("run", "cargo run");
            commands.put("test", "cargo test");
            commands.put("singleTest", "cargo test test_name");
            commands.put("release", "cargo build --release");
            commands.put("check", "cargo check");
            commands.put("format", "cargo fmt");
            commands.put("lint", "cargo clippy");
        }
    }
}