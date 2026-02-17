package com.devoxx.genie.model.spec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the backlog/config.yml configuration file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacklogConfig {
    private String projectName;
    @Builder.Default
    private String defaultStatus = "To Do";
    @Builder.Default
    private List<String> statuses = new ArrayList<>(List.of("To Do", "In Progress", "Done"));
    @Builder.Default
    private List<String> labels = new ArrayList<>();
    @Builder.Default
    private List<BacklogMilestone> milestones = new ArrayList<>();
    @Builder.Default
    private String taskPrefix = "task";
    @Builder.Default
    private String dateFormat = "yyyy-mm-dd";
    @Builder.Default
    private int maxColumnWidth = 20;
    @Builder.Default
    private boolean autoOpenBrowser = true;
    @Builder.Default
    private int defaultPort = 6420;
    @Builder.Default
    private boolean remoteOperations = true;
    @Builder.Default
    private boolean autoCommit = false;
    @Builder.Default
    private boolean bypassGitHooks = false;
    @Builder.Default
    private boolean checkActiveBranches = true;
    @Builder.Default
    private int activeBranchDays = 30;

    /**
     * Project-wide Definition of Done defaults.
     * These items are automatically added to every new task unless explicitly skipped.
     * Compatible with Backlog.md's definition_of_done config.
     */
    @Builder.Default
    private List<String> definitionOfDone = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacklogMilestone {
        private String name;
        private String description;
    }
}
