package com.devoxx.genie.service.agent.tool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class BuildSystemDetector {

    public enum BuildSystem {
        GRADLE, MAVEN, NPM, CARGO, GO, MAKE, UNKNOWN
    }

    private BuildSystemDetector() {
    }

    public static @NotNull BuildSystem detect(@NotNull String projectBasePath) {
        File base = new File(projectBasePath);

        if (new File(base, "build.gradle").exists() || new File(base, "build.gradle.kts").exists()) {
            return BuildSystem.GRADLE;
        }
        if (new File(base, "pom.xml").exists()) {
            return BuildSystem.MAVEN;
        }
        if (new File(base, "package.json").exists()) {
            return BuildSystem.NPM;
        }
        if (new File(base, "Cargo.toml").exists()) {
            return BuildSystem.CARGO;
        }
        if (new File(base, "go.mod").exists()) {
            return BuildSystem.GO;
        }
        if (new File(base, "Makefile").exists()) {
            return BuildSystem.MAKE;
        }
        return BuildSystem.UNKNOWN;
    }

    public static @NotNull List<String> getTestCommand(@NotNull BuildSystem buildSystem,
                                                        @Nullable String testTarget,
                                                        boolean isWindows) {
        List<String> command = new ArrayList<>();
        boolean hasTarget = testTarget != null && !testTarget.isBlank();

        switch (buildSystem) {
            case GRADLE -> addGradleCommands(command, testTarget, hasTarget, isWindows);
            case MAVEN  -> addMavenCommands(command, testTarget, hasTarget, isWindows);
            case NPM    -> addNpmCommands(command, testTarget, hasTarget, isWindows);
            case CARGO  -> addCargoCommands(command, testTarget, hasTarget);
            case GO     -> addGoCommands(command, testTarget, hasTarget);
            case MAKE   -> { command.add("make"); command.add("test"); }
            default     -> { /* Return empty — caller should handle unknown */ }
        }
        return command;
    }

    private static void addGradleCommands(@NotNull List<String> command, String testTarget,
                                          boolean hasTarget, boolean isWindows) {
        command.add(isWindows ? "gradlew.bat" : "./gradlew");
        command.add("test");
        if (hasTarget) {
            command.add("--tests");
            command.add(testTarget);
        }
    }

    private static void addMavenCommands(@NotNull List<String> command, String testTarget,
                                         boolean hasTarget, boolean isWindows) {
        command.add(isWindows ? "mvn.cmd" : "mvn");
        command.add("test");
        if (hasTarget) {
            command.add("-Dtest=" + testTarget);
        }
    }

    private static void addNpmCommands(@NotNull List<String> command, String testTarget,
                                       boolean hasTarget, boolean isWindows) {
        command.add(isWindows ? "npm.cmd" : "npm");
        command.add("test");
        if (hasTarget) {
            command.add("--");
            command.add(testTarget);
        }
    }

    private static void addCargoCommands(@NotNull List<String> command, String testTarget,
                                         boolean hasTarget) {
        command.add("cargo");
        command.add("test");
        if (hasTarget) {
            command.add(testTarget);
        }
    }

    private static void addGoCommands(@NotNull List<String> command, String testTarget,
                                      boolean hasTarget) {
        command.add("go");
        command.add("test");
        if (hasTarget) {
            command.add(testTarget);
        } else {
            command.add("./...");
        }
    }

    public static @NotNull String buildCommandString(@NotNull List<String> commandParts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commandParts.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String part = commandParts.get(i);
            if (part.contains(" ")) {
                sb.append('"').append(part).append('"');
            } else {
                sb.append(part);
            }
        }
        return sb.toString();
    }
}
