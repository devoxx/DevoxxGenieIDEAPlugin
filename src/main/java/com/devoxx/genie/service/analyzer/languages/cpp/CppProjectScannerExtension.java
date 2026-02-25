package com.devoxx.genie.service.analyzer.languages.cpp;

import com.devoxx.genie.service.analyzer.ProjectAnalyzerExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension to enhance project scanning with C/C++-specific details
 */
public class CppProjectScannerExtension implements ProjectAnalyzerExtension {

    public static final String TEST_FRAMEWORK = "testFramework";
    public static final String BUILD = "build";
    public static final String CLEAN = "clean";
    public static final String INSTALL = "install";
    public static final String CONFIGURE = "configure";

    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if C/C++ is detected as a language
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null || !languages.toString().contains("C/C++")) {
            return;
        }

        // Get project base directory
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            return;
        }

        // Process C/C++-specific information
        try {
            Map<String, Object> cppInfo = new HashMap<>();
            detectBuildSystem(baseDir, cppInfo);
            detectQualityTools(baseDir, cppInfo);
            detectTestFramework(baseDir, cppInfo);
            projectInfo.put("cpp", cppInfo);
            enhanceBuildSystem(projectInfo, cppInfo);
        } catch (Exception e) {
            // Handle exception gracefully
        }
    }
    
    private void detectBuildSystem(@NotNull VirtualFile baseDir, Map<String, Object> cppInfo) {
        boolean hasCMake = baseDir.findChild("CMakeLists.txt") != null;
        boolean hasMakefile = baseDir.findChild("Makefile") != null;
        boolean hasBazel = baseDir.findChild("BUILD") != null || baseDir.findChild("WORKSPACE") != null;
        boolean hasConanfile = baseDir.findChild("conanfile.txt") != null || baseDir.findChild("conanfile.py") != null;
        boolean hasVcxproj = baseDir.findChild("*.vcxproj") != null; // Visual Studio

        if (hasCMake) {
            cppInfo.put("buildSystem", "CMake");
            extractCMakeInfo(baseDir, cppInfo);
        } else if (hasMakefile) {
            cppInfo.put("buildSystem", "Make");
        } else if (hasBazel) {
            cppInfo.put("buildSystem", "Bazel");
        } else if (hasVcxproj) {
            cppInfo.put("buildSystem", "Visual Studio");
        }

        if (hasConanfile) {
            cppInfo.put("dependencyManager", "Conan");
        }
    }

    private void detectQualityTools(@NotNull VirtualFile baseDir, Map<String, Object> cppInfo) {
        if (baseDir.findChild(".clang-tidy") != null) {
            cppInfo.put("staticAnalyzer", "clang-tidy");
        }
        if (baseDir.findChild(".clang-format") != null) {
            cppInfo.put("formatter", "clang-format");
        }
    }

    private void detectTestFramework(@NotNull VirtualFile baseDir, Map<String, Object> cppInfo) {
        boolean hasGTest = findInFile(baseDir, "CMakeLists.txt", "gtest") ||
                           findInFile(baseDir, "conanfile.txt", "gtest");
        boolean hasCatch = findInFile(baseDir, "CMakeLists.txt", "Catch2") ||
                           findInFile(baseDir, "conanfile.txt", "catch2");
        boolean hasBoostTest = findInFile(baseDir, "CMakeLists.txt", "Boost.Test") ||
                               findInFile(baseDir, "conanfile.txt", "boost/test");

        if (hasGTest) {
            cppInfo.put(TEST_FRAMEWORK, "GoogleTest");
        } else if (hasCatch) {
            cppInfo.put(TEST_FRAMEWORK, "Catch2");
        } else if (hasBoostTest) {
            cppInfo.put(TEST_FRAMEWORK, "Boost.Test");
        }
    }

    private void extractCMakeInfo(@NotNull VirtualFile baseDir, Map<String, Object> cppInfo) {
        VirtualFile cmakeFile = baseDir.findChild("CMakeLists.txt");
        if (cmakeFile == null) return;
        
        try {
            String content = VfsUtilCore.loadText(cmakeFile);
            
            // Extract project name
            Pattern projectPattern = Pattern.compile("project\\s*\\(\\s*([^\\s)]+)");
            Matcher projectMatcher = projectPattern.matcher(content);
            if (projectMatcher.find()) {
                cppInfo.put("projectName", projectMatcher.group(1));
            }
            
            // Extract CMake minimum version
            Pattern versionPattern = Pattern.compile("cmake_minimum_required\\s*\\(\\s*VERSION\\s+([^\\s)]+)");
            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                cppInfo.put("cmakeVersion", versionMatcher.group(1));
            }
            
            // Check for C++ standard
            Pattern cppStdPattern = Pattern.compile("set\\s*\\(\\s*CMAKE_CXX_STANDARD\\s+([0-9]+)");
            Matcher cppStdMatcher = cppStdPattern.matcher(content);
            if (cppStdMatcher.find()) {
                cppInfo.put("cppStandard", cppStdMatcher.group(1));
            }
            
        } catch (IOException ignored) {}
    }
    
    private boolean findInFile(@NotNull VirtualFile baseDir, String fileName, String content) {
        VirtualFile file = baseDir.findChild(fileName);
        if (file != null && !file.isDirectory()) {
            try {
                String text = VfsUtilCore.loadText(file);
                return text.contains(content);
            } catch (IOException ignored) {}
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private void enhanceBuildSystem(@NotNull Map<String, Object> projectInfo, Map<String, Object> cppInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem == null) {
            buildSystem = new HashMap<>();
            projectInfo.put("buildSystem", buildSystem);
        }
        
        // Update or add C++-specific commands
        Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
        if (commands == null) {
            commands = new HashMap<>();
            buildSystem.put("commands", commands);
        }
        
        // Add build commands based on detected build system
        String buildSystemType = (String) cppInfo.get("buildSystem");
        if ("CMake".equals(buildSystemType)) {
            commands.put(CONFIGURE, "cmake -B build");
            commands.put(BUILD, "cmake --build build");
            commands.put(CLEAN, "cmake --build build --target clean");
            commands.put(INSTALL, "cmake --install build");
            
            // Add testing commands if a test framework was detected
            if (cppInfo.containsKey(TEST_FRAMEWORK)) {
                commands.put("test", "cd build && ctest");
                commands.put("singleTest", "cd build && ctest -R Test_Name");
            }
        } else if ("Make".equalsIgnoreCase(buildSystemType)) {
            commands.put(BUILD, "make");
            commands.put(CLEAN, "make clean");
            commands.put("test", "make test");
        } else if ("Bazel".equalsIgnoreCase(buildSystemType)) {
            commands.put(BUILD, "bazel build //...");
            commands.put("test", "bazel test //...");
        }
        
        // Add code quality commands
        if (cppInfo.containsKey("formatter") && "clang-format".equals(cppInfo.get("formatter"))) {
            commands.put("format", "clang-format -i src/**/*.cpp src/**/*.h");
        }
        
        if (cppInfo.containsKey("staticAnalyzer") && "clang-tidy".equals(cppInfo.get("staticAnalyzer"))) {
            commands.put("lint", "clang-tidy src/**/*.cpp");
        }
        
        // Add dependency manager commands
        if (cppInfo.containsKey("dependencyManager") && "Conan".equals(cppInfo.get("dependencyManager"))) {
            commands.put("deps", "conan install . --build=missing");
        }
    }
}
