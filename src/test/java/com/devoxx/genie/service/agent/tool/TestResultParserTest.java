package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.agent.TestResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestResultParserTest {

    // --- Gradle parsing ---

    @Test
    void parseGradle_allPassed() {
        String output = """
                > Task :test

                10 tests completed, 0 failed

                BUILD SUCCESSFUL in 5s
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.GRADLE, 0);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
        assertThat(result.getTestsRun()).isEqualTo(10);
        assertThat(result.getTestsPassed()).isEqualTo(10);
        assertThat(result.getTestsFailed()).isZero();
        assertThat(result.getFailedTestNames()).isEmpty();
    }

    @Test
    void parseGradle_withFailures() {
        String output = """
                > Task :test

                > com.example.FooTest > testBar FAILED
                > com.example.BazTest > testQux FAILED

                10 tests completed, 2 failed

                BUILD FAILED
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.GRADLE, 1);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
        assertThat(result.getTestsRun()).isEqualTo(10);
        assertThat(result.getTestsPassed()).isEqualTo(8);
        assertThat(result.getTestsFailed()).isEqualTo(2);
        assertThat(result.getFailedTestNames()).containsExactly(
                "com.example.FooTest > testBar",
                "com.example.BazTest > testQux"
        );
    }

    @Test
    void parseGradle_withSkipped() {
        String output = """
                > Task :test

                8 tests completed, 0 failed, 2 tests skipped

                BUILD SUCCESSFUL
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.GRADLE, 0);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
        assertThat(result.getTestsRun()).isEqualTo(8);
        assertThat(result.getTestsSkipped()).isEqualTo(2);
    }

    @Test
    void parseGradle_compilationError() {
        String output = """
                > Task :compileJava FAILED

                FAILURE: Build failed with an exception.
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.GRADLE, 1);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.ERROR);
        assertThat(result.getTestsRun()).isZero();
    }

    // --- Maven parsing ---

    @Test
    void parseMaven_allPassed() {
        String output = """
                [INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
                [INFO] BUILD SUCCESS
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.MAVEN, 0);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
        assertThat(result.getTestsRun()).isEqualTo(15);
        assertThat(result.getTestsPassed()).isEqualTo(15);
        assertThat(result.getTestsFailed()).isZero();
    }

    @Test
    void parseMaven_withFailures() {
        String output = """
                [ERROR] testFoo(com.example.MyTest)  <<< FAILURE!
                [ERROR] testBar(com.example.MyTest)  <<< FAIL!
                [INFO] Tests run: 10, Failures: 1, Errors: 1, Skipped: 0
                [INFO] BUILD FAILURE
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.MAVEN, 1);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
        assertThat(result.getTestsRun()).isEqualTo(10);
        assertThat(result.getTestsFailed()).isEqualTo(2); // failures + errors
        assertThat(result.getTestsPassed()).isEqualTo(8);
    }

    @Test
    void parseMaven_withSkipped() {
        String output = """
                [INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 3
                [INFO] BUILD SUCCESS
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.MAVEN, 0);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
        assertThat(result.getTestsRun()).isEqualTo(10);
        assertThat(result.getTestsSkipped()).isEqualTo(3);
        assertThat(result.getTestsPassed()).isEqualTo(7);
    }

    // --- npm/Jest parsing ---

    @Test
    void parseNpm_allPassed() {
        String output = """
                PASS src/app.test.js
                Tests:       5 passed, 5 total
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.NPM, 0);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
        assertThat(result.getTestsRun()).isEqualTo(5);
        assertThat(result.getTestsPassed()).isEqualTo(5);
        assertThat(result.getTestsFailed()).isZero();
    }

    @Test
    void parseNpm_withFailures() {
        String output = """
                FAIL src/app.test.js
                Tests:   2 failed, 3 passed, 5 total
                """;

        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.NPM, 1);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
        assertThat(result.getTestsRun()).isEqualTo(5);
        assertThat(result.getTestsFailed()).isEqualTo(2);
        assertThat(result.getTestsPassed()).isEqualTo(3);
    }

    // --- Fallback parsing ---

    @Test
    void parseFallback_exitCodeZero() {
        TestResult result = TestResultParser.parse("some output",
                BuildSystemDetector.BuildSystem.CARGO, 0);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
        assertThat(result.getExitCode()).isZero();
        assertThat(result.getTestsRun()).isZero();
    }

    @Test
    void parseFallback_exitCodeNonZero() {
        TestResult result = TestResultParser.parse("error output",
                BuildSystemDetector.BuildSystem.CARGO, 1);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
        assertThat(result.getExitCode()).isEqualTo(1);
    }

    @Test
    void parseFallback_unknownBuildSystem() {
        TestResult result = TestResultParser.parse("output",
                BuildSystemDetector.BuildSystem.UNKNOWN, 0);

        assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
    }

    // --- Summary formatting ---

    @Test
    void summary_containsStatusAndCounts() {
        String output = "10 tests completed, 2 failed";
        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.GRADLE, 1);

        assertThat(result.getSummary()).contains("FAILED");
        assertThat(result.getSummary()).contains("Tests run: 10");
        assertThat(result.getSummary()).contains("Failed: 2");
    }

    @Test
    void summary_containsFailedTestNames() {
        String output = """
                > com.example.FooTest > testBar FAILED
                5 tests completed, 1 failed
                """;
        TestResult result = TestResultParser.parse(output,
                BuildSystemDetector.BuildSystem.GRADLE, 1);

        assertThat(result.getSummary()).contains("com.example.FooTest > testBar");
    }

    // --- Output truncation ---

    @Test
    void rawOutput_isTruncatedWhenTooLong() {
        String longOutput = "x".repeat(60_000);
        TestResult result = TestResultParser.parse(longOutput,
                BuildSystemDetector.BuildSystem.UNKNOWN, 0);

        assertThat(result.getRawOutput().length()).isLessThan(60_000);
        assertThat(result.getRawOutput()).contains("(output truncated)");
    }
}
