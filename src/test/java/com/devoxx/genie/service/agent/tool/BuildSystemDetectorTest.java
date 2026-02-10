package com.devoxx.genie.service.agent.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuildSystemDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void detect_gradle_withBuildGradle() throws IOException {
        new File(tempDir.toFile(), "build.gradle").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.GRADLE);
    }

    @Test
    void detect_gradle_withBuildGradleKts() throws IOException {
        new File(tempDir.toFile(), "build.gradle.kts").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.GRADLE);
    }

    @Test
    void detect_maven_withPomXml() throws IOException {
        new File(tempDir.toFile(), "pom.xml").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.MAVEN);
    }

    @Test
    void detect_npm_withPackageJson() throws IOException {
        new File(tempDir.toFile(), "package.json").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.NPM);
    }

    @Test
    void detect_cargo_withCargoToml() throws IOException {
        new File(tempDir.toFile(), "Cargo.toml").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.CARGO);
    }

    @Test
    void detect_go_withGoMod() throws IOException {
        new File(tempDir.toFile(), "go.mod").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.GO);
    }

    @Test
    void detect_make_withMakefile() throws IOException {
        new File(tempDir.toFile(), "Makefile").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.MAKE);
    }

    @Test
    void detect_unknown_emptyDirectory() {
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.UNKNOWN);
    }

    @Test
    void detect_gradle_prioritizedOverMaven() throws IOException {
        new File(tempDir.toFile(), "build.gradle").createNewFile();
        new File(tempDir.toFile(), "pom.xml").createNewFile();
        assertThat(BuildSystemDetector.detect(tempDir.toString()))
                .isEqualTo(BuildSystemDetector.BuildSystem.GRADLE);
    }

    // --- Command generation ---

    @Test
    void getTestCommand_gradle_unix_noTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.GRADLE, null, false);
        assertThat(cmd).containsExactly("./gradlew", "test");
    }

    @Test
    void getTestCommand_gradle_unix_withTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.GRADLE, "com.example.MyTest", false);
        assertThat(cmd).containsExactly("./gradlew", "test", "--tests", "com.example.MyTest");
    }

    @Test
    void getTestCommand_gradle_windows_noTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.GRADLE, null, true);
        assertThat(cmd).containsExactly("gradlew.bat", "test");
    }

    @Test
    void getTestCommand_maven_unix_noTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.MAVEN, null, false);
        assertThat(cmd).containsExactly("mvn", "test");
    }

    @Test
    void getTestCommand_maven_unix_withTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.MAVEN, "MyTest#testMethod", false);
        assertThat(cmd).containsExactly("mvn", "test", "-Dtest=MyTest#testMethod");
    }

    @Test
    void getTestCommand_npm_unix_noTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.NPM, null, false);
        assertThat(cmd).containsExactly("npm", "test");
    }

    @Test
    void getTestCommand_npm_unix_withTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.NPM, "mytest.spec.js", false);
        assertThat(cmd).containsExactly("npm", "test", "--", "mytest.spec.js");
    }

    @Test
    void getTestCommand_cargo_noTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.CARGO, null, false);
        assertThat(cmd).containsExactly("cargo", "test");
    }

    @Test
    void getTestCommand_cargo_withTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.CARGO, "my_test", false);
        assertThat(cmd).containsExactly("cargo", "test", "my_test");
    }

    @Test
    void getTestCommand_go_noTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.GO, null, false);
        assertThat(cmd).containsExactly("go", "test", "./...");
    }

    @Test
    void getTestCommand_go_withTarget() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.GO, "./pkg/...", false);
        assertThat(cmd).containsExactly("go", "test", "./pkg/...");
    }

    @Test
    void getTestCommand_make() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.MAKE, null, false);
        assertThat(cmd).containsExactly("make", "test");
    }

    @Test
    void getTestCommand_unknown_returnsEmpty() {
        List<String> cmd = BuildSystemDetector.getTestCommand(
                BuildSystemDetector.BuildSystem.UNKNOWN, null, false);
        assertThat(cmd).isEmpty();
    }

    // --- Command string building ---

    @Test
    void buildCommandString_simple() {
        String result = BuildSystemDetector.buildCommandString(List.of("./gradlew", "test"));
        assertThat(result).isEqualTo("./gradlew test");
    }

    @Test
    void buildCommandString_withSpaces() {
        String result = BuildSystemDetector.buildCommandString(
                List.of("./gradlew", "test", "--tests", "com.example.My Test"));
        assertThat(result).isEqualTo("./gradlew test --tests \"com.example.My Test\"");
    }
}
