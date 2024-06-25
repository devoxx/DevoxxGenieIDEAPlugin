package com.devoxx.genie.ui.component;

import com.devoxx.genie.service.PromptExecutionService;
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
        if (text.startsWith("/")) {
            String prefix = text.substring(1);
            for (String command : commands) {
                if (command.startsWith(prefix)) {
                    isAutoCompleting = true;
                    setText(command);
                    setCaretPosition(command.length());
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

            if (offs == 0 && !str.startsWith("/")) {
                str = "/" + str;
            }
            super.insertString(offs, str, a);

            SwingUtilities.invokeLater(() -> {
                try {
                    String text = getText(0, getLength());
                    if (text.startsWith("/") && text.length() > 1) {
                        for (String command : commands) {
                            if (command.startsWith(text) && !command.equals(text)) {
                                int currentLength = text.length();
                                String completion = command.substring(currentLength);
                                isAutoCompleting = true;
                                insertString(currentLength, completion, null);
                                setCaretPosition(getLength());
                                moveCaretPosition(currentLength);
                                isAutoCompleting = false;
                                break;
                            }
                        }
                    }
                } catch (BadLocationException e) {
                    LOG.debug("Error while auto-completing command", e);
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
