package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panel for displaying MCP messages within a chat response
 */
@Slf4j
public class MCPMessagePanel extends JPanel implements MCPLoggingMessage, Disposable {
    
    private static final int MAX_MESSAGES = 10; // Maximum number of messages to show
    private static final String TITLE = "MCP Tool Messages";
    
    private final JTextArea messageArea;
    private final List<String> messages = new ArrayList<>();
    private final AtomicBoolean isSubscribed = new AtomicBoolean(false);
    
    /**
     * Constructor for MCPMessagePanel
     *
     * @param chatMessageContext The chat message context
     */
    public MCPMessagePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(new BorderLayout());
        Project project = chatMessageContext.getProject();
        
        // Create a text area with monospaced font for displaying messages
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        messageArea.setBackground(new JBColor(new Color(240, 240, 240), new Color(45, 45, 45)));
        
        // Set up scrolling
        JBScrollPane scrollPane = new JBScrollPane(messageArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, 120));
        
        // Add titled border
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                TITLE,
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Subscribe to MCP logging messages
        ApplicationManager.getApplication().invokeLater(() -> {
            MessageBusUtil.connect(project, connection -> {
                MessageBusUtil.subscribe(connection, AppTopics.MCP_LOGGING_MSG, this);
                isSubscribed.set(true);
                Disposer.register(this, connection);
            });
        });
    }
    
    @Override
    public void onMCPLoggingMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        // Process and add the message
        ApplicationManager.getApplication().invokeLater(() -> {
            // Add the new message
            messages.add(message);
            
            // Limit the number of messages
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
            
            // Update the display
            updateMessageDisplay();
        });
    }
    
    /**
     * Update the message display with current messages
     */
    private void updateMessageDisplay() {
        StringBuilder displayContent = new StringBuilder();
        
        for (String msg : messages) {
            // Format the message - add a prefix indicator for direction
            if (msg.startsWith("<")) {
                displayContent.append("← ").append(msg.substring(1).trim()).append("\n\n");
            } else if (msg.startsWith(">")) {
                displayContent.append("→ ").append(msg.substring(1).trim()).append("\n\n");
            } else {
                displayContent.append(msg).append("\n\n");
            }
        }
        
        messageArea.setText(displayContent.toString());
        
        // Scroll to bottom
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }
    
    @Override
    public void dispose() {
        // Clean up resources
        messages.clear();
    }
}
