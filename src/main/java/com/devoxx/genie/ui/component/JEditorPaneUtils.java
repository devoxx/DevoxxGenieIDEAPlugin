package com.devoxx.genie.ui.component;

import com.intellij.openapi.editor.impl.EditorCssFontResolver;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.List;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_BG_COLOR;

public class JEditorPaneUtils {

    private JEditorPaneUtils() {
    }

    /**
     * Creates a JEditorPane tailored for HTML content, utilizing IntelliJ API to enhance display and interaction.
     * Features include word wrapping, icons, and support for Base64 images.
     */
    public static @NotNull JEditorPane createHtmlJEditorPane(@NotNull CharSequence content,
                                                             HyperlinkListener hyperlinkListener,
                                                             StyleSheet styleSheet) {
        JEditorPane editorPane = new JEditorPane();
        editorPane.addHyperlinkListener(hyperlinkListener != null ? hyperlinkListener : BrowserHyperlinkListener.INSTANCE);
        editorPane.setContentType("text/html");

        HTMLEditorKitBuilder htmlEditorKitBuilder =
            new HTMLEditorKitBuilder()
                .withWordWrapViewFactory()
                .withFontResolver(EditorCssFontResolver.getGlobalInstance());

        HTMLEditorKit editorKit = htmlEditorKitBuilder.build();
        editorKit.getStyleSheet().addStyleSheet(styleSheet);
        editorPane.setEditorKit(editorKit);

        editorPane.setEditable(false);
        editorPane.setForeground(JBColor.foreground());
        editorPane.setBackground(PROMPT_BG_COLOR);
        editorPane.setText(colorizeSeparators(content.toString()));

        UIUtil.doNotScrollToCaret(editorPane);
        UIUtil.invokeLaterIfNeeded(() -> {
            editorPane.revalidate();
            editorPane.setCaretPosition(editorPane.getDocument().getLength());
        });
        return editorPane;
    }

    @Contract(pure = true)
    private static String colorizeSeparators(String html) {
        String body = UIUtil.getHtmlBody(html);
        List<String> parts = StringUtil.split(body, UIUtil.BORDER_LINE, true, false);
        if (parts.size() <= 1) return html;

        return StringUtil.join(parts, part -> {
            if (part.equals(parts.get(0))) {
                return part;
            } else {
                return "<div style='margin-top:6; padding-top:6; border-top: thin solid #" +
                    ColorUtil.toHex(UIUtil.getTooltipSeparatorColor()) + "'>" + part + "</div>";
            }
        }, "");
    }
}

