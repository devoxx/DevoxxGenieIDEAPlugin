package com.devoxx.genie.model.automation;

import lombok.Getter;

/**
 * IDE event types that can trigger agent activations.
 */
@Getter
public enum IdeEventType {

    // VCS / Git triggers
    BEFORE_COMMIT("Before Commit", Category.VCS, "Triggered before a git commit is created"),
    AFTER_COMMIT("After Commit", Category.VCS, "Triggered after a git commit is created"),
    BRANCH_SWITCH("Branch Switch", Category.VCS, "Triggered when switching git branches"),
    AFTER_PULL("After Pull/Merge", Category.VCS, "Triggered after a git pull or merge"),
    BEFORE_PUSH("Before Push", Category.VCS, "Triggered before pushing commits to remote"),

    // File & Editor triggers
    FILE_SAVED("File Saved", Category.FILE, "Triggered when a file is saved"),
    FILE_CREATED("File Created", Category.FILE, "Triggered when a new file is created"),
    FILE_OPENED("File Opened", Category.FILE, "Triggered when a file is opened in the editor"),

    // Build & Compilation triggers
    BUILD_FAILED("Build Failed", Category.BUILD, "Triggered when a build fails with errors"),
    BUILD_SUCCEEDED("Build Succeeded", Category.BUILD, "Triggered after a successful build"),
    GRADLE_SYNC("Gradle Sync Complete", Category.BUILD, "Triggered after Gradle sync finishes"),

    // Test triggers
    TEST_FAILED("Test Failed", Category.TEST, "Triggered when a test fails"),
    TEST_SUITE_PASSED("Test Suite Passed", Category.TEST, "Triggered when all tests in a suite pass"),
    TEST_RUN_COMPLETE("Test Run Complete", Category.TEST, "Triggered when a test run finishes"),

    // Code Structure triggers
    METHOD_ADDED("New Method Added", Category.CODE, "Triggered when a new method is added via PSI"),
    INTERFACE_CHANGED("Interface Changed", Category.CODE, "Triggered when an interface or abstract class is modified"),

    // Run / Debug triggers
    EXCEPTION_HIT("Exception During Debug", Category.DEBUG, "Triggered when an exception breakpoint is hit"),
    PROCESS_CRASHED("Process Crashed", Category.DEBUG, "Triggered when a process exits with non-zero code"),

    // Project Lifecycle triggers
    PROJECT_OPENED("Project Opened", Category.LIFECYCLE, "Triggered when a project is opened");

    public enum Category {
        VCS("VCS / Git"),
        FILE("File & Editor"),
        BUILD("Build & Compilation"),
        TEST("Testing"),
        CODE("Code Structure"),
        DEBUG("Run / Debug"),
        LIFECYCLE("Project Lifecycle");

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
