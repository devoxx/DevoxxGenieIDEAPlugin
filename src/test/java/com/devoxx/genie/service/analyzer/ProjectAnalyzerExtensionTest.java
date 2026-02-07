package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.service.analyzer.languages.cpp.CppProjectScannerExtension;
import com.devoxx.genie.service.analyzer.languages.go.GoProjectScannerExtension;
import com.devoxx.genie.service.analyzer.languages.javascript.JavaScriptProjectScannerExtension;
import com.devoxx.genie.service.analyzer.languages.kotlin.KotlinProjectScannerExtension;
import com.devoxx.genie.service.analyzer.languages.php.PhpProjectScannerExtension;
import com.devoxx.genie.service.analyzer.languages.python.PythonProjectScannerExtension;
import com.devoxx.genie.service.analyzer.languages.rust.RustProjectScannerExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests that all ProjectAnalyzerExtension implementations use
 * ProjectUtil.guessProjectDir(project) instead of the deprecated project.getBaseDir().
 */
class ProjectAnalyzerExtensionTest {

    private Project mockProject;
    private VirtualFile mockBaseDir;

    @BeforeEach
    void setUp() {
        mockProject = mock(Project.class);
        mockBaseDir = mock(VirtualFile.class);
        when(mockBaseDir.isDirectory()).thenReturn(true);
        when(mockBaseDir.getPath()).thenReturn("/project");
        when(mockBaseDir.getName()).thenReturn("project");
    }

    @Test
    void cppExtension_usesProjectUtilGuessProjectDir() {
        CppProjectScannerExtension extension = new CppProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("C/C++");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject));
        }
    }

    @Test
    void cppExtension_handlesNullProjectDir() {
        CppProjectScannerExtension extension = new CppProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("C/C++");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            // Should return early without adding cpp info
            assertNull(projectInfo.get("cpp"));
        }
    }

    @Test
    void cppExtension_skipsNonCppProject() {
        CppProjectScannerExtension extension = new CppProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Java");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);

            // Should not even call guessProjectDir for non-C++ projects
            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject), never());
        }
    }

    @Test
    void goExtension_usesProjectUtilGuessProjectDir() {
        GoProjectScannerExtension extension = new GoProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Go");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject));
        }
    }

    @Test
    void goExtension_handlesNullProjectDir() {
        GoProjectScannerExtension extension = new GoProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Go");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            assertNull(projectInfo.get("go"));
        }
    }

    @Test
    void jsExtension_usesProjectUtilGuessProjectDir() {
        JavaScriptProjectScannerExtension extension = new JavaScriptProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("JavaScript");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject));
        }
    }

    @Test
    void jsExtension_handlesNullProjectDir() {
        JavaScriptProjectScannerExtension extension = new JavaScriptProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("JavaScript");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            assertNull(projectInfo.get("javascript"));
        }
    }

    @Test
    void kotlinExtension_usesProjectUtilGuessProjectDir() {
        KotlinProjectScannerExtension extension = new KotlinProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Kotlin");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject));
        }
    }

    @Test
    void kotlinExtension_handlesNullProjectDir() {
        KotlinProjectScannerExtension extension = new KotlinProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Kotlin");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            assertNull(projectInfo.get("kotlin"));
        }
    }

    @Test
    void phpExtension_usesProjectUtilGuessProjectDir() {
        PhpProjectScannerExtension extension = new PhpProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("PHP");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject));
        }
    }

    @Test
    void phpExtension_handlesNullProjectDir() {
        PhpProjectScannerExtension extension = new PhpProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("PHP");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            assertNull(projectInfo.get("php"));
        }
    }

    @Test
    void pythonExtension_usesProjectUtilGuessProjectDir() {
        PythonProjectScannerExtension extension = new PythonProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Python");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject));
        }
    }

    @Test
    void pythonExtension_handlesNullProjectDir() {
        PythonProjectScannerExtension extension = new PythonProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Python");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            assertNull(projectInfo.get("python"));
        }
    }

    @Test
    void rustExtension_usesProjectUtilGuessProjectDir() {
        RustProjectScannerExtension extension = new RustProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Rust");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(mockProject));
        }
    }

    @Test
    void rustExtension_handlesNullProjectDir() {
        RustProjectScannerExtension extension = new RustProjectScannerExtension();
        Map<String, Object> projectInfo = createProjectInfoWithLanguage("Rust");

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            assertNull(projectInfo.get("rust"));
        }
    }

    private Map<String, Object> createProjectInfoWithLanguage(String language) {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put(language, 10);
        projectInfo.put("languages", languages);
        return projectInfo;
    }
}
