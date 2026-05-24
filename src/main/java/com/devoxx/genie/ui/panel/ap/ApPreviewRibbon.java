package com.devoxx.genie.ui.panel.ap;

import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Compact horizontal banner that flags the Docker Agentic Platform integration as
 * an "Early Preview" feature. Used at the top of both the settings page and the
 * tool window tab so users see the disclaimer in both surfaces.
 */
public final class ApPreviewRibbon extends JPanel {

    private static final String MESSAGE_PREFIX =
            "🧪  Early Preview — APIs, storage, and behavior may change. ";
    private static final String FEEDBACK_TEXT = "Feedback welcome.";
    private static final String ISSUES_URL = "https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues";

    public ApPreviewRibbon() {
        super(new BorderLayout());

        Color bg = JBColor.namedColor("Notification.background", new JBColor(0xFFF6D8, 0x4F4527));
        Color fg = JBColor.namedColor("Label.foreground", JBColor.foreground());
        Color border = JBColor.namedColor("Notification.borderColor", new JBColor(0xE5C158, 0x7A6932));

        setBackground(bg);
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, border),
                JBUI.Borders.empty(4, 10)));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
        content.setOpaque(false);

        JBLabel prefix = new JBLabel(MESSAGE_PREFIX);
        prefix.setForeground(fg);
        prefix.setFont(prefix.getFont().deriveFont(Font.PLAIN));

        HyperlinkLabel feedbackLink = new HyperlinkLabel(FEEDBACK_TEXT);
        feedbackLink.setHyperlinkTarget(ISSUES_URL);
        feedbackLink.setToolTipText("Open the DevoxxGenie issue tracker on GitHub");

        content.add(prefix);
        content.add(feedbackLink);
        add(content, BorderLayout.WEST);
    }
}
