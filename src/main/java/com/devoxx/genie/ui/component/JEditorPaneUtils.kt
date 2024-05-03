package com.devoxx.genie.ui.component

import com.intellij.openapi.editor.impl.EditorCssFontResolver
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.*
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.util.ui.*
import javax.swing.JEditorPane
import javax.swing.UIManager
import javax.swing.event.HyperlinkListener
import javax.swing.text.html.StyleSheet
import org.jetbrains.annotations.Contract

/**
 * Creates a [JEditorPane] tailored for HTML content, utilizing IntelliJ API to enhance display and interaction.
 * Features include word wrapping, icons, and support for Base64 images.
 */
fun htmlJEditorPane(
    content: CharSequence,
    extensions: List<ExtendableHTMLViewFactory.Extension> = emptyList(),
    hyperlinkListener: HyperlinkListener = BrowserHyperlinkListener.INSTANCE
): JEditorPane = JEditorPane().apply {
    addHyperlinkListener(hyperlinkListener)
    contentType = "text/html"
    editorKit = HTMLEditorKitBuilder()
        .withViewFactoryExtensions(ExtendableHTMLViewFactory.Extensions.WORD_WRAP, *extensions.toTypedArray())
        .withFontResolver(EditorCssFontResolver.getGlobalInstance())
        .build().also {
            it.styleSheet.addStyleSheet(customizeStyles())
        }
    isEditable = false
    foreground = JBColor.foreground()
    isOpaque = false
    text = colorizeSeparators(content.toString())
    UIUtil.doNotScrollToCaret(this)
    UIUtil.invokeLaterIfNeeded {
        revalidate()
        setCaretPosition(document.length)
    }
}

private fun customizeStyles(): StyleSheet = StyleSheetUtil.loadStyleSheet("""
    h6 { font-size: ${scaleFontSize(1)}}
    h5 { font-size: ${scaleFontSize(2)}}
    h4 { font-size: ${scaleFontSize(3)}}
    h3 { font-size: ${scaleFontSize(4)}}
    h2 { font-size: ${scaleFontSize(6)}}
    h1 { font-size: ${scaleFontSize(8)}}
    h1, h2, h3, h4, h5, h6, p, ul, ol { margin: 0 0 0 0; ${paragraphSpacing()} }
    p, li { line-height: 125%; }
    ul { margin-left: ${scale(10)}px; }
    ol { margin-left: ${scale(20)}px; }
    li { padding: ${scale(1)}px 0 ${scale(2)}px 0; }
    code, pre, .pre { font-family: "${EditorCssFontResolver.EDITOR_FONT_NAME_NO_LIGATURES_PLACEHOLDER}"; font-size: 14pt; color: #ff8000; }
    hr {
        margin: ${scale(4)}px 0;
        border-bottom: ${scale(1)}px solid ${ColorUtil.toHtmlColor(UIUtil.getTooltipSeparatorColor())};
        width: 100%;
    }
    a { color: ${ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED)}; text-decoration: none; }
""".trimIndent())

@Contract(pure = true)
private fun colorizeSeparators(html: String): String {
    val body = UIUtil.getHtmlBody(html)
    val parts = StringUtil.split(body, UIUtil.BORDER_LINE, true, false)
    if (parts.size <= 1) return html

    return parts.joinToString(separator = "", prefix = "<div", postfix = "</div>") { part ->
        if (part != parts.first()) "<div style='margin-top:6; padding-top:6; border-top: thin solid #${ColorUtil.toHex(UIUtil.getTooltipSeparatorColor())}'>$part</div>"
        else part
    }
}

private fun scaleFontSize(increment: Int) = (UIManager.getFont("Label.font").size + increment).toString()

private fun paragraphSpacing() = "padding: ${scale(4)}px 0 ${scale(4)}px 0"
