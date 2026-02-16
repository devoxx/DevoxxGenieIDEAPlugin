package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.spec.AcpToolConfig;
import com.devoxx.genie.service.acp.protocol.AcpClient;
import com.devoxx.genie.service.cli.CliConsoleManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Prompt execution strategy for ACP Runners.
 * Uses the ACP (Agent Communication Protocol) to communicate with external agents
 * via JSON-RPC 2.0 over stdin/stdout, providing structured streaming, file operations,
 * terminal management, and capability negotiation.
 */
@Slf4j
public class AcpPromptStrategy extends AbstractPromptExecutionStrategy {

    private AtomicReference<AcpClient> activeClient = new AtomicReference<>();

    public AcpPromptStrategy(@NotNull Project project) {
        super(project);
    }

    @Override
    protected String getStrategyName() {
        return "ACP Runner";
    }

    @Override
    protected void executeStrategySpecific(@NotNull ChatMessageContext context,
                                           @NotNull PromptOutputPanel panel,
                                           @NotNull PromptTask<PromptResult> resultTask) {
        String toolName = context.getLanguageModel().getModelName();
        AcpToolConfig acpTool = findAcpTool(toolName);
        if (acpTool == null) {
            log.error("ACP tool not found: {}", toolName);
            resultTask.complete(PromptResult.failure(context,
                    new IllegalStateException("ACP tool not found: " + toolName)));
            return;
        }

        String prompt = buildPromptWithHistory(context);
        String executablePath = acpTool.getExecutablePath();

        log.info("ACP execute: tool={}, executable={}", toolName, executablePath);

        CliConsoleManager consoleManager = CliConsoleManager.getInstance(project);
        consoleManager.printTaskHeader("acp-chat", prompt.length() > 60 ? prompt.substring(0, 60) + "..." : prompt, toolName);
        consoleManager.activateToolWindow();

        ConversationWebViewController webViewController =
                panel.getConversationPanel() != null ? panel.getConversationPanel().webViewController : null;

        threadPoolManager.getPromptExecutionPool().execute(() ->
                runAcpSession(context, acpTool, prompt, consoleManager, webViewController, resultTask));

        resultTask.whenComplete((result, error) -> {
            if (resultTask.isCancelled()) {
                closeClient();
            }
        });
    }

    private void runAcpSession(@NotNull ChatMessageContext context,
                                @NotNull AcpToolConfig acpTool,
                                @NotNull String prompt,
                                @NotNull CliConsoleManager consoleManager,
                                @Nullable ConversationWebViewController webViewController,
                                @NotNull PromptTask<PromptResult> resultTask) {
        long startTime = System.currentTimeMillis();
        StringBuilder accumulatedResponse = new StringBuilder();

        try {
            AcpClient client = AcpClient.builder()
                    .outputConsumer(textChunk -> {
                        accumulatedResponse.append(textChunk);
                        final String fullText = accumulatedResponse.toString();

                        ApplicationManager.getApplication().invokeLater(() -> {
                            consoleManager.printOutput(textChunk);

                            if (webViewController != null && !fullText.isEmpty()) {
                                context.setAiMessage(AiMessage.from(fullText));
                                webViewController.updateAiMessageContent(context);
                            }
                        });
                    })
                    .build();
            activeClient.set(client);

            String basePath = project.getBasePath();
            File cwd = basePath != null ? new File(basePath) : null;

            consoleManager.printSystem("[ACP] Starting " + acpTool.getName() + "...");
            String acpFlag = acpTool.getAcpFlag() != null ? acpTool.getAcpFlag() : "acp";
            client.start(cwd, acpTool.getExecutablePath(), acpFlag);

            consoleManager.printSystem("[ACP] Initializing protocol...");
            client.initialize();

            consoleManager.printSystem("[ACP] Creating session...");
            client.createSession(basePath != null ? basePath : System.getProperty("user.dir"));

            consoleManager.printSystem("[ACP] Sending prompt...");
            client.sendPrompt(prompt);

            activeClient.set(null);
            client.close();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("ACP session completed: elapsed={}ms, responseLength={}", elapsed, accumulatedResponse.length());

            ApplicationManager.getApplication().invokeLater(() -> {
                String exitMsg = "\n=== ACP session completed (after " + elapsed + "ms) ===\n";
                consoleManager.printSystem(exitMsg);

                context.setExecutionTimeMs(elapsed);
                String finalText = accumulatedResponse.toString();
                if (!finalText.isEmpty()) {
                    context.setAiMessage(AiMessage.from(finalText));
                }

                if (webViewController != null) {
                    webViewController.updateAiMessageContent(context);
                    webViewController.markMCPLogsAsCompleted(context.getId());
                }

                ChatMemoryManager.getInstance().addAiResponse(context);
                resultTask.complete(PromptResult.success(context));
            });

        } catch (Exception e) {
            activeClient = null;
            log.error("ACP session failed: {}", e.getMessage(), e);
            ApplicationManager.getApplication().invokeLater(() -> {
                consoleManager.printError("[ACP] Error: " + e.getMessage());
                resultTask.complete(PromptResult.failure(context, e));
            });
        }
    }

    @Override
    public void cancel() {
        log.info("Cancelling ACP Runner strategy");
        closeClient();
    }

    private void closeClient() {
        AcpClient client = activeClient.getAndSet(null);
        if (client != null) {
            log.info("Closing ACP client");
            client.close();
            ApplicationManager.getApplication().invokeLater(() ->
                    CliConsoleManager.getInstance(project).printSystem("\n=== ACP session cancelled ===\n"));
        }
    }

    private @Nullable AcpToolConfig findAcpTool(String toolName) {
        return DevoxxGenieStateService.getInstance().getAcpTools().stream()
                .filter(AcpToolConfig::isEnabled)
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }
}
