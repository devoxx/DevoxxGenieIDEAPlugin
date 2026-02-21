package com.devoxx.genie.service.analyzer.languages.go;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for GoProjectScannerExtension, covering the refactored detectGoFrameworks method
 * which was split into detectGoFrameworks + detectGoFrameworksFromGoSum to reduce
 * cognitive complexity (SonarQube java:S3776).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoProjectScannerExtensionTest {

    @Mock private Project mockProject;
    @Mock private VirtualFile mockBaseDir;
    @Mock private VirtualFile mockGoSumFile;

    private GoProjectScannerExtension extension;

    @BeforeEach
    void setUp() {
        extension = new GoProjectScannerExtension();
        when(mockBaseDir.isDirectory()).thenReturn(true);
        when(mockBaseDir.findChild("go.mod")).thenReturn(null);
        when(mockBaseDir.findChild("go.sum")).thenReturn(null);
        when(mockBaseDir.findChild("Gopkg.toml")).thenReturn(null);
        when(mockBaseDir.findChild(".golangci.yml")).thenReturn(null);
        when(mockBaseDir.findChild(".golangci.yaml")).thenReturn(null);
        when(mockBaseDir.findChild("Makefile")).thenReturn(null);
    }

    private Map<String, Object> createGoProjectInfo() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("Go", 10);
        projectInfo.put("languages", languages);
        return projectInfo;
    }

    @Test
    void enhanceProjectInfo_skipsNonGoProject() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("Java", 10);
        projectInfo.put("languages", languages);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(any()), never());
            assertThat(projectInfo.get("go")).isNull();
        }
    }

    @Test
    void enhanceProjectInfo_skipsNullLanguages() {
        Map<String, Object> projectInfo = new HashMap<>();

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(any()), never());
        }
    }

    @Test
    void detectGoFrameworks_echoFrameworkDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/labstack/echo v4.11.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo).isNotNull();
            assertThat(goInfo.get("webFramework")).isEqualTo("Echo");
        }
    }

    @Test
    void detectGoFrameworks_ginFrameworkDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/gin-gonic/gin v1.9.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("webFramework")).isEqualTo("Gin");
        }
    }

    @Test
    void detectGoFrameworks_gorillaFrameworkDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/gorilla/mux v1.8.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("webFramework")).isEqualTo("Gorilla");
        }
    }

    @Test
    void detectGoFrameworks_fiberFrameworkDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/gofiber/fiber v2.0.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("webFramework")).isEqualTo("Fiber");
        }
    }

    @Test
    void detectGoFrameworks_chiFrameworkDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/go-chi/chi v5.0.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("webFramework")).isEqualTo("Chi");
        }
    }

    @Test
    void detectGoFrameworks_gormOrmDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("gorm.io/gorm v1.23.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("orm")).isEqualTo("GORM");
        }
    }

    @Test
    void detectGoFrameworks_graphqlGoDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/graphql-go/graphql v0.8.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("graphql")).isEqualTo("graphql-go");
        }
    }

    @Test
    void detectGoFrameworks_gqlgenDetectedFromGoSum() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/99designs/gqlgen v0.17.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("graphql")).isEqualTo("gqlgen");
        }
    }

    @Test
    void detectGoFrameworks_noGoSum_noFrameworkDetected() {
        Map<String, Object> projectInfo = createGoProjectInfo();
        // go.sum returns null (set in setUp)

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo).isNotNull();
            assertThat(goInfo.get("webFramework")).isNull();
        }
    }

    @Test
    void detectGoFrameworks_goSumWithMultipleLibraries_detectsEchoAndGorm() throws IOException {
        Map<String, Object> projectInfo = createGoProjectInfo();
        when(mockBaseDir.findChild("go.sum")).thenReturn(mockGoSumFile);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtil> vfsUtilMock = mockStatic(VfsUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilMock.when(() -> VfsUtil.loadText(mockGoSumFile))
                    .thenReturn("github.com/labstack/echo v4.11.0 h1:...\ngorm.io/gorm v1.23.0 h1:...");
            vfsUtilCoreMock.when(() -> VfsUtilCore.visitChildrenRecursively(eq(mockBaseDir), any(VirtualFileVisitor.class)))
                    .then(invocation -> null);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> goInfo = (Map<String, Object>) projectInfo.get("go");
            assertThat(goInfo.get("webFramework")).isEqualTo("Echo");
            assertThat(goInfo.get("orm")).isEqualTo("GORM");
        }
    }
}
