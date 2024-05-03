package com.devoxx.genie.ui.component

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.impl.EditorCssFontResolver
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.util.ui.*
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.annotations.Contract
import java.awt.Dimension
import javax.swing.JEditorPane
import javax.swing.UIManager
import javax.swing.event.HyperlinkListener

/**
 * Creates a [JEditorPane] tailored for HTML content with the given HTML [content] and [maxWidth].
 *
 * This [JEditorPane] uses the extended IntelliJ [ExtendableHTMLViewFactory] to support:
 * - word wrapping
 * - icons (from IJ)
 * - Base 64 images
 * The [extensions] parameter can be used to add more extensions to the [ExtendableHTMLViewFactory].
 *
 * Also, the [JEditorPane] is configured to use the [EditorCssFontResolver] to use the same font
 * as the current [com.intellij.openapi.editor.Editor].
 * Some rules are also added to tweak the size of some HTML elements.
 *
 *
 * @param content the HTML content to display
 * @param maxWidth the maximum width to use for the [JEditorPane.preferredSize]
 * @param extensions the list of [ExtendableHTMLViewFactory.Extension] to use for the [JEditorPane]
 *
 * FIX_WHEN_MIN_242: Remove this and switch to JBHtmlPane
 */
fun htmlJEditorPane(
    content: CharSequence,
    maxWidth: Int? = null,
    extensions: List<ExtendableHTMLViewFactory.Extension> = emptyList(),
    hyperlinkListener: HyperlinkListener = BrowserHyperlinkListener.INSTANCE,
): JEditorPane {
    return JEditorPane().apply {
        // Setting the hyperlink listener **before**, so it's possible to override
        // listeners installed by the editor kit, in particular HTMLEditorKitBuilder
        // installs some default listeners (see : com.intellij.util.ui.JBHtmlEditorKit.install)
        addHyperlinkListener(hyperlinkListener)

        contentType = "text/html"
        editorKit = HTMLEditorKitBuilder()
            .withViewFactoryExtensions(
                ExtendableHTMLViewFactory.Extensions.WORD_WRAP,
                *extensions.toTypedArray()
            )
            .withFontResolver(EditorCssFontResolver.getGlobalInstance())
            .build()
            .also {
                val baseFontSize = UIManager.getFont("Label.font").size
                val codeFontName = EditorCssFontResolver.EDITOR_FONT_NAME_NO_LIGATURES_PLACEHOLDER
                // val contentCodeFontSizePercent = DocumentationSettings.getMonospaceFontSizeCorrection(true)

                val paragraphSpacing = """padding: ${scale(4)}px 0 ${scale(4)}px 0"""

                it.styleSheet.addStyleSheet(
                    StyleSheetUtil.loadStyleSheet(
                        """
                        h6 { font-size: ${baseFontSize + 1}}
                        h5 { font-size: ${baseFontSize + 2}}
                        h4 { font-size: ${baseFontSize + 3}}
                        h3 { font-size: ${baseFontSize + 4}}
                        h2 { font-size: ${baseFontSize + 6}}
                        h1 { font-size: ${baseFontSize + 8}}
                        h1, h2, h3, h4, h5, h6 {margin: 0 0 0 0; $paragraphSpacing; }
                        p { margin: 0 0 0 0; $paragraphSpacing; line-height: 125%; }
                        ul { margin: 0 0 0 ${scale(10)}px; $paragraphSpacing;}
                        ol { margin: 0 0 0 ${scale(20)}px; $paragraphSpacing;}
                        li { padding: ${scale(1)}px 0 ${scale(2)}px 0; }
                        li p { padding-top: 0; padding-bottom: 0; }
                        code { color: #ff8000 }
                        hr {
                            padding: ${scale(1)}px 0 0 0;
                            margin: ${scale(4)}px 0 ${scale(4)}px 0;
                            border-bottom: ${scale(1)}px solid ${ColorUtil.toHtmlColor(UIUtil.getTooltipSeparatorColor())};
                            width: 100%;
                        }
                        code, pre, .pre { font-family:"$codeFontName"; font-size:14pt; }
                        a { color: ${ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED)}; text-decoration: none; }
                        """.trimIndent()
                    )
                )
            }

        UIUtil.doNotScrollToCaret(this)
        caretPosition = 0
        isEditable = false
        foreground = JBColor.foreground()
        isOpaque = false

        text = colorizeSeparators(content.toString())

        maxWidth?.let {
            fitContent(maxWidth)
        }
    }
}

/**
 * Properly resize the [JEditorPane] to fit the content vertically.
 * [JEditorPane] cannot resize both vertically and horizontally at the same time,
 * it is necessary to use [JEditorPane.setSize] to give the **width constraint**, and then
 * make [JEditorPane.preferredSize] have the computed **height constraint** using [maxWidth].
 *
 * @receiver the [JEditorPane] to resize
 * @param maxWidth the maximum width to use for the [JEditorPane.preferredSize]
 */
private fun JEditorPane.fitContent(maxWidth: Int) {
    // If the preferred width is already smaller than the max width,
    // there's no need to try to fit the content
    if (preferredSize.width < maxWidth) return

    // Sets the size so that the JEditorPane can compute the preferred height
    setSize(maxWidth, Short.MAX_VALUE.toInt())
    // Updates the preferred width with the new width
    preferredSize = Dimension(maxWidth, preferredSize.height)
    // Setting the size again, so that it has the updated width and height
    size = preferredSize
}

// Copied from com.intellij.codeInsight.hint.LineTooltipRenderer.colorizeSeparators
// Java text components don't support specifying color for 'hr' tag, so we need to replace it with something else,
// if we need a separator with custom color
@Contract(pure = true)
private fun colorizeSeparators(html: String): String {
    val body = UIUtil.getHtmlBody(html)
    val parts = StringUtil.split(body, UIUtil.BORDER_LINE, true, false)
    if (parts.size <= 1) {
        return html
    }
    val b = StringBuilder()
    for (part in parts) {
        val addBorder = b.isNotEmpty()
        b.append("<div")
        if (addBorder) {
            b.append(" style='margin-top:6; padding-top:6; border-top: thin solid #")
                .append(ColorUtil.toHex(UIUtil.getTooltipSeparatorColor()))
                .append("'")
        }
        b.append("'>").append(part).append("</div>")
    }
    return XmlStringUtil.wrapInHtml(b.toString())
}
