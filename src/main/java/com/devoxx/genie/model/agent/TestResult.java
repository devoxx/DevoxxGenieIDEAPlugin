package com.devoxx.genie.model.agent;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestResult {

    public enum Status {
        PASSED, FAILED, ERROR, TIMEOUT
    }

    private final Status status;
    private final int exitCode;
    private final int testsRun;
    private final int testsPassed;
    private final int testsFailed;
    private final int testsSkipped;
    private final List<String> failedTestNames;
    private final String summary;
    private final String rawOutput;
}
