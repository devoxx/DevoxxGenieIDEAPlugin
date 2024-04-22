package com.devoxx.genie.ui;

public interface CommandHandlerListener {
    void executePrompt(String command, String query);
    void showHelpMsg();
}
