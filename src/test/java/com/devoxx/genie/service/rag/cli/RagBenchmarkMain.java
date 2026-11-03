package com.devoxx.genie.service.rag.cli;

/**
 * Entry point for the {@code ragBenchmark} Gradle task, equivalent to {@code RagCli benchmark}.
 * Exists because Gradle's {@code --args} replaces a task's preset arguments, so the task can't
 * prepend the {@code benchmark} subcommand itself.
 */
public final class RagBenchmarkMain {

    private RagBenchmarkMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && (args[0].equals("--help") || args[0].equals("-h"))) {
            RagBenchmarkCommand.printUsage();
            return;
        }
        RagBenchmarkCommand.run(args);
    }
}
