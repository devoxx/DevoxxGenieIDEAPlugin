package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.service.cli.CliConsoleManager;
import com.devoxx.genie.service.cli.command.CliCommand;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.compose.ConversationViewController;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Prompt execution strategy for CLI Runners.
 * Executes a CLI tool as an external process, streaming output to both
 * the conversation WebView and the Run tool window console.
 */
@Slf4j
public class CliPromptStrategy extends AbstractPromptExecutionStrategy {

    private final AtomicReference<Process> activeProcess = new AtomicReference<>();

    public CliPromptStrategy(@NotNull Project project) {
        super(project);
    }

    @Override
    protected String getStrategyName() {
        return "CLI Runner";
    }

    private record CliExecutionParams(
            @NotNull CliToolConfig cliTool,
            @NotNull CliCommand cliCommand,
            @NotNull List<String> command,
            @NotNull String prompt) {}

    private record ProcessOutcome(
            int exitCode,
            long elapsed,
            long startTime,
            @NotNull StringBuilder accumulatedResponse,
            @NotNull List<String> stderrLines) {}

    @Override
    protected void executeStrategySpecific(@NotNull ChatMessageContext context,
                                           @NotNull PromptOutputPanel panel,
                                           @NotNull PromptTask<PromptResult> resultTask) {
        String toolName = context.getLanguageModel().getModelName();
        CliToolConfig cliTool = findCliTool(toolName);
        if (cliTool == null) {
            log.error("CLI tool not found: {}", toolName);
            resultTask.complete(PromptResult.failure(context,
                    new IllegalStateException("CLI tool not found: " + toolName)));
            return;
        }

        CliToolConfig.CliType cliType = cliTool.getType() != null ? cliTool.getType() : CliToolConfig.CliType.CUSTOM;
        // Auto-detect type from tool name when stored type is CUSTOM (backwards compat)
        if (cliType == CliToolConfig.CliType.CUSTOM) {
            for (CliToolConfig.CliType t : CliToolConfig.CliType.values()) {
                if (t != CliToolConfig.CliType.CUSTOM &&
                        t.getDisplayName().equalsIgnoreCase(cliTool.getName())) {
                    cliType = t;
                    break;
                }
            }
        }
        CliCommand cliCommand = cliType.createCommand();

        String prompt = buildPromptWithHistory(context);
        List<String> command = cliCommand.buildChatCommand(cliTool, prompt);

        log.info("CLI chat execute: tool={}, type={}, command={}", toolName, cliType, command);

        CliConsoleManager consoleManager = CliConsoleManager.getInstance(project);
        consoleManager.printTaskHeader("chat", prompt.length() > 60 ? prompt.substring(0, 60) + "..." : prompt, toolName);
        consoleManager.activateToolWindow();

        ConversationViewController viewController =
                panel.getConversationPanel() != null ? panel.getConversationPanel().viewController : null;

        CliExecutionParams params = new CliExecutionParams(cliTool, cliCommand, command, prompt);
        threadPoolManager.getPromptExecutionPool().execute(() ->
                runCliProcess(context, params, consoleManager, viewController, resultTask));

        resultTask.whenComplete((result, error) -> {
            if (resultTask.isCancelled()) {
                destroyProcess();
            }
        });
    }

    private void runCliProcess(@NotNull ChatMessageContext context,
                               @NotNull CliExecutionParams params,
                               @NotNull CliConsoleManager consoleManager,
                               @Nullable ConversationViewController viewController,
                               @NotNull PromptTask<PromptResult> resultTask) {
        long startTime = System.currentTimeMillis();
        try {
            Process process = startProcess(params.command(), params.cliTool());
            activeProcess.set(process);
            log.info("CLI chat process started (pid={})", process.pid());

            params.cliCommand().writePrompt(process, params.prompt());

            StringBuilder accumulatedResponse = new StringBuilder();
            List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

            Thread stderrThread = startStderrReader(process, stderrLines, consoleManager);
            streamStdoutToViews(process, params.cliCommand(), accumulatedResponse, consoleManager, viewController, context, resultTask);

            log.info("CLI stdout captured {} chars: [{}]", accumulatedResponse.length(),
                    accumulatedResponse.length() > 300 ? accumulatedResponse.substring(0, 300) + "..." : accumulatedResponse);

            int exitCode = process.waitFor();
            stderrThread.join(5000);
            activeProcess.set(null);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("CLI chat process exited: exitCode={}, elapsed={}ms", exitCode, elapsed);

            ProcessOutcome outcome = new ProcessOutcome(exitCode, elapsed, startTime, accumulatedResponse, stderrLines);
            finalizeProcessResult(outcome, consoleManager, viewController, context, resultTask);

        } catch (IOException e) {
            activeProcess.set(null);
            handleProcessStartFailure(e, consoleManager, context, resultTask);
        } catch (InterruptedException e) {
            activeProcess.set(null);
            handleProcessInterrupted(consoleManager, resultTask);
        }
    }

    private @NotNull Process startProcess(@NotNull List<String> command,
                                          @NotNull CliToolConfig cliTool) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        String basePath = project.getBasePath();
        if (basePath != null) {
            pb.directory(new java.io.File(basePath));
        }

        pb.environment().putAll(com.intellij.util.EnvironmentUtil.getEnvironmentMap());

        if (cliTool.getEnvVars() != null && !cliTool.getEnvVars().isEmpty()) {
            pb.environment().putAll(cliTool.getEnvVars());
        }

