package com.devoxx.genie.ui;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {

    private final Map<String, Runnable> commandMap = new HashMap<>();

    public static final String COMMAND_HELP = "/help";
    public static final String COMMAND_TEST = "/test";
    public static final String COMMAND_REVIEW = "/review";
    public static final String COMMAND_EXPLAIN = "/explain";
    public static final String COMMAND_CUSTOM = "/custom";

    private final CommandHandlerListener listener;

    /**
     * The command handler constructor.
     * @param listener the command handler listener
     */
    public CommandHandler(CommandHandlerListener listener) {
        this.listener = listener;
        initializeCommands();
    }

    /**
     * Initialize the available commands.
     */
    private void initializeCommands() {
        SettingsState settings = SettingsState.getInstance();

        commandMap.put(COMMAND_HELP, listener::showHelpMsg);

        commandMap.put(COMMAND_TEST,
            () -> listener.executePrompt(COMMAND_TEST, settings.getTestPrompt()));

        commandMap.put(COMMAND_REVIEW,
            () -> listener.executePrompt(COMMAND_REVIEW, settings.getReviewPrompt()));

        commandMap.put(COMMAND_EXPLAIN,
            () -> listener.executePrompt(COMMAND_EXPLAIN, settings.getExplainPrompt()));

        commandMap.put(COMMAND_CUSTOM,
            () -> listener.executePrompt(COMMAND_CUSTOM, settings.getCustomPrompt()));
    }

    /**
     * Handle the command input.
     * @param commandInput the command input
     */
    public void handleCommand(String commandInput) {
        Runnable command = commandMap.get(commandInput);
        if (command != null) {
            command.run();
        } else {
            listener.showHelpMsg();
        }
    }
}
