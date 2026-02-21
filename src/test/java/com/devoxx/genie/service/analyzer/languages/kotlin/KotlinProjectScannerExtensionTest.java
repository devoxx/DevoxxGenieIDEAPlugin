package com.devoxx.genie.service.analyzer.languages.kotlin;

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
 * Tests for KotlinProjectScannerExtension, covering the refactored enhanceBuildSystem method
 * which was split into getBuildCommands, addFrameworkCommands, addAndroidCommands,
 * addMultiplatformCommands, and addCodeQualityCommands to reduce cognitive complexity
 * (SonarQube java:S3776).
 *
 * <p>VfsUtil.loadText delegates to VfsUtilCore.loadText. We mock VfsUtilCore with
 * {@code any(VirtualFile.class)} to avoid referencing the VirtualFile mock directly
 * in the lambda, which sidesteps a Mockito recorder-state issue in the IntelliJ test environment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KotlinProjectScannerExtensionTest {

    @Mock private Project mockProject;
    @Mock private VirtualFile mockBaseDir;
    @Mock private VirtualFile mockBuildGradleKts;
    @Mock private VirtualFile mockPomXml;

    private KotlinProjectScannerExtension extension;

    @BeforeEach
    void setUp() {
        extension = new KotlinProjectScannerExtension();
        when(mockBaseDir.isDirectory()).thenReturn(true);
        when(mockBaseDir.findChild(any(String.class))).thenReturn(null);
        when(mockBaseDir.getChildren()).thenReturn(new VirtualFile[0]);
        when(mockBuildGradleKts.isDirectory()).thenReturn(false);
        when(mockPomXml.isDirectory()).thenReturn(false);
    }

    private Map<String, Object> createKotlinProjectInfo() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("Kotlin", 10);
        projectInfo.put("languages", languages);
        return projectInfo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getCommands(Map<String, Object> projectInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        assertThat(buildSystem).isNotNull();
        return (Map<String, String>) buildSystem.get("commands");
    }

    // ─── Guard tests ─────────────────────────────────────────────────────────────

    @Test
    void enhanceProjectInfo_skipsNonKotlinProject() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("Java", 10);
        projectInfo.put("languages", languages);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);

            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(any()), never());
            assertThat(projectInfo.get("kotlin")).isNull();
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

    // ─── Build system command tests ───────────────────────────────────────────────

    @Test
    void enhanceBuildSystem_gradleKts_addsGradleCommands() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("build.gradle.kts")).thenReturn(mockBuildGradleKts);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("build")).isEqualTo("./gradlew build");
            assertThat(commands.get("test")).isEqualTo("./gradlew test");
            assertThat(commands.get("run")).isEqualTo("./gradlew run");
            assertThat(commands.get("clean")).isEqualTo("./gradlew clean");
            assertThat(commands.get("singleTest")).isEqualTo("./gradlew test --tests \"*TestName*\"");
        }
    }

    @Test
    void enhanceBuildSystem_mavenProject_addsMavenCommands() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("pom.xml")).thenReturn(mockPomXml);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("build")).isEqualTo("mvn clean package");
            assertThat(commands.get("test")).isEqualTo("mvn test");
            assertThat(commands.get("run")).isEqualTo("mvn exec:java");
            assertThat(commands.get("clean")).isEqualTo("mvn clean");
            assertThat(commands.get("singleTest")).isEqualTo("mvn test -Dtest=TestName");
        }
    }

    @Test
    void enhanceBuildSystem_noBuildFile_defaultsToGradleCommands() {
        // No build files → kotlinInfo has no "buildSystem" key → getOrDefault returns "Gradle (Kotlin DSL)"
        Map<String, Object> projectInfo = createKotlinProjectInfo();

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("build")).isEqualTo("./gradlew build");
            assertThat(commands.get("test")).isEqualTo("./gradlew test");
        }
    }

    // ─── Framework command tests ──────────────────────────────────────────────────

    @Test
    void addFrameworkCommands_springBootWithGradle_addsBootRunCommand() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("build.gradle.kts")).thenReturn(mockBuildGradleKts);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("org.springframework.boot");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("bootRun")).isEqualTo("./gradlew bootRun");
        }
    }

    @Test
    void addFrameworkCommands_springBootWithMaven_addsBootRunCommand() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("pom.xml")).thenReturn(mockPomXml);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("org.springframework.boot");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("bootRun")).isEqualTo("mvn spring-boot:run");
        }
    }

    // ─── Android command tests ────────────────────────────────────────────────────

    @Test
    void addAndroidCommands_androidGradleProject_addsAndroidCommands() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("build.gradle.kts")).thenReturn(mockBuildGradleKts);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("kotlin(\"android\")");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("assembleDebug")).isEqualTo("./gradlew assembleDebug");
            assertThat(commands.get("assembleRelease")).isEqualTo("./gradlew assembleRelease");
            assertThat(commands.get("installDebug")).isEqualTo("./gradlew installDebug");
            assertThat(commands.get("connectedAndroidTest")).isEqualTo("./gradlew connectedAndroidTest");
        }
    }

    // ─── Multiplatform command tests ──────────────────────────────────────────────

    @Test
    void addMultiplatformCommands_kmpGradleProject_addsMultiplatformCommands() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("build.gradle.kts")).thenReturn(mockBuildGradleKts);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("kotlin(\"multiplatform\")");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("jsBrowserRun")).isEqualTo("./gradlew jsBrowserRun");
            assertThat(commands.get("iosX64Test")).isEqualTo("./gradlew iosX64Test");
            assertThat(commands.get("allTests")).isEqualTo("./gradlew allTests");
        }
    }

    // ─── Code quality command tests ───────────────────────────────────────────────

    @Test
    void addCodeQualityCommands_ktlintWithGradle_addsKtlintCommands() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("build.gradle.kts")).thenReturn(mockBuildGradleKts);

        VirtualFile mockEditorConfig = mock(VirtualFile.class);
        when(mockBaseDir.findChild(".editorconfig")).thenReturn(mockEditorConfig);
        when(mockEditorConfig.isDirectory()).thenReturn(false);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            // Return "ktlint" content only when .editorconfig is read; empty for all others
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenAnswer(invocation -> {
                        VirtualFile vf = invocation.getArgument(0);
                        return (vf == mockEditorConfig) ? "ktlint" : "";
                    });

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("lint")).isEqualTo("./gradlew ktlintCheck");
            assertThat(commands.get("format")).isEqualTo("./gradlew ktlintFormat");
        }
    }

    @Test
    void addCodeQualityCommands_detektWithGradle_addsDetektCommand() {
        Map<String, Object> projectInfo = createKotlinProjectInfo();
        when(mockBaseDir.findChild("build.gradle.kts")).thenReturn(mockBuildGradleKts);

        VirtualFile mockDetektYml = mock(VirtualFile.class);
        when(mockBaseDir.findChild("detekt.yml")).thenReturn(mockDetektYml);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("detekt")).isEqualTo("./gradlew detekt");
        }
    }
}
