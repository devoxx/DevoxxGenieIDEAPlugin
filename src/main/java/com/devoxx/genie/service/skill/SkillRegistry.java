package com.devoxx.genie.service.skill;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
 * are filtered out.</p>
 *
 * <p>The registry wraps the {@code @Experimental} langchain4j-skills API so the rest of the
 * codebase only depends on this class.</p>
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class SkillRegistry {

    public static final String SKILLS_SUBDIR = ".devoxxgenie/skills";

    /** Describes the origin of a loaded skill for the settings UI. */
    public enum Source { USER, PROJECT }

    /** Skill plus metadata for the settings UI. */
    public record SkillEntry(@NotNull Skill skill, @NotNull Source source, boolean disabled) {
        public String name() {
            return skill.name();
        }
        public String description() {
            return skill.description();
        }
    }

    private final Project project;
    private final Path userSkillsDirOverride;

    private List<SkillEntry> entriesCache = Collections.emptyList();

    @SuppressWarnings("unused") // used by IntelliJ service container
    public SkillRegistry(@NotNull Project project) {
        this(project, defaultUserSkillsDir());
    }

    @TestOnly
    public SkillRegistry(@NotNull Project project, @Nullable Path userSkillsDirOverride) {
        this.project = project;
        this.userSkillsDirOverride = userSkillsDirOverride;
        reload();
    }

    public static SkillRegistry getInstance(@NotNull Project project) {
        return project.getService(SkillRegistry.class);
    }

    /**
     * Re-scans the user and project skill directories from disk.
     */
    public synchronized void reload() {
        Map<String, SkillEntry> byName = new LinkedHashMap<>();

        // Load user skills first.
        Path userDir = userSkillsDir();
        for (Skill s : loadFrom(userDir, Source.USER)) {
            byName.put(s.name(), toEntry(s, Source.USER));
        }

        // Project skills override user skills on name collision.
        Path projectDir = projectSkillsDir();
        for (Skill s : loadFrom(projectDir, Source.PROJECT)) {
            if (byName.containsKey(s.name())) {
                log.warn("Skill '{}' from project directory {} overrides user-level skill of the same name",
                        s.name(), projectDir);
            }
            byName.put(s.name(), toEntry(s, Source.PROJECT));
        }

        this.entriesCache = List.copyOf(byName.values());
        log.info("Skills loaded: {} user dir={} project dir={}", entriesCache.size(), userDir, projectDir);
    }

    /**
     * @return every detected skill with metadata for display in settings, regardless of whether
     *         the user has disabled them. The list is immutable.
     */
    @NotNull
    public synchronized List<SkillEntry> getAllSkills() {
        return entriesCache;
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
     * Builds a langchain4j {@link Skills} instance containing every loaded skill not in the
     * disabled-set. Returns {@code null} when there are no enabled skills, because
     * {@code Skills.from(...)} rejects empty collections.
     */
    @Nullable
    public synchronized Skills buildSkills() {
        List<Skill> active = activeSkills();
        if (active.isEmpty()) {
            return null;
        }
        try {
            return Skills.from(active);
        } catch (RuntimeException e) {
            log.warn("Failed to build langchain4j Skills wrapper", e);
            return null;
        }
    }

    /**
     * Builds the system-prompt fragment listing the active skills. Returns an empty string when
     * no skills are active.
     */
    @NotNull
    public synchronized String getSystemPromptFragment() {
        Skills skills = buildSkills();
        return skills == null ? "" : skills.formatAvailableSkills();
    }

    private List<Skill> activeSkills() {
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

    private SkillEntry toEntry(@NotNull Skill skill, @NotNull Source source) {
        Set<String> disabled = DevoxxGenieStateService.getInstance().getDisabledSkillNames();
        boolean isDisabled = disabled != null && disabled.contains(skill.name());
        return new SkillEntry(skill, source, isDisabled);
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
