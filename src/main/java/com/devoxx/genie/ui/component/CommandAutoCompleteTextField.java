package com.devoxx.genie.ui.component;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class CommandAutoCompleteTextField extends JBTextArea implements CustomPromptChangeListener {

    private static final Logger LOG = Logger.getInstance(CommandAutoCompleteTextField.class);

    private final List<String> commands = new ArrayList<>();
    private boolean isAutoCompleting = false;

    private String placeholder = "";

    public CommandAutoCompleteTextField() {
        super();

        setDocument(new CommandDocument());
        addKeyListener(new CommandKeyListener());

        ApplicationManager.getApplication()
            .getMessageBus()
            .connect()
            .subscribe(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC, this);

        initializeCommands();
    }

    private void initializeCommands() {
        commands.clear();
        commands.add("/test");
        commands.add("/explain");
        commands.add("/review");

        DevoxxGenieSettingsService stateService = DevoxxGenieSettingsServiceProvider.getInstance();
        for (CustomPrompt customPrompt : stateService.getCustomPrompts()) {
            commands.add("/" + customPrompt.getName());
        }
    }

    private class CommandKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(@NotNull KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) {
                sendPrompt();
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
                autoComplete();
                e.consume();
            }
        }
    }

    private void sendPrompt() {
        String text = getText().trim();
        if (!text.isEmpty()) {
            ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC_TOPIC)
                .onPromptSubmitted(text);
        }
    }

    private void autoComplete() {
        String text = getText();
        String[] lines = text.split("\n");
        if (lines.length > 0 && lines[lines.length - 1].startsWith("/")) {
            String currentLine = lines[lines.length - 1];
            for (String command : commands) {
                if (command.startsWith(currentLine)) {
                    isAutoCompleting = true;
                    int start = text.lastIndexOf("\n") + 1;
                    try {
                        getDocument().remove(start, currentLine.length());
                        getDocument().insertString(start, command, null);
                    } catch (BadLocationException ex) {
                        LOG.error("Error while auto-completing command", ex);
                    }
                    setCaretPosition(text.length() - currentLine.length() + command.length());
                    isAutoCompleting = false;
                    break;
                }
            }
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

            SwingUtilities.invokeLater(() -> {
                try {
                    String text = getText(0, getLength());
                    String[] lines = text.split("\n");
                    if (lines.length > 0) {
                        String currentLine = lines[lines.length - 1];
                        if (currentLine.startsWith("/") && currentLine.length() > 1) {
                            for (String command : commands) {
                                if (command.startsWith(currentLine) && !command.equals(currentLine)) {
                                    String completion = command.substring(currentLine.length());
                                    isAutoCompleting = true;
                                    insertString(getLength(), completion, null);
                                    setCaretPosition(getLength());
                                    moveCaretPosition(getLength() - completion.length());
                                    isAutoCompleting = false;
                                    break;
                                }
                            }
                        }
                    }
                } catch (BadLocationException e) {
                    LOG.error("Error while auto-completing command", e);
                }
            });
        }
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

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
}
