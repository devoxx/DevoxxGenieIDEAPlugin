package com.devoxx.genie.ui.component;

import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class CommandAutoCompleteTextField extends PlaceholderTextArea {

    private static final Logger LOG = Logger.getInstance(CommandAutoCompleteTextField.class);

    private final List<String> commands = new ArrayList<>();
    private boolean isAutoCompleting = false;

    public CommandAutoCompleteTextField() {
        super();
        initializeCommands();
        setDocument(new CommandDocument());
        addKeyListener(new CommandKeyListener());
    }

    private void initializeCommands() {
        commands.add("/test");
        commands.add("/explain");
        commands.add("/review");
        commands.add("/custom");
        // Add more commands as needed
    }

    private class CommandKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
                autoComplete();
                e.consume();
            }
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
                        ex.printStackTrace();
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
                                    int start = text.lastIndexOf("\n") + 1;
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
                    e.printStackTrace();
                }
            });
        }
    }

    // Placeholder functionality
    private String placeholder = "";

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && !placeholder.isEmpty()) {
            g.setColor(java.awt.Color.GRAY);
            g.drawString(placeholder, getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
        }
    }
}
