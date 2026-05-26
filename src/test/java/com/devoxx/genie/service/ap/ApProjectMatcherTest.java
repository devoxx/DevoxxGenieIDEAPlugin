package com.devoxx.genie.service.ap;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApProjectMatcherTest {

    @Mock
    private Project project;

    @Test
    void candidateNames_ideNameAndDistinctBasePath_bothReturned() {
        when(project.getName()).thenReturn("MyProject");
        when(project.getBasePath()).thenReturn("/Users/dev/workspace/my-project");

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("MyProject", "my-project");
    }

    @Test
    void candidateNames_ideNameEqualsDirName_deduped() {
        when(project.getName()).thenReturn("my-project");
        when(project.getBasePath()).thenReturn("/Users/dev/workspace/my-project");

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("my-project");
    }

    @Test
    void candidateNames_ideNameMatchesDirNameCaseInsensitive_deduped() {
        when(project.getName()).thenReturn("MyProject");
        when(project.getBasePath()).thenReturn("/Users/dev/workspace/myproject");

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("MyProject");
    }

    @Test
    void candidateNames_nullIdeName_onlyDirName() {
        when(project.getName()).thenReturn(null);
        when(project.getBasePath()).thenReturn("/Users/dev/workspace/my-project");

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("my-project");
    }

    @Test
    void candidateNames_blankIdeName_onlyDirName() {
        when(project.getName()).thenReturn("   ");
        when(project.getBasePath()).thenReturn("/Users/dev/workspace/my-project");

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("my-project");
    }

    @Test
    void candidateNames_nullBasePath_onlyIdeName() {
        when(project.getName()).thenReturn("MyProject");
        when(project.getBasePath()).thenReturn(null);

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("MyProject");
    }

    @Test
    void candidateNames_blankBasePath_onlyIdeName() {
        when(project.getName()).thenReturn("MyProject");
        when(project.getBasePath()).thenReturn("");

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("MyProject");
    }

    @Test
    void candidateNames_bothNullOrBlank_emptyList() {
        when(project.getName()).thenReturn(null);
        when(project.getBasePath()).thenReturn(null);

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).isEmpty();
    }

    @Test
    void candidateNames_basePathWithTrailingPathSeparator_dirNameStillExtracted() {
        when(project.getName()).thenReturn("Acme");
        when(project.getBasePath()).thenReturn("/Users/dev/acme-checkout");

        List<String> result = ApProjectMatcher.candidateNames(project);

        assertThat(result).containsExactly("Acme", "acme-checkout");
    }
}
