package com.devoxx.genie.service.analyzer.languages.javascript;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for JavaScriptProjectScannerExtension, covering the refactored enhanceBuildSystem method
 * and helper methods to verify that cognitive complexity was reduced (SonarQube java:S3776).
 *
 * <p>VfsUtil.loadText delegates to VfsUtilCore.loadText. We mock VfsUtilCore with
 * {@code any(VirtualFile.class)} to avoid referencing the VirtualFile mock directly
 * in the lambda, which sidesteps a Mockito recorder-state issue in the IntelliJ test environment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JavaScriptProjectScannerExtensionTest {

    @Mock private Project mockProject;
    @Mock private VirtualFile mockBaseDir;
    @Mock private VirtualFile mockPackageJson;
    @Mock private VirtualFile mockPackageJsonParent;

    private JavaScriptProjectScannerExtension extension;

    @BeforeEach
    void setUp() throws IOException {
        extension = new JavaScriptProjectScannerExtension();
        when(mockBaseDir.isDirectory()).thenReturn(true);
        when(mockBaseDir.findChild(any(String.class))).thenReturn(null);
    }

    private Map<String, Object> createJsProjectInfo() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("JavaScript", 10);
        projectInfo.put("languages", languages);
        return projectInfo;
    }

    // ─── Guard tests (no file I/O needed) ───────────────────────────────────────

    @Test
    void enhanceProjectInfo_skipsNonJsProject() {
        Map<String, Object> projectInfo = new HashMap<>();
        Map<String, Object> languages = new HashMap<>();
        languages.put("Java", 10);
        projectInfo.put("languages", languages);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);
            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(any()), never());
            assertThat(projectInfo.get("javascript")).isNull();
        }
    }

    @Test
    void enhanceProjectInfo_skipsNullLanguages() {
        Map<String, Object> projectInfo = new HashMap<>();
        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            extension.enhanceProjectInfo(mockProject, projectInfo);
            projectUtilMock.verify(() -> ProjectUtil.guessProjectDir(any()), never());
            assertThat(projectInfo.get("javascript")).isNull();
        }
    }

    @Test
    void enhanceProjectInfo_skipsNullBaseDir() {
        Map<String, Object> projectInfo = createJsProjectInfo();
        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(null);
            extension.enhanceProjectInfo(mockProject, projectInfo);
            assertThat(projectInfo.get("javascript")).isNull();
        }
    }

    // ─── Default npm build commands (no package.json → defaults) ────────────────

    @Test
    void enhanceBuildSystem_usesNpmByDefault() {
        // No package.json → jsInfo has no packageManager → defaults to npm
        Map<String, Object> projectInfo = createJsProjectInfo();
        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("install")).isEqualTo("npm install");
            assertThat(commands.get("start")).isEqualTo("npm run start");
            assertThat(commands.get("build")).isEqualTo("npm run build");
        }
    }

    @Test
    void enhanceBuildSystem_detectsJavaScriptLanguageWhenNoPackageJson() {
        Map<String, Object> projectInfo = createJsProjectInfo();
        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo).isNotNull();
            assertThat(jsInfo.get("language")).isEqualTo("JavaScript");
        }
    }

    @Test
    void enhanceBuildSystem_createsBuildSystemMapIfAbsent() {
        Map<String, Object> projectInfo = createJsProjectInfo();
        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class)) {
            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            extension.enhanceProjectInfo(mockProject, projectInfo);
            assertThat(projectInfo.get("buildSystem")).isNotNull();
        }
    }

    // ─── Package manager detection ───────────────────────────────────────────────

    @Test
    void enhanceBuildSystem_usesYarnWhenYarnLockPresent() throws IOException {
        String yarnDir = createTempDirWithFile("yarn.lock");
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(yarnDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("{}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("install")).isEqualTo("yarn");
            assertThat(commands.get("start")).isEqualTo("yarn start");
            assertThat(commands.get("build")).isEqualTo("yarn build");
        }
    }

    @Test
    void enhanceBuildSystem_usesPnpmWhenPnpmLockPresent() throws IOException {
        String pnpmDir = createTempDirWithFile("pnpm-lock.yaml");
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(pnpmDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("{}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("install")).isEqualTo("pnpm install");
            assertThat(commands.get("start")).isEqualTo("pnpm start");
            assertThat(commands.get("build")).isEqualTo("pnpm build");
        }
    }

    // ─── Test framework detection ────────────────────────────────────────────────

    @Test
    void addTestCommands_addsJestCommandsWhenJestDetected() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"devDependencies\": {\"jest\": \"^29.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("testFramework")).isEqualTo("Jest");
            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("test")).isEqualTo("npm run test");
            assertThat(commands.get("testWatch")).isEqualTo("npm run test -- --watch");
            assertThat(commands.get("singleTest")).isEqualTo("npm run test -- -t \"Test Name\"");
        }
    }

    @Test
    void addTestCommands_addsMochaCommandWhenMochaDetected() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"devDependencies\": {\"mocha\": \"^10.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("testFramework")).isEqualTo("Mocha");
            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("test")).isEqualTo("npm run test");
            assertThat(commands.containsKey("testWatch")).isFalse();
        }
    }

    // ─── E2E test framework detection ───────────────────────────────────────────

    @Test
    void addE2eTestCommands_addsCypressCommandsWhenCypressDetected() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"devDependencies\": {\"cypress\": \"^13.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("e2e")).isEqualTo("npm run cypress:open");
            assertThat(commands.get("e2eHeadless")).isEqualTo("npm run cypress:run");
        }
    }

    @Test
    void addE2eTestCommands_addsPlaywrightCommandsWhenPlaywrightDetected() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"devDependencies\": {\"@playwright/test\": \"^1.40.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("e2e")).isEqualTo("npm run playwright test");
            assertThat(commands.get("e2eUI")).isEqualTo("npm run playwright test --ui");
        }
    }

    // ─── Lint / format detection ─────────────────────────────────────────────────

    @Test
    void addLintFormatCommands_addsEslintCommandsWhenEslintConfigFound() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockBaseDir.findChild(".eslintrc.json")).thenReturn(mock(VirtualFile.class));
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("{}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("lint")).isEqualTo("npm run lint");
            assertThat(commands.get("lintFix")).isEqualTo("npm run lint -- --fix");
        }
    }

    @Test
    void addLintFormatCommands_addsPrettierCommandWhenPrettierConfigFound() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockBaseDir.findChild(".prettierrc")).thenReturn(mock(VirtualFile.class));
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class))).thenReturn("{}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("format")).isEqualTo("npm run format");
        }
    }

    // ─── Framework detection ─────────────────────────────────────────────────────

    @Test
    void detectJsFrameworks_detectsReactFramework() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"react\": \"^18.0.0\", \"react-dom\": \"^18.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("framework")).isEqualTo("React");
        }
    }

    @Test
    void detectJsFrameworks_detectsNextJsFramework() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"react\": \"^18.0.0\", \"next\": \"^14.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("framework")).isEqualTo("Next.js");
        }
    }

    @Test
    void detectJsFrameworks_detectsVueFramework() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"vue\": \"^3.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("framework")).isEqualTo("Vue");
        }
    }

    @Test
    void detectJsFrameworks_detectsExpressFramework() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"express\": \"^4.18.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("framework")).isEqualTo("Express.js");
        }
    }

    @Test
    void detectJsFrameworks_detectsSvelteKitFramework() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"devDependencies\": {\"svelte\": \"^4.0.0\", \"@sveltejs/kit\": \"^2.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("framework")).isEqualTo("SvelteKit");
        }
    }

    // ─── Framework-specific build commands ──────────────────────────────────────

    @Test
    void addFrameworkCommands_addsNextJsCommands() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"react\": \"^18.0.0\", \"next\": \"^14.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("dev")).isEqualTo("npm run dev");
            assertThat(commands.get("export")).isEqualTo("npm run export");
        }
    }

    @Test
    void addFrameworkCommands_addsNuxtJsCommands() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"vue\": \"^3.0.0\", \"nuxt\": \"^3.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("framework")).isEqualTo("Nuxt.js");
            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("dev")).isEqualTo("npm run dev");
            assertThat(commands.get("generate")).isEqualTo("npm run generate");
        }
    }

    @Test
    void addFrameworkCommands_addsAngularCommandsWithNpxForNpm() throws IOException {
        String noLockDir = createEmptyTempDir();
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(noLockDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"@angular/core\": \"^17.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            @SuppressWarnings("unchecked")
            Map<String, Object> jsInfo = (Map<String, Object>) projectInfo.get("javascript");
            assertThat(jsInfo.get("framework")).isEqualTo("Angular");
            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("serve")).isEqualTo("npm run serve");
            assertThat(commands.get("generate")).isEqualTo("npx ng generate");
        }
    }

    @Test
    void addFrameworkCommands_addsAngularCommandsWithYarnGenerateForYarn() throws IOException {
        String yarnDir = createTempDirWithFile("yarn.lock");
        Map<String, Object> projectInfo = createJsProjectInfo();
        when(mockBaseDir.findChild("package.json")).thenReturn(mockPackageJson);
        when(mockPackageJson.getParent()).thenReturn(mockPackageJsonParent);
        when(mockPackageJsonParent.getPath()).thenReturn(yarnDir);

        try (MockedStatic<ProjectUtil> projectUtilMock = mockStatic(ProjectUtil.class);
             MockedStatic<VfsUtilCore> vfsUtilCoreMock = mockStatic(VfsUtilCore.class)) {

            projectUtilMock.when(() -> ProjectUtil.guessProjectDir(mockProject)).thenReturn(mockBaseDir);
            vfsUtilCoreMock.when(() -> VfsUtilCore.loadText(any(VirtualFile.class)))
                    .thenReturn("{\"dependencies\": {\"@angular/core\": \"^17.0.0\"}}");

            extension.enhanceProjectInfo(mockProject, projectInfo);

            Map<String, String> commands = getCommands(projectInfo);
            assertThat(commands.get("generate")).isEqualTo("yarn ng generate");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, String> getCommands(Map<String, Object> projectInfo) {
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        assertThat(buildSystem).as("buildSystem should not be null").isNotNull();
        return (Map<String, String>) buildSystem.get("commands");
    }

    private String createTempDirWithFile(String filename) throws IOException {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("js-test-");
        java.nio.file.Files.createFile(dir.resolve(filename));
        dir.toFile().deleteOnExit();
        return dir.toString();
    }

    private String createEmptyTempDir() throws IOException {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("js-test-");
        dir.toFile().deleteOnExit();
        return dir.toString();
    }
}
