package com.devoxx.genie.service.analyzer.languages.cpp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for CppProjectScannerExtension, covering the refactored detectBuildSystem,
 * detectQualityTools, and detectTestFramework methods to reduce cognitive complexity
 * (SonarQube java:S3776).
 *
 * <p>VfsUtil.loadText delegates to VfsUtilCore.loadText. We mock VfsUtilCore with
 * {@code any(VirtualFile.class)} to avoid referencing the VirtualFile mock directly
 * in the lambda, which sidesteps a Mockito recorder-state issue in the IntelliJ test environment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CppProjectScannerExtensionTest {

    @Mock private Project mockProject;
    @Mock private VirtualFile mockBaseDir;
    @Mock private VirtualFile mockFile;

    private CppProjectScannerExtension extension;

    @BeforeEach
    void setUp() {
        extension = new CppProjectScannerExtension();
        when(mockBaseDir.isDirectory()).thenReturn(true);
        when(mockBaseDir.findChild(any(String.class))).thenReturn(null);
        when(mockFile.isDirectory()).thenReturn(false);
    }

    private Map<String, Object> createCppProjectInfo() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("C/C++", 10);
        projectInfo.put("languages", languages);
        return projectInfo;
    }

    @Test
    void enhanceProjectInfo_skipsNonCppProject() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("Java", 10);
        projectInfo.put("languages", languages);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(any()), never());
            assertThat(projectInfo.get("cpp")).isNull();
        }
    }

    @Test
    void enhanceProjectInfo_skipsNullLanguages() {
        Map<String, Object> projectInfo = new HashMap<>();

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(any()), never());
            assertThat(projectInfo.get("cpp")).isNull();
        }
    }

    @Test
    void enhanceProjectInfo_skipsNullBaseDir() {
        Map<String, Object> projectInfo = createCppProjectInfo();

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            assertThat(projectInfo.get("cpp")).isNull();
        }
    }

    @Test
    void detectBuildSystem_cmakeDetected() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("CMakeLists.txt")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo).isNotNull();
            assertThat(cppInfo.get("buildSystem")).isEqualTo("CMake");
        }
    }

    @Test
    void detectBuildSystem_makefileDetected() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("Makefile")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("buildSystem")).isEqualTo("Make");
        }
    }

    @Test
    void detectBuildSystem_bazelDetectedViaBuildFile() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("BUILD")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("buildSystem")).isEqualTo("Bazel");
        }
    }

    @Test
    void detectBuildSystem_bazelDetectedViaWorkspace() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("WORKSPACE")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("buildSystem")).isEqualTo("Bazel");
        }
    }

    @Test
    void detectBuildSystem_conanDependencyManagerDetected() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("conanfile.txt")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("dependencyManager")).isEqualTo("Conan");
        }
    }

    @Test
    void detectQualityTools_clangTidyDetected() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild(".clang-tidy")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("staticAnalyzer")).isEqualTo("clang-tidy");
        }
    }

    @Test
    void detectQualityTools_clangFormatDetected() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild(".clang-format")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("formatter")).isEqualTo("clang-format");
        }
    }

    @Test
    void detectTestFramework_googleTestDetectedFromCMake() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("CMakeLists.txt")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("find_package(GTest)\ntarget_link_libraries(mylib gtest)");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("testFramework")).isEqualTo("GoogleTest");
        }
    }

    @Test
    void detectTestFramework_catch2DetectedFromCMake() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("CMakeLists.txt")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("find_package(Catch2 REQUIRED)");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("testFramework")).isEqualTo("Catch2");
        }
    }

    @Test
    void detectTestFramework_boostTestDetectedFromConanfile() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("conanfile.txt")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("[requires]\nboost/test/1.82.0");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo.get("testFramework")).isEqualTo("Boost.Test");
        }
    }

    @Test
    void detectTestFramework_noFrameworkWhenNoFiles() {
        Map<String, Object> projectInfo = createCppProjectInfo();

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> cppInfo = (Map<String, Object>) projectInfo.get("cpp");
            assertThat(cppInfo).isNotNull();
            assertThat(cppInfo.get("testFramework")).isNull();
        }
    }

    @Test
    void enhanceBuildSystem_cmakeAddsConfigureAndBuildCommands() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("CMakeLists.txt")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
            @SuppressWarnings("unchecked")
            Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
            assertThat(commands.get("configure")).isEqualTo("cmake -B build");
            assertThat(commands.get("build")).isEqualTo("cmake --build build");
        }
    }

    @Test
    void enhanceBuildSystem_makeAddsCommands() {
        Map<String, Object> projectInfo = createCppProjectInfo();
        when(mockBaseDir.findChild("Makefile")).thenReturn(mockFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
            @SuppressWarnings("unchecked")
            Map<String, String> commands = (Map<String, String>) buildSystem.get("commands");
            assertThat(commands.get("build")).isEqualTo("make");
            assertThat(commands.get("clean")).isEqualTo("make clean");
        }
    }
}
