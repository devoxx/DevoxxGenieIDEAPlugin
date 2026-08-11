package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.tool.AgentDiffPreviewFactory.DiffPreview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentDiffPreviewFactoryTest {

    @Mock
    private Project project;

    // --- edit_file ---

    @Test
    void editFile_singleOccurrence_previewsReplacedContent() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("hello world\nsecond line\n");

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"src/Test.java\", \"old_string\": \"hello\", \"new_string\": \"goodbye\"}");

        assertThat(preview).isPresent();
        assertThat(preview.get().path()).isEqualTo("src/Test.java");
        assertThat(preview.get().before()).isEqualTo("hello world\nsecond line\n");
        assertThat(preview.get().after()).isEqualTo("goodbye world\nsecond line\n");
    }

    @Test
    void editFile_multipleOccurrencesWithoutReplaceAll_isEmpty() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("foo\nfoo\n");

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"a.txt\", \"old_string\": \"foo\", \"new_string\": \"bar\"}");

        assertThat(preview).isEmpty();
    }

    @Test
    void editFile_multipleOccurrencesWithReplaceAll_previewsAllReplacements() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("foo\nfoo\n");

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"a.txt\", \"old_string\": \"foo\", \"new_string\": \"bar\", \"replace_all\": true}");

        assertThat(preview).isPresent();
        assertThat(preview.get().after()).isEqualTo("bar\nbar\n");
    }

    @Test
    void editFile_oldStringNotFound_isEmpty() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("hello world");

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"a.txt\", \"old_string\": \"missing\", \"new_string\": \"bar\"}");

        assertThat(preview).isEmpty();
    }

    @Test
    void editFile_crlfFile_bothSidesNormalizedSoOnlyEditedLineDiffers() throws IOException {
        // The executor matches in LF space and restores CRLF on write. If the preview compared
        // raw content against normalized content, every line would show up as changed.
        AgentDiffPreviewFactory factory = factoryFor("one\r\ntwo\r\nthree\r\n");

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"a.txt\", \"old_string\": \"two\", \"new_string\": \"2\"}");

        assertThat(preview).isPresent();
        assertThat(preview.get().before()).isEqualTo("one\ntwo\nthree\n");
        assertThat(preview.get().after()).isEqualTo("one\n2\nthree\n");
    }

    @Test
    void editFile_multiLineOldStringWithLfAgainstCrlfFile_matches() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("class A {\r\n    int x;\r\n}\r\n");

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"A.java\", \"old_string\": \"    int x;\\n}\", \"new_string\": \"    int y;\\n}\"}");

        assertThat(preview).isPresent();
        assertThat(preview.get().after()).isEqualTo("class A {\n    int y;\n}\n");
    }

    @Test
    void editFile_missingArguments_isEmpty() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("content");

        assertThat(factory.create("edit_file", "{\"old_string\": \"a\", \"new_string\": \"b\"}")).isEmpty();
        assertThat(factory.create("edit_file", "{\"path\": \"a.txt\", \"new_string\": \"b\"}")).isEmpty();
        assertThat(factory.create("edit_file", "{\"path\": \"a.txt\", \"old_string\": \"a\"}")).isEmpty();
        assertThat(factory.create("edit_file", "not json")).isEmpty();
    }

    @Test
    void editFile_pathTraversal_isEmpty() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("content");

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"../../etc/passwd\", \"old_string\": \"a\", \"new_string\": \"b\"}");

        assertThat(preview).isEmpty();
    }

    @Test
    void editFile_fileNotFound_isEmpty() {
        AgentDiffPreviewFactory factory = testableFactory(mock(VirtualFile.class), null, true);

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"gone.txt\", \"old_string\": \"a\", \"new_string\": \"b\"}");

        assertThat(preview).isEmpty();
    }

    @Test
    void editFile_fileOutsideProject_isEmpty() throws IOException {
        AgentDiffPreviewFactory factory =
                testableFactory(mock(VirtualFile.class), fileWithContent("secret"), false);

        Optional<DiffPreview> preview = factory.create("edit_file",
                "{\"path\": \"outside.txt\", \"old_string\": \"secret\", \"new_string\": \"leaked\"}");

        assertThat(preview).isEmpty();
    }

    // --- write_file ---

    @Test
    void writeFile_existingFile_previewsCurrentVersusNewContent() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("old content\n");

        Optional<DiffPreview> preview = factory.create("write_file",
                "{\"path\": \"a.txt\", \"content\": \"new content\\n\"}");

        assertThat(preview).isPresent();
        assertThat(preview.get().before()).isEqualTo("old content\n");
        assertThat(preview.get().after()).isEqualTo("new content\n");
    }

    @Test
    void writeFile_newFile_previewsAgainstEmptySide() {
        AgentDiffPreviewFactory factory = testableFactory(mock(VirtualFile.class), null, true);

        Optional<DiffPreview> preview = factory.create("write_file",
                "{\"path\": \"New.java\", \"content\": \"class New {}\"}");

        assertThat(preview).isPresent();
        assertThat(preview.get().before()).isEmpty();
        assertThat(preview.get().after()).isEqualTo("class New {}");
    }

    @Test
    void writeFile_missingContent_isEmpty() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("old");

        assertThat(factory.create("write_file", "{\"path\": \"a.txt\"}")).isEmpty();
    }

    // --- other tools ---

    @Test
    void nonFileTools_areNotPreviewable() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("content");

        assertThat(factory.create("run_command", "{\"command\": \"rm -rf /\"}")).isEmpty();
        assertThat(factory.create("read_file", "{\"path\": \"a.txt\"}")).isEmpty();
        assertThat(factory.create("some_mcp_tool", "{\"foo\": \"bar\"}")).isEmpty();
    }

    @Test
    void nullArguments_areNotPreviewable() throws IOException {
        AgentDiffPreviewFactory factory = factoryFor("content");

        assertThat(factory.create("edit_file", null)).isEmpty();
    }

    // --- helpers ---

    private AgentDiffPreviewFactory factoryFor(String fileContent) throws IOException {
        return testableFactory(mock(VirtualFile.class), fileWithContent(fileContent), true);
    }

    private VirtualFile fileWithContent(String content) throws IOException {
        VirtualFile file = mock(VirtualFile.class);
        when(file.exists()).thenReturn(true);
        when(file.isDirectory()).thenReturn(false);
        when(file.contentsToByteArray()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private AgentDiffPreviewFactory testableFactory(VirtualFile projectBase,
                                                    VirtualFile file,
                                                    boolean insideProject) {
        return new AgentDiffPreviewFactory(project) {
            @Override
            VirtualFile getProjectBaseDir() {
                return projectBase;
            }

            @Override
            VirtualFile findFile(VirtualFile base, String path) {
                return file;
            }

            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return insideProject;
            }
        };
    }
}