        return pb.start();
    }

    private @NotNull Thread startStderrReader(@NotNull Process process,
                                              @NotNull List<String> stderrLines,
                                              @NotNull CliConsoleManager consoleManager) {
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrLines.add(line);
                    log.debug("CLI stderr raw line: [{}]", line);
                    final String text = line;
                    ApplicationManager.getApplication().invokeLater(() ->
                            consoleManager.printError(text));
                }
            } catch (IOException e) {
                log.debug("CLI stderr reader ended: {}", e.getMessage());
            }
        }, "cli-chat-stderr-reader");
        stderrThread.setDaemon(true);
        stderrThread.start();
        return stderrThread;
    }

    private void streamStdoutToViews(@NotNull Process process,
                                     @NotNull CliCommand cliCommand,
                                     @NotNull StringBuilder accumulatedResponse,
                                     @NotNull CliConsoleManager consoleManager,
                                     @Nullable ConversationViewController viewController,
                                     @NotNull ChatMessageContext context,
                                     @NotNull PromptTask<PromptResult> resultTask) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (resultTask.isCancelled()) {
                    destroyProcess();
                    return;
                }

                final String consoleLine = line;
                log.debug("CLI stdout raw line: [{}]", line);

                String filtered = cliCommand.filterResponseLine(line);
                log.debug("CLI stdout filtered: [{}]", filtered);
                if (filtered != null) {
                    accumulatedResponse.append(filtered).append("\n");
                }
                final String fullText = accumulatedResponse.toString();

                ApplicationManager.getApplication().invokeLater(() -> {
                    consoleManager.printOutput(consoleLine);

                    if (viewController != null && !fullText.isEmpty()) {
                        context.setAiMessage(AiMessage.from(fullText));
                        viewController.updateAiMessageContent(context);
                    }
                });
            }
        }
    }

    private void finalizeProcessResult(@NotNull ProcessOutcome outcome,
                                       @NotNull CliConsoleManager consoleManager,
                                       @Nullable ConversationViewController viewController,
                                       @NotNull ChatMessageContext context,
                                       @NotNull PromptTask<PromptResult> resultTask) {
        String exitMsg = "\n=== Process exited with code " + outcome.exitCode() + " (after " + outcome.elapsed() + "ms) ===\n";

        ApplicationManager.getApplication().invokeLater(() -> {
            if (outcome.exitCode() == 0) {
                finalizeSuccess(exitMsg, outcome.startTime(), outcome.accumulatedResponse(), consoleManager,
                        viewController, context, resultTask);
            } else {
                finalizeError(outcome.exitCode(), exitMsg, outcome.stderrLines(), consoleManager, context, resultTask);
            }
        });
    }

    private void finalizeSuccess(@NotNull String exitMsg,
                                 long startTime,
                                 @NotNull StringBuilder accumulatedResponse,
                                 @NotNull CliConsoleManager consoleManager,
                                 @Nullable ConversationViewController viewController,
                                 @NotNull ChatMessageContext context,
                                 @NotNull PromptTask<PromptResult> resultTask) {
        consoleManager.printSystem(exitMsg);

        context.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        String finalText = accumulatedResponse.toString();
        if (!finalText.isEmpty()) {
            context.setAiMessage(AiMessage.from(finalText));
        }

        if (viewController != null) {
            viewController.updateAiMessageContent(context);
            viewController.markMCPLogsAsCompleted(context.getId());
        }

        ChatMemoryManager.getInstance().addAiResponse(context);
        resultTask.complete(PromptResult.success(context));
    }

    private void finalizeError(int exitCode,
                               @NotNull String exitMsg,
                               @NotNull List<String> stderrLines,
                               @NotNull CliConsoleManager consoleManager,
                               @NotNull ChatMessageContext context,
                               @NotNull PromptTask<PromptResult> resultTask) {
        consoleManager.printError(exitMsg);
        String errorOutput = String.join("\n", stderrLines).trim();
        resultTask.complete(PromptResult.failure(context,
                new RuntimeException("CLI process exited with code " + exitCode +
                        (errorOutput.isEmpty() ? "" : ": " + errorOutput))));
    }

    private void handleProcessStartFailure(@NotNull IOException e,
                                           @NotNull CliConsoleManager consoleManager,
                                           @NotNull ChatMessageContext context,
                                           @NotNull PromptTask<PromptResult> resultTask) {
        log.error("CLI chat failed to start process: {}", e.getMessage(), e);
        ApplicationManager.getApplication().invokeLater(() -> {
            consoleManager.printError("Failed to start process: " + e.getMessage());
            resultTask.complete(PromptResult.failure(context, e));
        });
    }

    private void handleProcessInterrupted(@NotNull CliConsoleManager consoleManager,
                                          @NotNull PromptTask<PromptResult> resultTask) {
        Thread.currentThread().interrupt();
        log.info("CLI chat process interrupted");
        ApplicationManager.getApplication().invokeLater(() -> {
            consoleManager.printSystem("Process interrupted");
            resultTask.cancel(true);
        });
    }

    @Override
    public void cancel() {
        log.info("Cancelling CLI Runner strategy");
        destroyProcess();
    }

    private void destroyProcess() {
        Process process = activeProcess.getAndSet(null);
        if (process != null && process.isAlive()) {
            log.info("Destroying CLI chat process (pid={})", process.pid());
            process.destroyForcibly();
            ApplicationManager.getApplication().invokeLater(() ->
                    CliConsoleManager.getInstance(project).printSystem("\n=== Process cancelled ===\n"));
        }
    }

    private @Nullable CliToolConfig findCliTool(String toolName) {
        return DevoxxGenieStateService.getInstance().getCliTools().stream()
                .filter(CliToolConfig::isEnabled)
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }

}
