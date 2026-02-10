package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.agent.TestResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestResultParser {

    // Gradle patterns
    private static final Pattern GRADLE_RESULT = Pattern.compile(
            "(\\d+) tests? completed, (\\d+) failed");
    private static final Pattern GRADLE_SKIPPED = Pattern.compile(
            "(\\d+) tests? skipped");
    private static final Pattern GRADLE_FAILED_TEST = Pattern.compile(
            "> (.+) FAILED");

    // Maven Surefire patterns
    private static final Pattern MAVEN_RESULT = Pattern.compile(
            "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");
    private static final Pattern MAVEN_FAILED_TEST = Pattern.compile(
            "<<<\\s*FAIL(?:URE)?!\\s*$", Pattern.MULTILINE);
    private static final Pattern MAVEN_FAILED_TEST_NAME = Pattern.compile(
            "(\\S+)\\s+<<<\\s*FAIL");

    // npm/Jest patterns
    private static final Pattern JEST_RESULT = Pattern.compile(
            "Tests:\\s+(\\d+) failed,\\s+(\\d+) passed,\\s+(\\d+) total");
    private static final Pattern JEST_ALL_PASSED = Pattern.compile(
            "Tests:\\s+(\\d+) passed,\\s+(\\d+) total");

    private static final int MAX_RAW_OUTPUT_LENGTH = 50_000;

    private TestResultParser() {
    }

    public static @NotNull TestResult parse(@NotNull String output,
                                             @NotNull BuildSystemDetector.BuildSystem buildSystem,
                                             int exitCode) {
        String truncatedOutput = truncate(output);

        return switch (buildSystem) {
            case GRADLE -> parseGradle(truncatedOutput, exitCode);
            case MAVEN -> parseMaven(truncatedOutput, exitCode);
            case NPM -> parseNpm(truncatedOutput, exitCode);
            default -> parseFallback(truncatedOutput, exitCode);
        };
    }

    private static @NotNull TestResult parseGradle(@NotNull String output, int exitCode) {
        int testsRun = 0;
        int testsFailed = 0;
        int testsSkipped = 0;

        Matcher resultMatcher = GRADLE_RESULT.matcher(output);
        while (resultMatcher.find()) {
            testsRun += Integer.parseInt(resultMatcher.group(1));
            testsFailed += Integer.parseInt(resultMatcher.group(2));
        }

        Matcher skippedMatcher = GRADLE_SKIPPED.matcher(output);
        while (skippedMatcher.find()) {
            testsSkipped += Integer.parseInt(skippedMatcher.group(1));
        }

        List<String> failedTestNames = new ArrayList<>();
        Matcher failedMatcher = GRADLE_FAILED_TEST.matcher(output);
        while (failedMatcher.find()) {
            failedTestNames.add(failedMatcher.group(1).trim());
        }

        int testsPassed = testsRun - testsFailed;
        TestResult.Status status = determineStatus(exitCode, testsFailed, testsRun);

        return buildResult(status, exitCode, testsRun, testsPassed, testsFailed, testsSkipped,
                failedTestNames, output);
    }

    private static @NotNull TestResult parseMaven(@NotNull String output, int exitCode) {
        int testsRun = 0;
        int testsFailed = 0;
        int testsErrors = 0;
        int testsSkipped = 0;

        Matcher resultMatcher = MAVEN_RESULT.matcher(output);
        while (resultMatcher.find()) {
            testsRun += Integer.parseInt(resultMatcher.group(1));
            testsFailed += Integer.parseInt(resultMatcher.group(2));
            testsErrors += Integer.parseInt(resultMatcher.group(3));
            testsSkipped += Integer.parseInt(resultMatcher.group(4));
        }

        int totalFailed = testsFailed + testsErrors;
        List<String> failedTestNames = new ArrayList<>();
        Matcher failedMatcher = MAVEN_FAILED_TEST_NAME.matcher(output);
        while (failedMatcher.find()) {
            failedTestNames.add(failedMatcher.group(1).trim());
        }

        int testsPassed = testsRun - totalFailed - testsSkipped;
        TestResult.Status status = determineStatus(exitCode, totalFailed, testsRun);

        return buildResult(status, exitCode, testsRun, testsPassed, totalFailed, testsSkipped,
                failedTestNames, output);
    }

    private static @NotNull TestResult parseNpm(@NotNull String output, int exitCode) {
        int testsRun = 0;
        int testsFailed = 0;
        int testsPassed = 0;

        Matcher failMatcher = JEST_RESULT.matcher(output);
        if (failMatcher.find()) {
            testsFailed = Integer.parseInt(failMatcher.group(1));
            testsPassed = Integer.parseInt(failMatcher.group(2));
            testsRun = Integer.parseInt(failMatcher.group(3));
        } else {
            Matcher passMatcher = JEST_ALL_PASSED.matcher(output);
            if (passMatcher.find()) {
                testsPassed = Integer.parseInt(passMatcher.group(1));
                testsRun = Integer.parseInt(passMatcher.group(2));
            }
        }

        TestResult.Status status = determineStatus(exitCode, testsFailed, testsRun);

        return buildResult(status, exitCode, testsRun, testsPassed, testsFailed, 0,
                List.of(), output);
    }

    private static @NotNull TestResult parseFallback(@NotNull String output, int exitCode) {
        TestResult.Status status = exitCode == 0 ? TestResult.Status.PASSED : TestResult.Status.FAILED;

        return buildResult(status, exitCode, 0, 0, 0, 0, List.of(), output);
    }

    private static TestResult.Status determineStatus(int exitCode, int failed, int total) {
        if (exitCode == 0 && failed == 0) {
            return TestResult.Status.PASSED;
        }
        if (failed > 0) {
            return TestResult.Status.FAILED;
        }
        if (exitCode != 0 && total == 0) {
            return TestResult.Status.ERROR;
        }
        return TestResult.Status.FAILED;
    }

    private static @NotNull TestResult buildResult(TestResult.Status status, int exitCode,
                                                    int testsRun, int testsPassed,
                                                    int testsFailed, int testsSkipped,
                                                    List<String> failedTestNames,
                                                    String rawOutput) {
        String summary = formatSummary(status, testsRun, testsPassed, testsFailed, testsSkipped,
                failedTestNames);

        return TestResult.builder()
                .status(status)
                .exitCode(exitCode)
                .testsRun(testsRun)
                .testsPassed(testsPassed)
                .testsFailed(testsFailed)
                .testsSkipped(testsSkipped)
                .failedTestNames(failedTestNames)
                .summary(summary)
                .rawOutput(rawOutput)
                .build();
    }

    private static @NotNull String formatSummary(TestResult.Status status,
                                                  int testsRun, int testsPassed,
                                                  int testsFailed, int testsSkipped,
                                                  List<String> failedTestNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("Test Result: ").append(status).append('\n');

        if (testsRun > 0) {
            sb.append("Tests run: ").append(testsRun);
            sb.append(", Passed: ").append(testsPassed);
            sb.append(", Failed: ").append(testsFailed);
            if (testsSkipped > 0) {
                sb.append(", Skipped: ").append(testsSkipped);
            }
            sb.append('\n');
        }

        if (!failedTestNames.isEmpty()) {
            sb.append("\nFailed tests:\n");
            for (String name : failedTestNames) {
                sb.append("  - ").append(name).append('\n');
            }
        }

        return sb.toString();
    }

    private static @NotNull String truncate(@NotNull String text) {
        if (text.length() > MAX_RAW_OUTPUT_LENGTH) {
            return text.substring(0, MAX_RAW_OUTPUT_LENGTH) + "\n... (output truncated)";
        }
        return text;
    }
}
