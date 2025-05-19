package com.devoxx.genie.ui.webview;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Factory for creating JCef browser instances or fallback components when JCEF is not available.
 * This class handles the initialization of JCEF components for the plugin.
 */
@Slf4j
public class WebViewFactory {

    private WebViewFactory() {
        // Utility class, no instances needed
    }

    /**
     * Creates a fallback text component when JCEF is not available.
     * 
     * @param message The message to display in the fallback component
     * @return A JComponent that can be used instead of the browser
     */
    public static @NotNull JComponent createFallbackComponent(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Main text area
        JBTextArea textArea = new JBTextArea();
        textArea.setText(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(JBUI.Borders.empty(10, 0, 10, 0));
        
        // Instructions panel with a titled border
        JPanel instructionsPanel = new JPanel(new BorderLayout());
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "How to Enable JCEF Support"
        );
        Font titleFont = UIUtil.getLabelFont().deriveFont(Font.BOLD);
        titledBorder.setTitleFont(titleFont);
        instructionsPanel.setBorder(titledBorder);
        
        // Instruction steps as HTML
        JEditorPane instructionsPane = new JEditorPane();
        instructionsPane.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        instructionsPane.setEditable(false);
        instructionsPane.setOpaque(false);
        instructionsPane.setText(
                "<html><body style='margin: 10px; font-family: sans-serif;'>" +
                "<ol>" +
                "<li>Go to <b>Help > Find Action</b> in the menu</li>" +
                "<li>Type \"<b>Choose Boot Java Runtime for the IDE</b>\" and select it</li>" +
                "<li>From the dropdown menu, select a runtime with <b>JCEF support</b></li>" +
                "<li>Click <b>OK</b> and restart the IDE</li>" +
                "</ol>" +
                "<p><i>Note: Changing the runtime may cause unexpected issues in some cases.</i></p>" +
                "</body></html>"
        );
        
        instructionsPanel.add(instructionsPane, BorderLayout.CENTER);
        
        // Assemble all components
        panel.add(textArea, BorderLayout.NORTH);
        panel.add(instructionsPanel, BorderLayout.CENTER);
        
        // Wrap everything in a scroll pane
        JBScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setMinimumSize(new Dimension(400, 300));
        return scrollPane;
    }
}
