package com.devoxx.genie.service.skill;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.Skills;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project-scoped registry of langchain4j {@link Skill}s loaded from disk.
 *
 * <p>Six source directories are scanned (in increasing priority order):</p>
 * <ol>
 *   <li>{@code ~/.agents/skills/}</li>
 *   <li>{@code ~/.claude/skills/}</li>
 *   <li>{@code ~/.devoxxgenie/skills/}</li>
 *   <li>{@code <project.basePath>/.agents/skills/}</li>
 *   <li>{@code <project.basePath>/.claude/skills/}</li>
 *   <li>{@code <project.basePath>/.devoxxgenie/skills/}</li>
 * </ol>
 *
 * <p>Sources are processed lowest-priority first; each higher-priority source overwrites
 * lower-priority entries on name collision (a warning is logged). Skills whose name appears
 * in {@link DevoxxGenieStateService#getDisabledSkillNames()} are filtered out at the point
 * where {@link Skills} is built.</p>
 *
 * <p>The registry wraps the {@code @Experimental} langchain4j-skills API so the rest of the
 * codebase only depends on this class.</p>
 *
 * <h3>Threading</h3>
 * <p>Disk scans must not run on the Event Dispatch Thread (EDT). The registry exposes:</p>
 * <ul>
 *   <li>{@link #reloadAsync(Runnable)} &mdash; schedules a scan on a pooled thread; the optional
 *       callback is dispatched on the EDT when the cache has been updated. Use this from
 *       Swing handlers (e.g. the Settings UI "Reload" button).</li>
 *   <li>{@link #reloadBlocking()} &mdash; synchronous scan suitable for background threads (agent
 *       execution path). Must <strong>not</strong> be called from the EDT.</li>
 * </ul>
 * <p>The constructor does not touch disk.</p>
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class SkillRegistry {

    public static final String DEVOXXGENIE_SKILLS_SUBDIR = ".devoxxgenie/skills";
    public static final String CLAUDE_SKILLS_SUBDIR = ".claude/skills";
    public static final String AGENTS_SKILLS_SUBDIR = ".agents/skills";

    /**
     * Backwards-compatible alias kept for callers that still reference the original constant.
     * Prefer {@link #DEVOXXGENIE_SKILLS_SUBDIR} in new code.
     */
    @Deprecated
    public static final String SKILLS_SUBDIR = DEVOXXGENIE_SKILLS_SUBDIR;

    /**
     * Tool name exposed by the langchain4j-skills {@code Skills.toolProvider()} that the LLM
     * invokes to activate a discovered skill. Tied to the {@code @Experimental} API &mdash; if
     * upstream renames it this constant must be updated in lockstep.
     */
    public static final String ACTIVATE_SKILL_TOOL_NAME = "activate_skill";

    /**
     * Describes the directory a skill was loaded from. Ordered from <strong>lowest</strong> to
     * <strong>highest</strong> priority so {@code Source.values()} can drive the merge loop
     * directly &mdash; each iteration overwrites earlier entries on collision.
     */
    public enum Source {
        USER_AGENTS("user (.agents)",            Scope.USER,    Tool.AGENTS),
        USER_CLAUDE("user (.claude)",            Scope.USER,    Tool.CLAUDE),
        USER_DEVOXXGENIE("user (.devoxxgenie)",  Scope.USER,    Tool.DEVOXXGENIE),
        PROJECT_AGENTS("project (.agents)",      Scope.PROJECT, Tool.AGENTS),
        PROJECT_CLAUDE("project (.claude)",      Scope.PROJECT, Tool.CLAUDE),
        PROJECT_DEVOXXGENIE("project (.devoxxgenie)", Scope.PROJECT, Tool.DEVOXXGENIE);

        public enum Scope { USER, PROJECT }
        public enum Tool {
            AGENTS(AGENTS_SKILLS_SUBDIR),
            CLAUDE(CLAUDE_SKILLS_SUBDIR),
            DEVOXXGENIE(DEVOXXGENIE_SKILLS_SUBDIR);

            private final String subdir;
            Tool(String subdir) { this.subdir = subdir; }
            public String subdir() { return subdir; }
        }

        private final String label;
        private final Scope scope;
        private final Tool tool;

        Source(String label, Scope scope, Tool tool) {
            this.label = label;
            this.scope = scope;
            this.tool = tool;
        }

        /** Display string shown in the Skills settings table. */
        public String label() { return label; }

        public Scope scope() { return scope; }
        public Tool tool() { return tool; }
    }

    /** Skill plus metadata for the settings UI. */
    public record SkillEntry(@NotNull Skill skill, @NotNull Source source) {
        public String name() {
            return skill.name();
        }
        public String description() {
            return skill.description();
        }

        /**
         * @return {@code true} when the user has disabled this skill in the current settings
         *         state. Re-read on each call so the value tracks the user toggling the
         *         checkbox.
         */
        public boolean disabled() {
            Set<String> names = DevoxxGenieStateService.getInstance().getDisabledSkillNames();
            return names != null && names.contains(name());
        }
    }

    private final Project project;
    /**
     * Test-only overrides for the {@code ~/.<tool>/skills/} directories. Keyed by
     * {@link Source.Tool} so tests can redirect each user-level tool's home into a
     * {@link org.junit.jupiter.api.io.TempDir}.
     */
    private final EnumMap<Source.Tool, Path> userDirOverrides;

    /** Guards {@link #entriesCache}, {@link #skillsCache}, {@link #fragmentCache} and {@link #loaded}. */
    private final Object cacheLock = new Object();

    private List<SkillEntry> entriesCache = Collections.emptyList();
    /** Cached {@link Skills} instance; rebuilt by {@link #buildSkills()} when null. */
    @Nullable
    private Skills skillsCache;
    /** Cached system-prompt fragment; rebuilt by {@link #getSystemPromptFragment()} when null. */
    @Nullable
    private String fragmentCache;
    /** {@code true} once at least one scan has populated the caches. */
    private boolean loaded;

    @SuppressWarnings("unused") // used by IntelliJ service container
    public SkillRegistry(@NotNull Project project) {
        this(project, new EnumMap<>(Source.Tool.class));
    }

    /**
     * Test constructor. The map keys point each user-level tool to a custom directory; entries
     * that are absent fall back to {@code ~/<tool.subdir()>}.
     */
    @TestOnly
    public SkillRegistry(@NotNull Project project, @NotNull Map<Source.Tool, Path> userDirOverrides) {
        this.project = project;
        this.userDirOverrides = new EnumMap<>(Source.Tool.class);
        this.userDirOverrides.putAll(userDirOverrides);
        // No disk I/O in the constructor: it may be called on the EDT (settings UI opening).
    }

    public static SkillRegistry getInstance(@NotNull Project project) {
        return project.getService(SkillRegistry.class);
    }

    /**
     * Synchronously scans every skill directory. <strong>Must not be called from the EDT</strong>
     * &mdash; use {@link #reloadAsync(Runnable)} instead for UI handlers.
     */
    public void reloadBlocking() {
        if (ApplicationManager.getApplication() != null
                && ApplicationManager.getApplication().isDispatchThread()) {
            log.warn("SkillRegistry.reloadBlocking() called from the EDT \u2014 prefer reloadAsync()");
        }
        scanAndUpdateCache();
    }

    /**
     * Schedules a pooled-thread scan of the skill directories. If {@code onComplete} is non-null
     * it is dispatched on the EDT once the cache has been updated.
     */
    public void reloadAsync(@Nullable Runnable onComplete) {
        Runnable task = () -> {
            try {
                scanAndUpdateCache();
            } finally {
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            }
        };
        if (ApplicationManager.getApplication() != null) {
            ApplicationManager.getApplication().executeOnPooledThread(task);
        } else {
            // Fallback for tests without the IntelliJ application bootstrap.
            new Thread(task, "SkillRegistry-reload").start();
        }
    }

    /**
     * Ensures the cache has been populated at least once. Performs a synchronous scan on the
     * caller's thread &mdash; only call from background threads. UI callers should use
     * {@link #reloadAsync(Runnable)}.
     */
    public void ensureLoaded() {
        synchronized (cacheLock) {
            if (loaded) {
                return;
            }
        }
        scanAndUpdateCache();
    }

    private void scanAndUpdateCache() {
        // Iterate Source.values() from lowest to highest priority. Each higher-priority entry
        // overwrites lower-priority ones on name collision, with a warning logged.
        Map<String, SkillEntry> byName = new LinkedHashMap<>();
        for (Source src : Source.values()) {
            Path dir = directoryFor(src);
            for (Skill s : loadFrom(dir, src)) {
                SkillEntry existing = byName.get(s.name());
                if (existing != null) {
                    log.warn("Skill '{}' from {} ({}) overrides earlier skill from {} ({})",
                            s.name(), src.label(), dir,
                            existing.source().label(), directoryFor(existing.source()));
                }
                byName.put(s.name(), new SkillEntry(s, src));
            }
        }

        List<SkillEntry> snapshot = List.copyOf(byName.values());
        synchronized (cacheLock) {
            this.entriesCache = snapshot;
            this.skillsCache = null;     // force rebuild on next buildSkills()
            this.fragmentCache = null;   // force rebuild on next getSystemPromptFragment()
            this.loaded = true;
        }
        log.info("Skills loaded: {} from sources {}", snapshot.size(),
                snapshot.stream().map(e -> e.source().label()).distinct().toList());
    }

    /**
     * @return every detected skill with metadata for display in settings, regardless of whether
     *         the user has disabled them. The list is immutable. Triggers a synchronous load
     *         if the cache has not been populated yet (do not call from the EDT in that case).
     */
    @NotNull
    public List<SkillEntry> getAllSkills() {
        ensureLoaded();
        synchronized (cacheLock) {
            return entriesCache;
        }
    }

    /**
     * @return the current cache contents without triggering a scan. Returns an empty list when
     *         the registry has not yet been loaded. Safe to call from the EDT.
     */
    @NotNull
    public List<SkillEntry> peekAllSkills() {
        synchronized (cacheLock) {
            return entriesCache;
        }
    }

    /**
     * @return the on-disk directory that corresponds to the given {@link Source}. May return
     *         {@code null} for project-scoped sources when {@link Project#getBasePath()} is
     *         unavailable (e.g. the default project in tests).
     */
    @Nullable
    public Path directoryFor(@NotNull Source source) {
        return switch (source.scope()) {
            case USER -> userDir(source.tool());
            case PROJECT -> projectDir(source.tool().subdir());
        };
    }

    @NotNull
    private Path userDir(@NotNull Source.Tool tool) {
        Path override = userDirOverrides.get(tool);
        return override != null ? override : defaultUserDir(tool.subdir());
    }

    /** @return {@code ~/.devoxxgenie/skills/}. */
    @NotNull
    public Path userDevoxxgenieDir() {
        return userDir(Source.Tool.DEVOXXGENIE);
    }

    /** @return {@code ~/.claude/skills/}. */
    @NotNull
    public Path userClaudeDir() {
        return userDir(Source.Tool.CLAUDE);
    }

    /** @return {@code ~/.agents/skills/}. */
    @NotNull
    public Path userAgentsDir() {
        return userDir(Source.Tool.AGENTS);
    }

    /**
     * Back-compat alias for {@link #userDevoxxgenieDir()}.
     */
    @NotNull
    @Deprecated
    public Path userSkillsDir() {
        return userDevoxxgenieDir();
    }

    /**
     * @return {@code <project>/.devoxxgenie/skills/} (may be {@code null} when no basePath).
     */
    @Nullable
    public Path projectDevoxxgenieDir() {
        return projectDir(DEVOXXGENIE_SKILLS_SUBDIR);
    }

    /**
     * @return {@code <project>/.claude/skills/} (may be {@code null} when no basePath).
     */
    @Nullable
    public Path projectClaudeDir() {
        return projectDir(CLAUDE_SKILLS_SUBDIR);
    }

    /**
     * @return {@code <project>/.agents/skills/} (may be {@code null} when no basePath).
     */
    @Nullable
    public Path projectAgentsDir() {
        return projectDir(AGENTS_SKILLS_SUBDIR);
    }

    /**
     * Back-compat alias for {@link #projectDevoxxgenieDir()}.
     */
    @Nullable
    @Deprecated
    public Path projectSkillsDir() {
        return projectDevoxxgenieDir();
    }

    @Nullable
    private Path projectDir(@NotNull String subdir) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return null;
        }
        return Paths.get(basePath, subdir);
    }

    /**
     * Builds (and caches) a langchain4j {@link Skills} instance containing every loaded skill
     * not in the disabled-set. Returns {@code null} when there are no enabled skills, because
     * {@code Skills.from(...)} rejects empty collections.
     */
    @Nullable
    public Skills buildSkills() {
        ensureLoaded();
        synchronized (cacheLock) {
            if (skillsCache != null) {
                return skillsCache;
            }
            List<Skill> active = activeSkillsLocked();
            if (active.isEmpty()) {
                return null;
            }
            try {
                skillsCache = Skills.from(active);
                return skillsCache;
            } catch (RuntimeException e) {
                log.warn("Failed to build langchain4j Skills wrapper", e);
                return null;
            }
        }
    }

    /**
     * Returns (and caches) the system-prompt fragment listing the active skills. Returns an
     * empty string when no skills are active.
     */
    @NotNull
    public String getSystemPromptFragment() {
        ensureLoaded();
        synchronized (cacheLock) {
            if (fragmentCache != null) {
                return fragmentCache;
            }
            Skills skills = buildSkillsLocked();
            fragmentCache = skills == null ? "" : skills.formatAvailableSkills();
            return fragmentCache;
        }
    }

    /** Same as {@link #buildSkills()} but assumes the caller already holds {@link #cacheLock}. */
    @Nullable
    private Skills buildSkillsLocked() {
        if (skillsCache != null) {
            return skillsCache;
        }
        List<Skill> active = activeSkillsLocked();
        if (active.isEmpty()) {
            return null;
        }
        try {
            skillsCache = Skills.from(active);
            return skillsCache;
        } catch (RuntimeException e) {
            log.warn("Failed to build langchain4j Skills wrapper", e);
            return null;
        }
    }

    /** Called with {@link #cacheLock} held. */
    private List<Skill> activeSkillsLocked() {
        Set<String> disabled = DevoxxGenieStateService.getInstance().getDisabledSkillNames();
        List<Skill> result = new ArrayList<>(entriesCache.size());
        for (SkillEntry entry : entriesCache) {
            if (disabled != null && disabled.contains(entry.name())) {
                continue;
            }
            result.add(entry.skill());
        }
        return result;
    }

    /**
     * Invalidates the cached {@link Skills} and system-prompt fragment without re-scanning
     * disk. Call after the user toggles {@code disabledSkillNames} so the next agent prompt
     * sees the new filter.
     */
    public void invalidateDerivedCaches() {
        synchronized (cacheLock) {
            this.skillsCache = null;
            this.fragmentCache = null;
        }
    }

    private List<? extends Skill> loadFrom(@Nullable Path dir, @NotNull Source source) {
        if (dir == null) {
            return List.of();
        }
        if (!Files.isDirectory(dir)) {
            log.debug("Skills {} directory not found: {}", source, dir);
            return List.of();
        }
        try {
            List<FileSystemSkill> skills = FileSystemSkillLoader.loadSkills(dir);
            log.debug("Loaded {} skill(s) from {} ({})", skills.size(), dir, source);
            return skills;
        } catch (RuntimeException e) {
            log.warn("Failed to load skills from {} ({}): {}", dir, source, e.getMessage());
            return List.of();
        }
    }

    @NotNull
    private static Path defaultUserDir(@NotNull String subdir) {
        String home = System.getProperty("user.home");
        return Paths.get(home, subdir);
    }

    /**
     * Ensures the directory backing the given source exists on disk, creating it if necessary.
     * Used by the settings UI's "Open folder" buttons so the user lands in an existing folder.
     *
     * @return the directory, or {@code null} when the source has no resolvable on-disk path
     *         (project sources without a basePath).
     */
    @Nullable
    public Path ensureDirectoryExists(@NotNull Source source) {
        Path dir = directoryFor(source);
        if (dir == null) {
            return null;
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Could not create skills dir {}: {}", dir, e.getMessage());
        }
        return dir;
    }

    /**
     * Creates every configured skills directory on disk if it does not yet exist. Retained for
     * callers that want to seed all locations at once (e.g. on first launch).
     */
    public void ensureDirectoriesExist() {
        for (Source src : Source.values()) {
            ensureDirectoryExists(src);
        }
    }
}
