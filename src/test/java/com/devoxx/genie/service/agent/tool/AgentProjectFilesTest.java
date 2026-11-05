package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentProjectFilesTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().resolve("shop");

    @Mock
    private VirtualFile root;

    @BeforeEach
    void setUp() {
        when(root.getPath()).thenReturn(systemIndependent(PROJECT_ROOT));
    }

    @Test
    void absolutePathInsideTheProjectBecomesRelativeToTheRoot() {
        var absolute = systemIndependent(PROJECT_ROOT.resolve("src/billing/java/com/shop/Invoice.java"));

        assertThat(AgentProjectFiles.relativeToRoot(root, absolute)).isEqualTo("src/billing/java/com/shop/Invoice.java");
    }

    @Test
    void relativePathIsLeftUnchanged() {
        assertThat(AgentProjectFiles.relativeToRoot(root, "src/billing/java/com/shop/Invoice.java"))
                .isEqualTo("src/billing/java/com/shop/Invoice.java");
    }

    @Test
    void absolutePathOutsideTheProjectIsLeftUnchanged() {
        var outside = systemIndependent(PROJECT_ROOT.resolveSibling("other").resolve("secret.txt"));

        assertThat(AgentProjectFiles.relativeToRoot(root, outside)).isEqualTo(outside);
    }

    @Test
    void siblingFolderSharingTheRootNamePrefixIsLeftUnchanged() {
        var sibling = systemIndependent(PROJECT_ROOT.resolveSibling("shop-other").resolve("secret.txt"));

        assertThat(AgentProjectFiles.relativeToRoot(root, sibling)).isEqualTo(sibling);
    }

    @Test
    void absolutePathEscapingTheRootWithParentSegmentsIsLeftUnchanged() {
        var escaping = systemIndependent(PROJECT_ROOT) + "/src/../../other/secret.txt";

        assertThat(AgentProjectFiles.relativeToRoot(root, escaping)).isEqualTo(escaping);
    }

    @Test
    void rootFallsBackToTheGuessedProjectDirWhenThereIsNoBasePath(@Mock Project project, @Mock VirtualFile guessed) {
        when(project.getBasePath()).thenReturn(null);

        try (MockedStatic<ProjectUtil> projectUtilStatic = mockStatic(ProjectUtil.class)) {
            projectUtilStatic.when(() -> ProjectUtil.guessProjectDir(project)).thenReturn(guessed);

            assertThat(AgentProjectFiles.root(project)).isSameAs(guessed);
        }
    }

    private static String systemIndependent(Path path) {
        return FileUtil.toSystemIndependentName(path.toString());
    }
}
