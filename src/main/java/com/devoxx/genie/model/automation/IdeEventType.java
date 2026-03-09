package com.devoxx.genie.model.automation;

import lombok.Getter;

/**
 * IDE event types that can trigger agent activations.
 */
@Getter
public enum IdeEventType {

    // VCS / Git triggers
    BEFORE_COMMIT("Before Commit", Category.VCS, "Triggered before a git commit is created"),

    // File & Editor triggers
    FILE_SAVED("File Saved", Category.FILE, "Triggered when a file is saved"),
    FILE_OPENED("File Opened", Category.FILE, "Triggered when a file is opened in the editor"),

    // Build & Compilation triggers
    BUILD_FAILED("Build Failed", Category.BUILD, "Triggered when a build fails with errors"),
    BUILD_SUCCEEDED("Build Succeeded", Category.BUILD, "Triggered after a successful build"),

    // Test triggers
    TEST_FAILED("Test Failed", Category.TEST, "Triggered when a test fails"),
    TEST_SUITE_PASSED("Test Suite Passed", Category.TEST, "Triggered when all tests in a suite pass"),

    // Run / Debug triggers
    PROCESS_CRASHED("Process Crashed", Category.DEBUG, "Triggered when a process exits with non-zero code");

    public enum Category {
        VCS("VCS / Git"),
        FILE("File & Editor"),
        BUILD("Build & Compilation"),
        TEST("Testing"),
        DEBUG("Run / Debug");

        @Getter
        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }
    }

    private final String displayName;
    private final Category category;
    private final String description;

    IdeEventType(String displayName, Category category, String description) {
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }
}
