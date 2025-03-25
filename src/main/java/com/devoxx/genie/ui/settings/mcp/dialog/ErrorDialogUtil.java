package com.devoxx.genie.ui.settings.mcp.dialog;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for displaying error dialogs
 */
public class ErrorDialogUtil {
    
    /**
     * Shows an error dialog with detailed information
     *
     * @param parent    The parent component
     * @param title     Dialog title
     * @param message   Error message
     * @param exception The exception that occurred
     */
    public static void showErrorDialog(Component parent, String title, String message, Exception exception) {
        // Create a text area for the stack trace
        JTextArea textArea = new JTextArea(15, 50);
        textArea.setEditable(false);
        
        // Prepare detailed error message
        StringBuilder detailedMessage = new StringBuilder();
        detailedMessage.append(message).append("\n\n");
        
        // Add stack trace if exception is provided
        if (exception != null) {
            // Root cause message
            Throwable cause = exception;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            detailedMessage.append("Root cause: ").append(cause.getMessage()).append("\n\n");
            
            // Full stack trace for debugging
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            detailedMessage.append("Stack trace:\n").append(sw);
        }
        
        textArea.setText(detailedMessage.toString());
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        // Show the error dialog
        JOptionPane.showMessageDialog(
                parent,
                scrollPane,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }
}
