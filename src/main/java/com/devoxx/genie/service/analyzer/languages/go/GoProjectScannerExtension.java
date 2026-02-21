package com.devoxx.genie.service.analyzer.languages.go;

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
 * Extension to enhance project scanning with Go-specific details
 */
public class GoProjectScannerExtension implements ProjectAnalyzerExtension {
    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if Go is detected as a language
        Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
        if (languages == null || !languages.toString().contains("Go")) {
            return;
        }

        // Get project base directory
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            return;
        }

        // Process Go-specific information
        try {
            Map<String, Object> goInfo = new HashMap<>();
            
            // Check for go.mod file (Go modules)
            VirtualFile goMod = baseDir.findChild("go.mod");
            if (goMod != null) {
                goInfo.put("dependencyManagement", "Go Modules");
                extractGoModInfo(goMod, goInfo);
            } else {
                // Check for dep or other older dependency management
                VirtualFile gopkg = baseDir.findChild("Gopkg.toml");
                if (gopkg != null) {
                    goInfo.put("dependencyManagement", "Dep");
                }
            }
            
            // Check for common Go frameworks
            detectGoFrameworks(baseDir, goInfo);
            
            // Check for Go tooling configuration
            detectGoTools(baseDir, goInfo);
            
            // Check for testing and benchmarking files
            detectGoTestingPractices(baseDir, goInfo);
            
            // Add Go information to project info
            projectInfo.put("go", goInfo);
            
            // Add Go specific build commands to build system
            enhanceBuildSystem(projectInfo, goInfo);
            
        } catch (Exception e) {
            // Handle exception gracefully
        }
    }
    
    private void extractGoModInfo(VirtualFile goMod, Map<String, Object> goInfo) {
        try {
            String content = VfsUtil.loadText(goMod);
            
            // Extract module name
            Pattern modulePattern = Pattern.compile("module\\s+([^\\s\\n]+)");
            Matcher moduleMatcher = modulePattern.matcher(content);
            if (moduleMatcher.find()) {
                goInfo.put("moduleName", moduleMatcher.group(1));
            }
            
            // Extract Go version
            Pattern goVersionPattern = Pattern.compile("go\\s+([0-9]+(\\.[0-9]+)*)");
            Matcher goVersionMatcher = goVersionPattern.matcher(content);
            if (goVersionMatcher.find()) {
                goInfo.put("goVersion", goVersionMatcher.group(1));
            }
            
            // Check if it has replace directives
            if (content.contains("replace ")) {
                goInfo.put("hasReplaceDirectives", true);
            }
            
        } catch (IOException ignored) {}
    }
    
    private void detectGoFrameworks(VirtualFile baseDir, Map<String, Object> goInfo) {
        // Check for common Go web frameworks
        VirtualFile goSum = baseDir.findChild("go.sum");
        if (goSum != null) {
            detectGoFrameworksFromGoSum(goSum, goInfo);
        }
        
        // Check for specific file patterns
        boolean hasEchoImports = findFileWithContent(baseDir, "*.go", "github.com/labstack/echo");
        boolean hasGinImports = findFileWithContent(baseDir, "*.go", "github.com/gin-gonic/gin");
        boolean hasGorillaImports = findFileWithContent(baseDir, "*.go", "github.com/gorilla/mux");
        boolean hasFiberImports = findFileWithContent(baseDir, "*.go", "github.com/gofiber/fiber");
        boolean hasChiImports = findFileWithContent(baseDir, "*.go", "github.com/go-chi/chi");
        
        if (hasEchoImports) {
            goInfo.put("webFramework", "Echo");
        } else if (hasGinImports) {
            goInfo.put("webFramework", "Gin");
        } else if (hasGorillaImports) {
            goInfo.put("webFramework", "Gorilla");
        } else if (hasFiberImports) {
            goInfo.put("webFramework", "Fiber");
        } else if (hasChiImports) {
            goInfo.put("webFramework", "Chi");
        }
    }
    
    private void detectGoFrameworksFromGoSum(VirtualFile goSum, Map<String, Object> goInfo) {
        try {
            String content = VfsUtil.loadText(goSum);

            // Check for Echo framework
            if (content.contains("github.com/labstack/echo")) {
                goInfo.put("webFramework", "Echo");
            } else if (content.contains("github.com/gin-gonic/gin")) {
                goInfo.put("webFramework", "Gin");
            } else if (content.contains("github.com/gorilla/mux")) {
                goInfo.put("webFramework", "Gorilla");
            } else if (content.contains("github.com/gofiber/fiber")) {
                goInfo.put("webFramework", "Fiber");
            } else if (content.contains("github.com/go-chi/chi")) {
                goInfo.put("webFramework", "Chi");
            }

            // Check for GORM
            if (content.contains("gorm.io/gorm")) {
                goInfo.put("orm", "GORM");
            }

            // Check for GraphQL libraries
            if (content.contains("github.com/graphql-go/graphql")) {
                goInfo.put("graphql", "graphql-go");
            } else if (content.contains("github.com/99designs/gqlgen")) {
                goInfo.put("graphql", "gqlgen");
            }

        } catch (IOException ignored) {}
    }

    private void detectGoTools(VirtualFile baseDir, Map<String, Object> goInfo) {
        // Check for golangci-lint configuration
        VirtualFile golangciYaml = baseDir.findChild(".golangci.yml");
        VirtualFile golangciYml = baseDir.findChild(".golangci.yaml");
        
        if (golangciYaml != null || golangciYml != null) {
            goInfo.put("linter", "golangci-lint");
        }
        
        // Check for go vet usage via Makefile
        VirtualFile makefile = baseDir.findChild("Makefile");
        if (makefile != null) {
            try {
                String content = VfsUtil.loadText(makefile);
                if (content.contains("go vet")) {
                    goInfo.put("staticAnalysis", "go vet");
                }
            } catch (IOException ignored) {}
        }
        
        // Check for Go formatter usage
        if (makefile != null) {
            try {
                String content = VfsUtil.loadText(makefile);
                if (content.contains("gofmt") || content.contains("go fmt")) {
                    goInfo.put("formatter", content.contains("gofmt") ? "gofmt" : "go fmt");
                }
            } catch (IOException ignored) {}
        }
    }
    
    private void detectGoTestingPractices(VirtualFile baseDir, Map<String, Object> goInfo) {
        // Check for test files
        boolean hasTests = findFilesWithPattern(baseDir);
        if (hasTests) {
            goInfo.put("hasTests", true);
        }
        
        // Check for benchmarks
        boolean hasBenchmarks = findFileWithContent(baseDir, "*_test.go", "func Benchmark");
        if (hasBenchmarks) {
            goInfo.put("hasBenchmarks", true);
        }
        
        // Check for testify
        boolean hasTestify = findFileWithContent(baseDir, "*.go", "github.com/stretchr/testify");
        if (hasTestify) {
            goInfo.put("testFramework", "testify");
        }
        
        // Check for gomock
        boolean hasGoMock = findFileWithContent(baseDir, "*.go", "github.com/golang/mock");
        if (hasGoMock) {
            goInfo.put("mockingFramework", "gomock");
        }
    }
    
    private boolean findFilesWithPattern(VirtualFile dir) {
        Pattern globPattern = createGlobPattern("*_test.go");
        
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
    
    private boolean findFileWithContent(VirtualFile dir, String filePattern, String content) {
        Pattern globPattern = createGlobPattern(filePattern);
        
        final boolean[] found = {false};
        
        VfsUtil.visitChildrenRecursively(dir, new com.intellij.openapi.vfs.VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory() && globPattern.matcher(file.getName()).matches()) {
                    try {
                        String fileContent = VfsUtil.loadText(file);
                        if (fileContent.contains(content)) {
                            found[0] = true;
                            return false; // Stop visiting
                        }
                    } catch (IOException ignored) {}
                }
                return !found[0]; // Continue visiting if not found yet
            }
        });
        
        return found[0];
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
    
    private void enhanceBuildSystem(@NotNull Map<String, Object> projectInfo, Map<String, Object> goInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem == null) {
            buildSystem = new HashMap<>();
            projectInfo.put("buildSystem", buildSystem);
        }
        
        // Update or add Go-specific commands
        Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
        if (commands == null) {
            commands = new HashMap<>();
            buildSystem.put("commands", commands);
        }
        
        // Add standard Go commands
        commands.put("build", "go build");
        commands.put("run", "go run .");
        commands.put("test", "go test ./...");
        commands.put("singleTest", "go test -v -run=TestName");
        commands.put("coverage", "go test -cover ./...");
        commands.put("benchmark", "go test -bench=. ./...");
        
        // Add dependency management commands
        if (goInfo.containsKey("dependencyManagement")) {
            String depManager = (String) goInfo.get("dependencyManagement");
            
            if ("Go Modules".equals(depManager)) {
                commands.put("deps", "go mod tidy");
                commands.put("download", "go mod download");
                commands.put("vendor", "go mod vendor");
            } else if ("Dep".equals(depManager)) {
                commands.put("deps", "dep ensure");
            }
        }
        
        // Add linting and formatting commands
        if (goInfo.containsKey("linter") && "golangci-lint".equals(goInfo.get("linter"))) {
            commands.put("lint", "golangci-lint run");
        } else {
            commands.put("lint", "go vet ./...");
        }
        
        String formatter = (String) goInfo.getOrDefault("formatter", "gofmt");
        commands.put("format", formatter.equals("gofmt") ? "gofmt -w ." : "go fmt ./...");
        
        // Add tool installation commands
        commands.put("tools", "go install golang.org/x/tools/...");
        
        // Add custom commands for common scenarios
        commands.put("clean", "go clean");
        commands.put("doc", "godoc -http=:6060");
        
        // Add cross-compilation example
        commands.put("cross-compile", "GOOS=linux GOARCH=amd64 go build");
    }
}
