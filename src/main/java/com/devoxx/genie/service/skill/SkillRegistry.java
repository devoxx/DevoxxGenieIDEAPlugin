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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project-scoped registry of langchain4j {@link Skill}s loaded from disk.
 *
 * <p>Two source directories are scanned:</p>
 * <ul>
 *   <li><strong>User skills</strong>: {@code ~/.devoxxgenie/skills/} (shared across projects)</li>
 *   <li><strong>Project skills</strong>: {@code &lt;project.basePath&gt;/.devoxxgenie/skills/}</li>
 * </ul>
 *
 * <p>If a skill name exists in both directories, the project-level skill wins (a warning is
 * logged). Skills whose name appears in {@link DevoxxGenieStateService#getDisabledSkillNames()}
 * are filtered out at the point where {@link Skills} is built.</p>
 *
 * <p>The registry wraps the {@code @Experimental} langchain4j-skills API so the rest of the
 * codebase only depends on this class.</p>
 *
 * <h3>Threading</h3>
 * <p>Disk scans must not run on the Event Dispatch Thread (EDT). The registry exposes:</p>
 * <ul>
 *   <li>{@link #reloadAsync(Runnable)} \u2014 schedules a scan on a pooled thread; the optional
 *       callback is dispatched on the EDT when the cache has been updated. Use this from
 *       Swing handlers (e.g. the Settings UI "Reload" button).</li>
 *   <li>{@link #reloadBlocking()} \u2014 synchronous scan suitable for background threads (agent
 *       execution path). Must <strong>not</strong> be called from the EDT.</li>
 * </ul>
 * <p>The constructor does not touch disk. The first lookup that needs a populated cache will
 * trigger a synchronous load via {@link #ensureLoaded()} \u2014 again, callers from the EDT must
 * have already triggered a {@link #reloadAsync(Runnable)}.</p>
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class SkillRegistry {

    public static final String SKILLS_SUBDIR = ".devoxxgenie/skills";

    /**
     * Tool name exposed by the langchain4j-skills {@code Skills.toolProvider()} that the LLM
     * invokes to activate a discovered skill. Tied to the {@code @Experimental} API \u2014 if
     * upstream renames it this constant must be updated in lockstep.
     */
    public static final String ACTIVATE_SKILL_TOOL_NAME = "activate_skill";

    /** Describes the origin of a loaded skill for the settings UI. */
    public enum Source { USER, PROJECT }

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
    private final Path userSkillsDirOverride;

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
        this(project, defaultUserSkillsDir());
    }

    @TestOnly
    public SkillRegistry(@NotNull Project project, @Nullable Path userSkillsDirOverride) {
        this.project = project;
        this.userSkillsDirOverride = userSkillsDirOverride;
        // No disk I/O in the constructor: it may be called on the EDT (settings UI opening).
        // The first consumer that needs data either:
        //   - calls reloadAsync(...) (settings UI) and waits for the callback, or
        //   - calls ensureLoaded() which performs a synchronous scan; safe from background threads
        //     such as the agent execution path.
    }

    public static SkillRegistry getInstance(@NotNull Project project) {
        return project.getService(SkillRegistry.class);
    }

    /**
     * Synchronously scans the user and project skill directories. <strong>Must not be called
     * from the EDT</strong> \u2014 use {@link #reloadAsync(Runnable)} instead for UI handlers.
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
     * caller's thread \u2014 only call from background threads. UI callers should use
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
        Map<String, SkillEntry> byName = new LinkedHashMap<>();

        Path userDir = userSkillsDir();
        for (Skill s : loadFrom(userDir, Source.USER)) {
            byName.put(s.name(), new SkillEntry(s, Source.USER));
        }

        Path projectDir = projectSkillsDir();
        for (Skill s : loadFrom(projectDir, Source.PROJECT)) {
            if (byName.containsKey(s.name())) {
                log.warn("Skill '{}' from project directory {} overrides user-level skill of the same name",
                        s.name(), projectDir);
            }
            byName.put(s.name(), new SkillEntry(s, Source.PROJECT));
        }

        List<SkillEntry> snapshot = List.copyOf(byName.values());
        synchronized (cacheLock) {
            this.entriesCache = snapshot;
            this.skillsCache = null;     // force rebuild on next buildSkills()
            this.fragmentCache = null;   // force rebuild on next getSystemPromptFragment()
            this.loaded = true;
        }
        log.info("Skills loaded: {} user dir={} project dir={}", snapshot.size(), userDir, projectDir);
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
     * @return user skills directory (regardless of whether it currently exists)
     */
    @NotNull
    public Path userSkillsDir() {
        return userSkillsDirOverride != null ? userSkillsDirOverride : defaultUserSkillsDir();
    }

    /**
     * @return project skills directory (regardless of whether it currently exists). Falls back to
     *         {@code null} if the project has no basePath (e.g. default project).
     */
    @Nullable
    public Path projectSkillsDir() {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return null;
        }
        return Paths.get(basePath, SKILLS_SUBDIR);
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
    private static Path defaultUserSkillsDir() {
        String home = System.getProperty("user.home");
        return Paths.get(home, SKILLS_SUBDIR);
    }

    /**
     * Creates the configured skills directories on disk if they don't yet exist.
     * Used by the settings UI's "Open folder" buttons so the user lands in an existing folder.
     */
    public void ensureDirectoriesExist() {
        try {
            Files.createDirectories(userSkillsDir());
        } catch (IOException e) {
            log.warn("Could not create user skills dir {}: {}", userSkillsDir(), e.getMessage());
        }
        Path projectDir = projectSkillsDir();
        if (projectDir != null) {
            try {
                Files.createDirectories(projectDir);
            } catch (IOException e) {
                log.warn("Could not create project skills dir {}: {}", projectDir, e.getMessage());
            }
        }
    }
}
