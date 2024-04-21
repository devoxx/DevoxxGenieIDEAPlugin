package com.devoxx.genie.ui;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.devoxx.genie.ui.CommandHandler.*;
import static org.mockito.Mockito.*;

class CommandHandlerTest {

    @Mock
    private CommandHandlerListener listener;

    @Mock
    private Application application;

    private SettingsState settingsState = new SettingsState();

    private CommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks

        // Prepare the ApplicationManager mock to return the mocked application
        // Mockito.mockStatic(ApplicationManager.class);
        ApplicationManager.setApplication(application);

        // Mock getting the SettingsState service from the application
        when(application.getService(SettingsState.class)).thenReturn(settingsState);

        // Now, we can create an instance of CommandHandler
        handler = new CommandHandler(listener);
    }

    @Test
    void handleHelpCommand() {
        handler.handleCommand(CommandHandler.COMMAND_HELP);
        verify(listener, times(1)).showHelp();
    }

    @Test
    void test_HandleTestCommand() {
        handler.handleCommand(COMMAND_TEST);
        verify(listener, times(1))
            .executePrompt(COMMAND_TEST, "Write a unit test for this code using JUnit.");
    }

    @Test
    void test_HandleReviewCommand() {
        handler.handleCommand(COMMAND_REVIEW);
        verify(listener, times(1))
            .executePrompt(COMMAND_REVIEW, "Review the selected code, can it be improved or are there bugs?");
    }

    @Test
    void testHandleExplainCommand() {
        handler.handleCommand(COMMAND_EXPLAIN);
        verify(listener, times(1))
            .executePrompt(COMMAND_EXPLAIN, "Explain the code so a junior developer can understand it.");
    }

    @Test
    void testHandleUnknownCommand() {
        handler.handleCommand("/unknown");
        verify(listener, times(1)).showHelp(); // Assuming showHelp is the fallback for unknown commands
    }
}
