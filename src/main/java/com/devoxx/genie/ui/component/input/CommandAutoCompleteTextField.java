package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;

@Slf4j
public class CommandAutoCompleteTextField extends JBTextArea implements CustomPromptChangeListener {

    private final List<String> commands = new ArrayList<>();
    private final transient Project project;

    private boolean isAutoCompleting = false;
    @Getter
    private String placeholder = "";
    
    private Runnable fileSelectionCallback;

    public CommandAutoCompleteTextField(@NotNull Project project) {
        super();
        this.project = project;
        setDocument(new CommandDocument());
        addKeyListener(new CommandKeyListener());

        project.getMessageBus()
                .connect()
                .subscribe(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC, this);

        initializeCommands();
    }

    private void initializeCommands() {
        commands.clear();
        commands.add(COMMAND_PREFIX + TEST_COMMAND);
        commands.add(COMMAND_PREFIX + EXPLAIN_COMMAND);
        commands.add(COMMAND_PREFIX + REVIEW_COMMAND);
        commands.add(COMMAND_PREFIX + TDG_COMMAND);
        commands.add(COMMAND_PREFIX + HELP_COMMAND);
        commands.add(COMMAND_PREFIX + INIT_COMMAND);

        DevoxxGenieSettingsService stateService = DevoxxGenieStateService.getInstance();
        for (CustomPrompt customPrompt : stateService.getCustomPrompts()) {
            commands.add(COMMAND_PREFIX + customPrompt.getName());
        }
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }
    
    public void setFileSelectionCallback(Runnable fileSelectionCallback) {
        this.fileSelectionCallback = fileSelectionCallback;
    }

    @Override
    public void onCustomPromptsChanged() {
        initializeCommands();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && !placeholder.isEmpty()) {
            g.setColor(JBColor.GRAY);
            g.drawString(placeholder, getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
        }
    }

    private class CommandKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(@NotNull KeyEvent e) {
            KeyStroke currentKeyStroke = KeyStroke.getKeyStrokeForEvent(e);
            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

            String submitShortcutString;
            String newlineShortcutString;
            
            if (SystemInfo.isMac) {
                submitShortcutString = stateService.getSubmitShortcutMac();
                newlineShortcutString = stateService.getNewlineShortcutMac();
            } else if (SystemInfo.isLinux) {
                submitShortcutString = stateService.getSubmitShortcutLinux();
                newlineShortcutString = stateService.getNewlineShortcutLinux();
            } else {
                submitShortcutString = stateService.getSubmitShortcutWindows();
                newlineShortcutString = stateService.getNewlineShortcutWindows();
            }

            KeyStroke submitKeyStroke = KeyStroke.getKeyStroke(submitShortcutString);
            KeyStroke newlineKeyStroke = KeyStroke.getKeyStroke(newlineShortcutString);
            
            if (submitKeyStroke != null && submitKeyStroke.equals(currentKeyStroke)) {
                sendPrompt();
                e.consume();
            } else if (newlineKeyStroke != null && newlineKeyStroke.equals(currentKeyStroke)) {
                insertNewline();
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
                autoComplete();
                e.consume();
            } else if (e.getKeyChar() == '@') {
                if (shouldShowFileSelection()) {
                    if (fileSelectionCallback != null) {
                        fileSelectionCallback.run();
                        e.consume();
                    }
                }
            }
        }

        private void sendPrompt() {
            String text = getText().trim();
            if (!text.isEmpty()) {
                project.getMessageBus()
                        .syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)
                        .onPromptSubmitted(project, text);
            }
        }

        private void autoComplete() {
            String text = getText();
            String[] lines = text.split("\n");
            if (lines.length > 0 && lines[lines.length - 1].startsWith(COMMAND_PREFIX)) {
                String currentLine = lines[lines.length - 1];
                for (String command : commands) {
                    if (command.startsWith(currentLine)) {
                        isAutoCompleting = true;
                        int start = text.lastIndexOf("\n") + 1;
                        try {
                            getDocument().remove(start, currentLine.length());
                            getDocument().insertString(start, command, null);
                        } catch (BadLocationException ex) {
                            log.error("Error while auto-completing command", ex);
                        }
                        setCaretPosition(text.length() - currentLine.length() + command.length());
                        isAutoCompleting = false;
                        break;
                    }
                }
            }
        }
        
        private void insertNewline() {
            try {
                getDocument().insertString(getCaretPosition(), "\n", null);
            } catch (BadLocationException ex) {
                log.error("Error inserting newline", ex);
            }
        }
        
        private boolean shouldShowFileSelection() {
            int caretPosition = getCaretPosition();
            String text = getText();
            
            // Check if @ is at the beginning of the text
            if (caretPosition == 0) {
                return true;
            }
            
            // Check if the character before the caret position is a space
            if (caretPosition > 0 && caretPosition <= text.length()) {
                char prevChar = text.charAt(caretPosition - 1);
                return prevChar == ' ' || prevChar == '\n' || prevChar == '\t';
            }
            
            return false;
        }
    }

    private class CommandDocument extends PlainDocument {
        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (isAutoCompleting) {
                super.insertString(offs, str, a);
                return;
            }

            super.insertString(offs, str, a);
            scheduleAutoComplete();
        }

        private void scheduleAutoComplete() {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    handleAutoComplete();
                } catch (BadLocationException e) {
                    log.error("Error while auto-completing command", e);
                }
            });
        }

        private void handleAutoComplete() throws BadLocationException {
            String text = getText(0, getLength());
            String currentLine = getCurrentLine(text);

            if (shouldAttemptAutoComplete(currentLine)) {
                attemptAutoComplete(currentLine);
            }
        }

        private String getCurrentLine(@NotNull String text) {
            String[] lines = text.split("\n");
            return lines.length > 0 ? lines[lines.length - 1] : "";
        }

        private boolean shouldAttemptAutoComplete(@NotNull String currentLine) {
            return currentLine.startsWith(COMMAND_PREFIX) && currentLine.length() > 1;
        }

        private void attemptAutoComplete(String currentLine) throws BadLocationException {
            for (String command : commands) {
                if (command.startsWith(currentLine) && !command.equals(currentLine)) {
                    applyAutoComplete(command, currentLine);
                    break;
                }
            }
        }

        private void applyAutoComplete(@NotNull String command,
                                       @NotNull String currentLine) throws BadLocationException {
            String completion = command.substring(currentLine.length());
            isAutoCompleting = true;
            insertString(getLength(), completion, null);
            setCaretPosition(getLength());
            moveCaretPosition(getLength() - completion.length());
            isAutoCompleting = false;
        }
    }
}
