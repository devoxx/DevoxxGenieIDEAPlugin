package com.devoxx.genie.service.spec;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Manages an IntelliJ Run tool window console for CLI task runner output.
 * Creates/reuses a ConsoleView in the Run tool window so users can watch
 * CLI progress in real-time.
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class CliConsoleManager implements Disposable {

    private static final String CONTENT_DISPLAY_NAME = "Spec CLI Runner";
    private static final String TOOL_WINDOW_ID = "Run";

    private final Project project;
    private ConsoleView consoleView;

    public CliConsoleManager(@NotNull Project project) {
        this.project = project;
    }

    public static CliConsoleManager getInstance(@NotNull Project project) {
        return project.getService(CliConsoleManager.class);
    }

    /**
     * Get or create the console view, ensuring it is attached to the Run tool window.
     */
    public @NotNull ConsoleView getOrCreateConsole() {
        if (consoleView == null) {
            consoleView = TextConsoleBuilderFactory.getInstance()
                    .createBuilder(project)
                    .getConsole();
            attachToRunToolWindow();
        }
        return consoleView;
    }

    /**
     * Print a task header line to the console.
     */
    public void printTaskHeader(@NotNull String taskId, @NotNull String title, @NotNull String toolName) {
        ConsoleView console = getOrCreateConsole();
        console.print(
                "\n=== Running " + taskId + ": " + title + " via " + toolName + " ===\n\n",
                ConsoleViewContentType.SYSTEM_OUTPUT
        );
    }

    /**
     * Print a line of process output.
     */
    public void printOutput(@NotNull String line) {
        ConsoleView console = getOrCreateConsole();
        console.print(line + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    /**
     * Print an error line.
     */
    public void printError(@NotNull String line) {
        ConsoleView console = getOrCreateConsole();
        console.print(line + "\n", ConsoleViewContentType.ERROR_OUTPUT);
    }

    /**
     * Print a system message (e.g., process exit info).
     */
    public void printSystem(@NotNull String message) {
        ConsoleView console = getOrCreateConsole();
        console.print(message + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    }

    /**
     * Activate the Run tool window so the console is visible.
     */
    public void activateToolWindow() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.show();
            // Select our content tab
            ContentManager cm = toolWindow.getContentManager();
            Content content = cm.findContent(CONTENT_DISPLAY_NAME);
            if (content != null) {
                cm.setSelectedContent(content);
            }
        }
    }

    private void attachToRunToolWindow() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            log.warn("Run tool window not found, console output will not be visible");
            return;
        }

        ContentManager cm = toolWindow.getContentManager();

        // Remove existing content with same name to avoid duplicates
        Content existing = cm.findContent(CONTENT_DISPLAY_NAME);
        if (existing != null) {
            cm.removeContent(existing, true);
        }

        Content content = ContentFactory.getInstance().createContent(
                consoleView.getComponent(), CONTENT_DISPLAY_NAME, false);
        content.setDisposer(this);
        cm.addContent(content);
    }

    @Override
    public void dispose() {
        if (consoleView != null) {
            consoleView.dispose();
            consoleView = null;
        }
    }
}
