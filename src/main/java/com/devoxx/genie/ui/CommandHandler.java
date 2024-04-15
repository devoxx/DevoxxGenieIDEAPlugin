package com.devoxx.genie.ui;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {

    private final Map<String, Runnable> commandMap = new HashMap<>();

    public static final String COMMAND_HELP = "/help";
    public static final String COMMAND_TEST = "/test";
    public static final String COMMAND_REVIEW = "/review";
    public static final String COMMAND_EXPLAIN = "/explain";

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
        commandMap.put(COMMAND_HELP, listener::showHelp);

        commandMap.put(COMMAND_TEST,
            () -> listener.executePrompt("Write a unit test for this code using JUnit."));

        commandMap.put(COMMAND_REVIEW,
            () -> listener.executePrompt("Review the selected code, can it be improved or are there bugs?"));

        commandMap.put(COMMAND_EXPLAIN,
            () -> listener.executePrompt("Explain the code so a junior developer can understand it."));
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
            listener.showHelp();
        }
    }
}
