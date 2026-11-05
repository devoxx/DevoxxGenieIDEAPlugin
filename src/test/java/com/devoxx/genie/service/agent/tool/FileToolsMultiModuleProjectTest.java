package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.tool.psi.PsiToolUtils;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.EmptyModuleType;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.BaseProjectDirectories;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FileToolsMultiModuleProjectTest {

    private static final String CLAZZ2 = "src/module2/java/com/example/Clazz2.java";

    private IdeaProjectTestFixture fixture;

    private Project project;

    private Path openedFolder;

    private VirtualFile module1;

    private VirtualFile module2;

    @BeforeEach
    void openAProjectWithTwoModuleRootsAndNoRootModule() throws Throwable {
        this.fixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getClass().getSimpleName()).getFixture();
        EdtTestUtil.runInEdtAndWait(this.fixture::setUp);
        this.project = this.fixture.getProject();
        this.openedFolder = Path.of(Objects.requireNonNull(this.project.getBasePath()));
        writeSource("src/module1/java/com/example/Clazz1.java", "class Clazz1 {}");
        writeSource(CLAZZ2, "class Clazz2 {}");
        this.module1 = addModule("module1");
        this.module2 = addModule("module2");
        awaitModuleRootsAsBaseDirectories();
    }

    @AfterEach
    void closeTheProject() throws Throwable {
        EdtTestUtil.runInEdtAndWait(this.fixture::tearDown);
    }

    @Test
    void intellijGuessesAModuleRootInsteadOfTheOpenedFolder() {
        assertThat(ProjectUtil.guessProjectDir(this.project)).isIn(this.module1, this.module2);
    }

    @Test
    void everyFileToolAndTheSemanticToolsUseTheOpenedFolderAsTheirRoot() {
        var opened = LocalFileSystem.getInstance().findFileByNioFile(this.openedFolder);

        assertThat(Arrays.asList(
                new ReadFileToolExecutor(this.project).getProjectBaseDir(),
                new ListFilesToolExecutor(this.project).getProjectBaseDir(),
                new SearchFilesToolExecutor(this.project).getProjectBaseDir(),
                new WriteFileToolExecutor(this.project).getProjectBaseDir(),
                new EditFileToolExecutor(this.project).getProjectBaseDir(),
                new AgentDiffPreviewFactory(this.project).getProjectBaseDir(),
                PsiToolUtils.getProjectBase(this.project)))
                .containsOnly(opened);
    }

    @Test
    void aLocationPrintedBySemanticToolsCanBeReadBackByReadFile() {
        var clazz2 = Objects.requireNonNull(LocalFileSystem.getInstance().findFileByNioFile(this.openedFolder.resolve(CLAZZ2)));
        var location = ReadAction.compute(() -> PsiToolUtils.formatLocation(
                Objects.requireNonNull(PsiManager.getInstance(this.project).findFile(clazz2)), PsiToolUtils.getProjectBase(this.project)));
        var path = location.substring(0, location.lastIndexOf(':'));

        assertThat(path).isEqualTo(CLAZZ2);
        assertThat(new ReadFileToolExecutor(this.project).execute(request("read_file", "{\"path\":\"" + path + "\"}"), null))
                .isEqualTo("class Clazz2 {}");
    }

    @Test
    void readFileReadsAFileOfAnotherModuleByItsProjectRelativePath() {
        assertThat(new ReadFileToolExecutor(this.project).execute(request("read_file", "{\"path\":\"" + CLAZZ2 + "\"}"), null))
                .isEqualTo("class Clazz2 {}");
    }

    @Test
    void readFileReadsAFileOfAnotherModuleByItsAbsolutePath() {
        var absolute = FileUtil.toSystemIndependentName(this.openedFolder.resolve(CLAZZ2).toString());

        assertThat(new ReadFileToolExecutor(this.project).execute(request("read_file", "{\"path\":\"" + absolute + "\"}"), null))
                .isEqualTo("class Clazz2 {}");
    }

    @Test
    void listFilesShowsEveryModule() {
        assertThat(new ListFilesToolExecutor(this.project).execute(request("list_files", "{\"path\":\"src\"}"), null))
                .contains("module1")
                .contains("module2");
    }

    @Test
    void searchFilesFindsMatchesInEveryModule() {
        assertThat(new SearchFilesToolExecutor(this.project).execute(request("search_files", "{\"pattern\":\"class Clazz\"}"), null))
                .contains("Clazz1.java")
                .contains("Clazz2.java");
    }

    @Test
    void writeFileCreatesTheFileInsideTheModuleItNames() {
        var added = "src/module2/java/com/example/Added.java";

        var result = new WriteFileToolExecutor(this.project).execute(
                request("write_file", "{\"path\":\"" + added + "\",\"content\":\"class Added {}\"}"), null);

        assertThat(result).startsWith("Successfully wrote");
        assertThat(this.openedFolder.resolve(added)).hasContent("class Added {}");
    }

    @Test
    void editFileEditsAFileOfAnotherModule() {
        var result = new EditFileToolExecutor(this.project).execute(request("edit_file",
                "{\"path\":\"" + CLAZZ2 + "\",\"old_string\":\"Clazz2 {}\",\"new_string\":\"Clazz2 { int fixed; }\"}"), null);

        assertThat(result).doesNotStartWith("Error");
        assertThat(this.openedFolder.resolve(CLAZZ2)).hasContent("class Clazz2 { int fixed; }");
    }

    private void writeSource(String relativePath, String content) throws IOException {
        var file = this.openedFolder.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private VirtualFile addModule(String name) {
        var root = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(this.openedFolder.resolve("src").resolve(name)));
        var module = WriteAction.computeAndWait(() -> ModuleManager.getInstance(this.project)
                .newModule(this.openedFolder.resolve(name + ".iml"), EmptyModuleType.EMPTY_MODULE));
        ModuleRootModificationUtil.addContentRoot(module, root);
        return root;
    }

    private void awaitModuleRootsAsBaseDirectories() throws InterruptedException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!BaseProjectDirectories.getBaseDirectories(this.project).containsAll(List.of(this.module1, this.module2))) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("IntelliJ did not register the module roots as base directories within 10 seconds");
            }
            Thread.sleep(10);
        }
    }

    private static ToolExecutionRequest request(String toolName, String arguments) {
        return ToolExecutionRequest.builder().name(toolName).arguments(arguments).build();
    }
}
