package com.devoxx.genie.service.skill;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.skills.Skills;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SkillRegistry}.
 *
 * <p>Each test wires a fresh {@link DevoxxGenieStateService} into the static
 * {@code getInstance()} accessor so the registry sees a deterministic
 * {@code disabledSkillNames} set.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkillRegistryTest {

    @Mock
    private Project project;

    @TempDir
    Path projectDir;

    @TempDir
    Path userHome;

    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private DevoxxGenieStateService state;

    @BeforeEach
    void setUp() {
        state = new DevoxxGenieStateService();
        state.setDisabledSkillNames(new HashSet<>());
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);

        when(project.getBasePath()).thenReturn(projectDir.toString());
        lenient().when(project.isDefault()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (stateStatic != null) {
            stateStatic.close();
        }
    }

    @Test
    void emptyDirectoriesProduceNoSkills() {
        SkillRegistry registry = new SkillRegistry(project, userHome.resolve(".devoxxgenie/skills"));

        assertThat(registry.getAllSkills()).isEmpty();
        assertThat(registry.buildSkills()).isNull();
        assertThat(registry.getSystemPromptFragment()).isEmpty();
    }

    @Test
    void nonExistentDirectoriesAreHandledGracefully() {
        // Override paths point to never-created directories. Should not throw.
        Path neverCreated = userHome.resolve("never-here/skills");

        SkillRegistry registry = new SkillRegistry(project, neverCreated);

        assertThat(registry.getAllSkills()).isEmpty();
        assertThat(registry.buildSkills()).isNull();
        assertThat(registry.getSystemPromptFragment()).isEmpty();
    }

    @Test
    void loadsSkillsFromUserAndProjectDirectories() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        writeSkill(userSkills, "user-only", "User-only skill");

        Path projectSkills = projectDir.resolve(".devoxxgenie/skills");
        writeSkill(projectSkills, "project-only", "Project-only skill");

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        List<SkillRegistry.SkillEntry> all = registry.getAllSkills();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(SkillRegistry.SkillEntry::name)
                .containsExactlyInAnyOrder("user-only", "project-only");

        SkillRegistry.SkillEntry user = findByName(all, "user-only");
        SkillRegistry.SkillEntry proj = findByName(all, "project-only");
        assertThat(user.source()).isEqualTo(SkillRegistry.Source.USER);
        assertThat(proj.source()).isEqualTo(SkillRegistry.Source.PROJECT);
    }

    @Test
    void projectSkillOverridesUserSkillOnNameCollision() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        writeSkill(userSkills, "shared", "User version");

        Path projectSkills = projectDir.resolve(".devoxxgenie/skills");
        writeSkill(projectSkills, "shared", "Project version");

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        List<SkillRegistry.SkillEntry> all = registry.getAllSkills();
        assertThat(all).hasSize(1);
        SkillRegistry.SkillEntry kept = all.get(0);
        assertThat(kept.name()).isEqualTo("shared");
        assertThat(kept.source()).isEqualTo(SkillRegistry.Source.PROJECT);
        assertThat(kept.description()).isEqualTo("Project version");
    }

    @Test
    void disabledSkillsAreFilteredFromBuildSkillsAndFragment() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        writeSkill(userSkills, "enabled-skill", "Should appear");
        writeSkill(userSkills, "disabled-skill", "Should be filtered out");

        state.setDisabledSkillNames(new HashSet<>(Set.of("disabled-skill")));

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        // Both still discovered for the settings UI.
        assertThat(registry.getAllSkills()).hasSize(2);
        SkillRegistry.SkillEntry disabledEntry = findByName(registry.getAllSkills(), "disabled-skill");
        assertThat(disabledEntry.disabled()).isTrue();
        SkillRegistry.SkillEntry enabledEntry = findByName(registry.getAllSkills(), "enabled-skill");
        assertThat(enabledEntry.disabled()).isFalse();

        // Only the enabled one reaches Skills/the LLM.
        Skills skills = registry.buildSkills();
        assertThat(skills).isNotNull();
        String fragment = registry.getSystemPromptFragment();
        assertThat(fragment).contains("enabled-skill");
        assertThat(fragment).doesNotContain("disabled-skill");
    }

    @Test
    void buildSkillsReturnsNullWhenAllSkillsDisabled() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        writeSkill(userSkills, "alpha", "first");
        writeSkill(userSkills, "beta", "second");

        state.setDisabledSkillNames(new HashSet<>(Set.of("alpha", "beta")));

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        assertThat(registry.buildSkills()).isNull();
        assertThat(registry.getSystemPromptFragment()).isEmpty();
    }

    @Test
    void reloadPicksUpNewlyAddedSkill() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        Files.createDirectories(userSkills);

        SkillRegistry registry = new SkillRegistry(project, userSkills);
        assertThat(registry.getAllSkills()).isEmpty();

        writeSkill(userSkills, "added-later", "Added after first scan");

        registry.reloadBlocking();
        assertThat(registry.getAllSkills()).hasSize(1);
        assertThat(registry.getAllSkills().get(0).name()).isEqualTo("added-later");
    }

    @Test
    void skillDirectoryWithoutSkillMdIsIgnored() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        Files.createDirectories(userSkills.resolve("not-a-skill"));
        Files.writeString(userSkills.resolve("not-a-skill/README.md"), "no SKILL.md here");

        // Still a real skill alongside.
        writeSkill(userSkills, "real-skill", "valid skill");

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        assertThat(registry.getAllSkills()).hasSize(1);
        assertThat(registry.getAllSkills().get(0).name()).isEqualTo("real-skill");
    }

    @Test
    void buildSkillsResultIsCachedAcrossCalls() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        writeSkill(userSkills, "cached-skill", "only-built-once");

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        // Two consecutive lookups must return the exact same cached Skills wrapper.
        Skills first = registry.buildSkills();
        Skills second = registry.buildSkills();
        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);

        // The system-prompt fragment is also cached: it should be the same string returned
        // by Skills.formatAvailableSkills() of that cached instance.
        String fragment1 = registry.getSystemPromptFragment();
        String fragment2 = registry.getSystemPromptFragment();
        assertThat(fragment1).isNotEmpty();
        assertThat(fragment2).isEqualTo(fragment1);
    }

    @Test
    void invalidateDerivedCachesForcesRebuildOfSkillsButKeepsDiskCache() throws IOException {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        writeSkill(userSkills, "reusable", "reusable skill");

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        Skills before = registry.buildSkills();
        registry.invalidateDerivedCaches();
        Skills after = registry.buildSkills();

        assertThat(before).isNotNull();
        assertThat(after).isNotNull();
        assertThat(after).isNotSameAs(before);
        // The detected skills list is unaffected (we did not re-scan disk).
        assertThat(registry.peekAllSkills()).hasSize(1);
    }

    @Test
    void reloadAsyncRunsOffEdtAndInvokesCallback() throws Exception {
        Path userSkills = userHome.resolve(".devoxxgenie/skills");
        writeSkill(userSkills, "async-skill", "loaded asynchronously");

        SkillRegistry registry = new SkillRegistry(project, userSkills);

        CountDownLatch done = new CountDownLatch(1);
        registry.reloadAsync(done::countDown);
        assertThat(done.await(5, TimeUnit.SECONDS))
                .as("reloadAsync callback should have fired within 5s")
                .isTrue();
        assertThat(registry.peekAllSkills())
                .extracting(SkillRegistry.SkillEntry::name)
                .containsExactly("async-skill");
    }

    @Test
    void peekAllSkillsReturnsEmptyBeforeFirstLoad() {
        SkillRegistry registry = new SkillRegistry(project, userHome.resolve(".devoxxgenie/skills"));
        // No ensureLoaded() or reload yet; peek should return whatever is currently cached.
        assertThat(registry.peekAllSkills()).isEmpty();
    }

    // --- helpers ---------------------------------------------------------

    private static void writeSkill(Path baseDir, String name, String description) throws IOException {
        Path skillDir = baseDir.resolve(name);
        Files.createDirectories(skillDir);
        String content = "---\nname: " + name + "\ndescription: " + description + "\n---\n"
                + "\nInstructions for the " + name + " skill.\n";
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }

    private static SkillRegistry.SkillEntry findByName(List<SkillRegistry.SkillEntry> entries, String name) {
        return entries.stream()
                .filter(e -> name.equals(e.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Skill not found: " + name));
    }
}
