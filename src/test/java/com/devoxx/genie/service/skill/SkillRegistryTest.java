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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * {@code disabledSkillNames} set, and redirects every user-level skill directory at
 * temp dirs so collisions between simultaneously-loaded skills can be exercised.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkillRegistryTest {

    @Mock
    private Project project;

    @TempDir
    Path projectDir;

    /** Stand-in for the user home; per-tool subdirectories live below it. */
    @TempDir
    Path userHome;

    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private DevoxxGenieStateService state;

    /** Path overrides for the three user-level tool directories. */
    private Path userAgents;
    private Path userClaude;
    private Path userDevoxxgenie;

    /** Path of the corresponding project-level directories (resolved against {@link #projectDir}). */
    private Path projectAgents;
    private Path projectClaude;
    private Path projectDevoxxgenie;

    @BeforeEach
    void setUp() {
        state = new DevoxxGenieStateService();
        state.setDisabledSkillNames(new HashSet<>());
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);

        when(project.getBasePath()).thenReturn(projectDir.toString());
        lenient().when(project.isDefault()).thenReturn(false);

        userAgents = userHome.resolve(".agents/skills");
        userClaude = userHome.resolve(".claude/skills");
        userDevoxxgenie = userHome.resolve(".devoxxgenie/skills");

        projectAgents = projectDir.resolve(".agents/skills");
        projectClaude = projectDir.resolve(".claude/skills");
        projectDevoxxgenie = projectDir.resolve(".devoxxgenie/skills");
    }

    @AfterEach
    void tearDown() {
        if (stateStatic != null) {
            stateStatic.close();
        }
    }

    /** Builds a registry whose user-level skill directories are redirected at the temp home. */
    private SkillRegistry newRegistry() {
        Map<SkillRegistry.Source.Tool, Path> overrides = new EnumMap<>(SkillRegistry.Source.Tool.class);
        overrides.put(SkillRegistry.Source.Tool.AGENTS, userAgents);
        overrides.put(SkillRegistry.Source.Tool.CLAUDE, userClaude);
        overrides.put(SkillRegistry.Source.Tool.DEVOXXGENIE, userDevoxxgenie);
        return new SkillRegistry(project, overrides);
    }

    // --- empty / missing dirs ---------------------------------------------

    @Test
    void emptyDirectoriesProduceNoSkills() {
        SkillRegistry registry = newRegistry();

        assertThat(registry.getAllSkills()).isEmpty();
        assertThat(registry.buildSkills()).isNull();
        assertThat(registry.getSystemPromptFragment()).isEmpty();
    }

    @Test
    void nonExistentDirectoriesAreHandledGracefully() {
        // No directories are ever created. Should not throw.
        SkillRegistry registry = newRegistry();

        assertThat(registry.getAllSkills()).isEmpty();
        assertThat(registry.buildSkills()).isNull();
        assertThat(registry.getSystemPromptFragment()).isEmpty();
    }

    // --- per-source loading -----------------------------------------------

    @Test
    void loadsSkillFromUserAgentsOnly() throws IOException {
        writeSkill(userAgents, "agents-only", "loaded from ~/.agents/skills");

        SkillRegistry registry = newRegistry();

        List<SkillRegistry.SkillEntry> all = registry.getAllSkills();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).name()).isEqualTo("agents-only");
        assertThat(all.get(0).source()).isEqualTo(SkillRegistry.Source.USER_AGENTS);
        assertThat(all.get(0).source().label()).isEqualTo("user (.agents)");
    }

    @Test
    void loadsSkillFromUserClaudeOnly() throws IOException {
        writeSkill(userClaude, "claude-only", "loaded from ~/.claude/skills");

        SkillRegistry registry = newRegistry();

        List<SkillRegistry.SkillEntry> all = registry.getAllSkills();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).name()).isEqualTo("claude-only");
        assertThat(all.get(0).source()).isEqualTo(SkillRegistry.Source.USER_CLAUDE);
    }

    @Test
    void loadsSkillsFromEverySourceWithCorrectLabels() throws IOException {
        writeSkill(userAgents, "from-user-agents", "user .agents");
        writeSkill(userClaude, "from-user-claude", "user .claude");
        writeSkill(userDevoxxgenie, "from-user-devoxxgenie", "user .devoxxgenie");
        writeSkill(projectAgents, "from-project-agents", "project .agents");
        writeSkill(projectClaude, "from-project-claude", "project .claude");
        writeSkill(projectDevoxxgenie, "from-project-devoxxgenie", "project .devoxxgenie");

        SkillRegistry registry = newRegistry();

        assertThat(registry.getAllSkills())
                .extracting(e -> e.source().label())
                .containsExactlyInAnyOrder(
                        "user (.agents)",
                        "user (.claude)",
                        "user (.devoxxgenie)",
                        "project (.agents)",
                        "project (.claude)",
                        "project (.devoxxgenie)");
    }

    // --- collision / priority resolution ----------------------------------

    @Test
    void projectAgentsBeatsUserClaudeOnNameCollision() throws IOException {
        writeSkill(userClaude, "shared", "user .claude version");
        writeSkill(projectAgents, "shared", "project .agents version");

        SkillRegistry registry = newRegistry();

        assertThat(registry.getAllSkills()).hasSize(1);
        SkillRegistry.SkillEntry kept = registry.getAllSkills().get(0);
        assertThat(kept.name()).isEqualTo("shared");
        assertThat(kept.source()).isEqualTo(SkillRegistry.Source.PROJECT_AGENTS);
        assertThat(kept.description()).isEqualTo("project .agents version");
    }

    @Test
    void projectDevoxxgenieWinsAcrossAllSixSourcesOnNameCollision() throws IOException {
        // Same skill name in all six directories.
        writeSkill(userAgents, "everywhere", "ua");
        writeSkill(userClaude, "everywhere", "uc");
        writeSkill(userDevoxxgenie, "everywhere", "ud");
        writeSkill(projectAgents, "everywhere", "pa");
        writeSkill(projectClaude, "everywhere", "pc");
        writeSkill(projectDevoxxgenie, "everywhere", "pd");

        SkillRegistry registry = newRegistry();

        List<SkillRegistry.SkillEntry> all = registry.getAllSkills();
        assertThat(all).hasSize(1);
        SkillRegistry.SkillEntry kept = all.get(0);
        assertThat(kept.name()).isEqualTo("everywhere");
        assertThat(kept.source())
                .as("project .devoxxgenie should win against every other source")
                .isEqualTo(SkillRegistry.Source.PROJECT_DEVOXXGENIE);
        assertThat(kept.description()).isEqualTo("pd");
    }

    @Test
    void priorityOrderIsAgentsLowestToDevoxxgenieHighestWithinEachScope() throws IOException {
        // Within the user scope: .agents < .claude < .devoxxgenie
        writeSkill(userAgents, "user-only", "u-agents");
        writeSkill(userClaude, "user-only", "u-claude");
        writeSkill(userDevoxxgenie, "user-only", "u-devoxxgenie");

        SkillRegistry registry = newRegistry();

        assertThat(registry.getAllSkills())
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.source()).isEqualTo(SkillRegistry.Source.USER_DEVOXXGENIE);
                    assertThat(e.description()).isEqualTo("u-devoxxgenie");
                });
    }

    @Test
    void projectScopeAlwaysBeatsUserScopeOnCollision() throws IOException {
        // User-side has all three tools, project-side only .agents.
        writeSkill(userAgents, "x", "u-agents");
        writeSkill(userClaude, "x", "u-claude");
        writeSkill(userDevoxxgenie, "x", "u-devoxxgenie");
        writeSkill(projectAgents, "x", "p-agents");

        SkillRegistry registry = newRegistry();

        assertThat(registry.getAllSkills())
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.source()).isEqualTo(SkillRegistry.Source.PROJECT_AGENTS);
                    assertThat(e.description()).isEqualTo("p-agents");
                });
    }

    // --- disabled-set filtering -------------------------------------------

    @Test
    void disabledSkillsAreFilteredFromBuildSkillsAndFragment() throws IOException {
        writeSkill(userDevoxxgenie, "enabled-skill", "Should appear");
        writeSkill(userClaude, "disabled-skill", "Should be filtered out");

        state.setDisabledSkillNames(new HashSet<>(Set.of("disabled-skill")));

        SkillRegistry registry = newRegistry();

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
        writeSkill(userDevoxxgenie, "alpha", "first");
        writeSkill(userAgents, "beta", "second");

        state.setDisabledSkillNames(new HashSet<>(Set.of("alpha", "beta")));

        SkillRegistry registry = newRegistry();

        assertThat(registry.buildSkills()).isNull();
        assertThat(registry.getSystemPromptFragment()).isEmpty();
    }

    // --- reload semantics -------------------------------------------------

    @Test
    void reloadPicksUpNewlyAddedSkill() throws IOException {
        Files.createDirectories(userDevoxxgenie);

        SkillRegistry registry = newRegistry();
        assertThat(registry.getAllSkills()).isEmpty();

        writeSkill(userDevoxxgenie, "added-later", "Added after first scan");

        registry.reloadBlocking();
        assertThat(registry.getAllSkills()).hasSize(1);
        assertThat(registry.getAllSkills().get(0).name()).isEqualTo("added-later");
    }

    @Test
    void skillDirectoryWithoutSkillMdIsIgnored() throws IOException {
        Files.createDirectories(userDevoxxgenie.resolve("not-a-skill"));
        Files.writeString(userDevoxxgenie.resolve("not-a-skill/README.md"), "no SKILL.md here");

        // Still a real skill alongside.
        writeSkill(userDevoxxgenie, "real-skill", "valid skill");

        SkillRegistry registry = newRegistry();

        assertThat(registry.getAllSkills()).hasSize(1);
        assertThat(registry.getAllSkills().get(0).name()).isEqualTo("real-skill");
    }

    // --- caching ----------------------------------------------------------

    @Test
    void buildSkillsResultIsCachedAcrossCalls() throws IOException {
        writeSkill(userDevoxxgenie, "cached-skill", "only-built-once");

        SkillRegistry registry = newRegistry();

        Skills first = registry.buildSkills();
        Skills second = registry.buildSkills();
        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);

        String fragment1 = registry.getSystemPromptFragment();
        String fragment2 = registry.getSystemPromptFragment();
        assertThat(fragment1).isNotEmpty();
        assertThat(fragment2).isEqualTo(fragment1);
    }

    @Test
    void invalidateDerivedCachesForcesRebuildOfSkillsButKeepsDiskCache() throws IOException {
        writeSkill(userDevoxxgenie, "reusable", "reusable skill");

        SkillRegistry registry = newRegistry();

        Skills before = registry.buildSkills();
        registry.invalidateDerivedCaches();
        Skills after = registry.buildSkills();

        assertThat(before).isNotNull();
        assertThat(after).isNotNull();
        assertThat(after).isNotSameAs(before);
        // The detected skills list is unaffected (we did not re-scan disk).
        assertThat(registry.peekAllSkills()).hasSize(1);
    }

    // --- async / threading ------------------------------------------------

    @Test
    void reloadAsyncRunsOffEdtAndInvokesCallback() throws Exception {
        writeSkill(userClaude, "async-skill", "loaded asynchronously");

        SkillRegistry registry = newRegistry();

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
        SkillRegistry registry = newRegistry();
        // No ensureLoaded() or reload yet; peek should return whatever is currently cached.
        assertThat(registry.peekAllSkills()).isEmpty();
    }

    // --- directory accessors ---------------------------------------------

    @Test
    void directoryAccessorsReturnTheExpectedPaths() {
        SkillRegistry registry = newRegistry();

        assertThat(registry.directoryFor(SkillRegistry.Source.USER_AGENTS)).isEqualTo(userAgents);
        assertThat(registry.directoryFor(SkillRegistry.Source.USER_CLAUDE)).isEqualTo(userClaude);
        assertThat(registry.directoryFor(SkillRegistry.Source.USER_DEVOXXGENIE)).isEqualTo(userDevoxxgenie);
        assertThat(registry.directoryFor(SkillRegistry.Source.PROJECT_AGENTS)).isEqualTo(projectAgents);
        assertThat(registry.directoryFor(SkillRegistry.Source.PROJECT_CLAUDE)).isEqualTo(projectClaude);
        assertThat(registry.directoryFor(SkillRegistry.Source.PROJECT_DEVOXXGENIE)).isEqualTo(projectDevoxxgenie);
    }

    @Test
    void ensureDirectoryExistsCreatesTheRequestedFolder() {
        SkillRegistry registry = newRegistry();

        Path created = registry.ensureDirectoryExists(SkillRegistry.Source.USER_AGENTS);

        assertThat(created).isEqualTo(userAgents);
        assertThat(Files.isDirectory(userAgents)).isTrue();
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
