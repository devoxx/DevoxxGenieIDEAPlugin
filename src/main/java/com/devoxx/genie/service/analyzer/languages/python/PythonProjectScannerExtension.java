package com.devoxx.genie.service.analyzer.languages.python;

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
 * Extension to enhance project scanning with Python-specific details
 */
public class PythonProjectScannerExtension implements ProjectAnalyzerExtension {
    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if Python is detected as a language
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null || !languages.toString().contains("Python")) {
            return;
        }

        // Get project base directory
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            return;
        }

        // Process Python-specific information
        try {
            Map<String, Object> pythonInfo = new HashMap<>();
            
            // Check for various Python project files
            boolean hasPipfile = baseDir.findChild("Pipfile") != null;
            boolean hasRequirements = baseDir.findChild("requirements.txt") != null;
            boolean hasSetupPy = baseDir.findChild("setup.py") != null;
            boolean hasPyprojectToml = baseDir.findChild("pyproject.toml") != null;
            
            // Determine project type/tools
            if (hasPyprojectToml) {
                pythonInfo.put("buildTool", "Poetry/PEP 517");
                extractPyprojectInfo(baseDir, pythonInfo);
            } else if (hasPipfile) {
                pythonInfo.put("buildTool", "Pipenv");
            } else if (hasSetupPy) {
                pythonInfo.put("buildTool", "setuptools");
            } else if (hasRequirements) {
                pythonInfo.put("buildTool", "pip");
            }
            
            // Check for Python version specification
            VirtualFile pythonVersion = baseDir.findChild(".python-version");
            if (pythonVersion != null) {
                try {
                    pythonInfo.put("pythonVersion", VfsUtil.loadText(pythonVersion).trim());
                } catch (IOException ignored) {}
            }
            
            // Check for testing frameworks
            boolean hasPytest = containsFile(baseDir, "pytest.ini") || containsFile(baseDir, "conftest.py");
            boolean hasUnitTest = findFilesWithPattern(baseDir, "test_*.py") || findFilesWithPattern(baseDir, "*_test.py");
            
            if (hasPytest) {
                pythonInfo.put("testFramework", "pytest");
            } else if (hasUnitTest) {
                pythonInfo.put("testFramework", "unittest");
            }
            
            // Check for linting/formatting tools
            boolean hasFlake8 = baseDir.findChild(".flake8") != null;
            boolean hasBlack = findInFilePattern(baseDir, "pyproject.toml", ".*\\[tool\\.black\\].*");
            boolean hasMypy = baseDir.findChild("mypy.ini") != null;
            boolean hasIsort = findInFilePattern(baseDir, "pyproject.toml", ".*\\[tool\\.isort\\].*");
            
            if (hasFlake8) pythonInfo.put("linter", "flake8");
            if (hasBlack) pythonInfo.put("formatter", "black");
            if (hasMypy) pythonInfo.put("typeChecker", "mypy");
            if (hasIsort) pythonInfo.put("importSorter", "isort");
            
            // Add Python information to project info
            projectInfo.put("python", pythonInfo);
            
            // Add Python specific build commands to build system
            enhanceBuildSystem(projectInfo, pythonInfo);
            
        } catch (Exception e) {
            // Handle exception gracefully
        }
    }
    
    private void extractPyprojectInfo(VirtualFile baseDir, Map<String, Object> pythonInfo) {
        VirtualFile pyprojectToml = baseDir.findChild("pyproject.toml");
        if (pyprojectToml == null) return;
        
        try {
            String content = VfsUtil.loadText(pyprojectToml);
            
            // Check for poetry
            if (content.contains("[tool.poetry]")) {
                pythonInfo.put("buildTool", "Poetry");
                
                // Extract project name
                Pattern namePattern = Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");
                Matcher nameMatcher = namePattern.matcher(content);
                if (nameMatcher.find()) {
                    pythonInfo.put("name", nameMatcher.group(1));
                }
                
                // Extract Python version requirement
                Pattern pythonVersionPattern = Pattern.compile("python\\s*=\\s*\"([^\"]+)\"");
                Matcher pythonVersionMatcher = pythonVersionPattern.matcher(content);
                if (pythonVersionMatcher.find()) {
                    pythonInfo.put("pythonVersionRequirement", pythonVersionMatcher.group(1));
                }
            }
            
            // Check for other build backends
            if (content.contains("flit")) {
                pythonInfo.put("buildTool", "Flit");
            } else if (content.contains("pdm")) {
                pythonInfo.put("buildTool", "PDM");
            }
            
        } catch (IOException ignored) {}
    }
    
    private void enhanceBuildSystem(Map<String, Object> projectInfo, Map<String, Object> pythonInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem == null) {
            buildSystem = new HashMap<>();
            projectInfo.put("buildSystem", buildSystem);
        }
        
        // Update or add Python-specific commands
        Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
        if (commands == null) {
            commands = new HashMap<>();
            buildSystem.put("commands", commands);
        }
        
        // Determine test command based on detected test framework
        String testCommand = "python -m unittest discover";
        if (pythonInfo.containsKey("testFramework") && "pytest".equals(pythonInfo.get("testFramework"))) {
            testCommand = "pytest";
        }
        
        // Determine build/run command based on tool
        String buildTool = (String) pythonInfo.get("buildTool");
        if ("Poetry".equals(buildTool)) {
            commands.put("install", "poetry install");
            commands.put("build", "poetry build");
            commands.put("run", "poetry run python main.py");
            commands.put("test", "poetry run " + testCommand);
        } else if ("Pipenv".equals(buildTool)) {
            commands.put("install", "pipenv install");
            commands.put("run", "pipenv run python main.py");
            commands.put("test", "pipenv run " + testCommand);
        } else {
            commands.put("install", "pip install -r requirements.txt");
            commands.put("run", "python main.py");
            commands.put("test", testCommand);
        }
        
        // Add linting commands if present
        if (pythonInfo.containsKey("linter") && "flake8".equals(pythonInfo.get("linter"))) {
            commands.put("lint", "flake8 .");
        }
        
        if (pythonInfo.containsKey("formatter") && "black".equals(pythonInfo.get("formatter"))) {
            commands.put("format", "black .");
        }
        
        if (pythonInfo.containsKey("typeChecker") && "mypy".equals(pythonInfo.get("typeChecker"))) {
            commands.put("typeCheck", "mypy .");
        }
    }
    
    private boolean containsFile(VirtualFile dir, String filename) {
        return dir.findChild(filename) != null;
    }
    
    private boolean findFilesWithPattern(VirtualFile dir, String pattern) {
        // Simple pattern matcher for filenames
        Pattern globPattern = createGlobPattern(pattern);
        
        final boolean[] found = {false};
        
        VfsUtil.visitChildrenRecursively(dir, new com.intellij.openapi.vfs.VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory() && globPattern.matcher(file.getName()).matches()) {
                    found[0] = true;
                    return false; // Stop visiting
                }
                return true;
            }
        });
        
        return found[0];
    }
    
    private boolean findInFilePattern(VirtualFile dir, String filename, String regex) {
        VirtualFile file = dir.findChild(filename);
        if (file != null && !file.isDirectory()) {
            try {
                String content = VfsUtil.loadText(file);
                return Pattern.compile(regex, Pattern.DOTALL).matcher(content).matches();
            } catch (IOException ignored) {}
        }
        return false;
    }
    
    private Pattern createGlobPattern(String glob) {
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
}
