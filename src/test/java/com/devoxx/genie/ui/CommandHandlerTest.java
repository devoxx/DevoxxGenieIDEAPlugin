package com.devoxx.genie.ui;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class CommandHandlerTest {

    private CommandHandlerListener listener;
    private CommandHandler handler;

    @BeforeEach
    void setUp() {
        listener = mock(CommandHandlerListener.class);
        handler = new CommandHandler(listener);
    }

    @Test
    void handleHelpCommand() {
        handler.handleCommand(CommandHandler.COMMAND_HELP);
        verify(listener, times(1)).showHelp();
    }

    @Test
    void test_HandleTestCommand() {
        handler.handleCommand(CommandHandler.COMMAND_TEST);
        verify(listener, times(1)).executePrompt("Write a unit test for this code using JUnit.");
    }

    @Test
    void test_HandleReviewCommand() {
        handler.handleCommand(CommandHandler.COMMAND_REVIEW);
        verify(listener, times(1)).executePrompt("Review the selected code, can it be improved or are there bugs?");
    }

    @Test
    void testHandleExplainCommand() {
        handler.handleCommand(CommandHandler.COMMAND_EXPLAIN);
        verify(listener, times(1)).executePrompt("Explain the code so a junior developer can understand it.");
    }

    @Test
    void testHandleUnknownCommand() {
        handler.handleCommand("/unknown");
        verify(listener, times(1)).showHelp(); // Assuming showHelp is the fallback for unknown commands
    }
}
