package com.devoxx.genie.ui.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Project-local scan preferences. Global preferences are retained only as migration defaults. */
@Service(Service.Level.PROJECT)
@State(name = "DevoxxGenieProjectScanSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class ProjectScanSettingsService implements PersistentStateComponent<ProjectScanSettingsService.ScanSettings> {
    // Stable, empty serialization defaults ensure migrated values are saved even when
    // they still match the global settings at the time of the first save.
    public static final class ScanSettings {
        public Boolean useGitIgnore;
        public Boolean excludeJavaDoc;
        public List<String> excludedDirectories;
        public List<String> excludedFiles;
        public List<String> includedFileExtensions;
        public List<String> ragExcludedDirectories;
    }

    private final ScanSettings state = new ScanSettings();
    /** True for a scanner that runs without a project: it follows the global settings, read at every call. */
    private final boolean followsGlobal;

    public ProjectScanSettingsService() {
        this(false);
    }

    private ProjectScanSettingsService(boolean followsGlobal) {
        this.followsGlobal = followsGlobal;
        if (followsGlobal) {
            return;
        }
        DevoxxGenieStateService defaults = DevoxxGenieStateService.getInstance();
        state.useGitIgnore = defaults.getUseGitIgnore();
        state.excludeJavaDoc = defaults.getExcludeJavaDoc();
        state.excludedDirectories = copy(defaults.getExcludedDirectories());
        state.excludedFiles = copy(defaults.getExcludedFiles());
        state.includedFileExtensions = copy(defaults.getIncludedFileExtensions());
        state.ragExcludedDirectories = copy(defaults.getRagExcludedDirectories());
    }

    public static ProjectScanSettingsService getInstance(@NotNull Project project) {
        ProjectScanSettingsService service = project.getService(ProjectScanSettingsService.class);
        return service != null ? service : followingGlobal();
    }

    /** The global Scan & Copy settings, read at every call, for scanners that run without a project. */
    public static ProjectScanSettingsService followingGlobal() {
        return new ProjectScanSettingsService(true);
    }

    @Override
    public @NotNull ScanSettings getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull ScanSettings saved) {
        if (saved.useGitIgnore != null) state.useGitIgnore = saved.useGitIgnore;
        if (saved.excludeJavaDoc != null) state.excludeJavaDoc = saved.excludeJavaDoc;
        if (saved.excludedDirectories != null) state.excludedDirectories = copy(saved.excludedDirectories);
        if (saved.excludedFiles != null) state.excludedFiles = copy(saved.excludedFiles);
        if (saved.includedFileExtensions != null) state.includedFileExtensions = copy(saved.includedFileExtensions);
        if (saved.ragExcludedDirectories != null) state.ragExcludedDirectories = copy(saved.ragExcludedDirectories);
    }

    public Boolean getUseGitIgnore() {
        return followsGlobal ? DevoxxGenieStateService.getInstance().getUseGitIgnore() : state.useGitIgnore;
    }

    public void setUseGitIgnore(Boolean value) {
        state.useGitIgnore = value;
    }

    public Boolean getExcludeJavaDoc() {
        return followsGlobal ? DevoxxGenieStateService.getInstance().getExcludeJavaDoc() : state.excludeJavaDoc;
    }

    public void setExcludeJavaDoc(Boolean value) {
        state.excludeJavaDoc = value;
    }

    public List<String> getExcludedDirectories() {
        return followsGlobal ? DevoxxGenieStateService.getInstance().getExcludedDirectories() : state.excludedDirectories;
    }

    public void setExcludedDirectories(List<String> value) {
        state.excludedDirectories = copy(value);
    }

    public List<String> getExcludedFiles() {
        return followsGlobal ? DevoxxGenieStateService.getInstance().getExcludedFiles() : state.excludedFiles;
    }

    public void setExcludedFiles(List<String> value) {
        state.excludedFiles = copy(value);
    }

    public List<String> getIncludedFileExtensions() {
        return followsGlobal ? DevoxxGenieStateService.getInstance().getIncludedFileExtensions() : state.includedFileExtensions;
    }

    public void setIncludedFileExtensions(List<String> value) {
        state.includedFileExtensions = copy(value);
    }

    public List<String> getRagExcludedDirectories() {
        return followsGlobal ? DevoxxGenieStateService.getInstance().getRagExcludedDirectories() : state.ragExcludedDirectories;
    }

    public void setRagExcludedDirectories(List<String> value) {
        state.ragExcludedDirectories = copy(value);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
