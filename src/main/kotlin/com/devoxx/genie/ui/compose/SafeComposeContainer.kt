package com.devoxx.genie.ui.compose

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * A Swing container that safely wraps a [ComposePanel] creation.
 *
 * On certain Windows GPU/driver configurations, Skiko's Direct3D native library
 * fails to load during [addNotify], throwing [UnsatisfiedLinkError].
 * This container catches that error, automatically retries with software rendering,
 * and only shows a fallback message if all attempts fail.
 *
 * @param createCompose Factory that creates and configures the ComposePanel
 * @param onInitFailure Callback invoked when Compose initialization fails
 * @param onSoftwareFallback Callback invoked when software rendering fallback is used
 */
class SafeComposeContainer(
    private val createCompose: () -> JComponent,
    private val onInitFailure: (Throwable) -> Unit,
    private val onSoftwareFallback: () -> Unit = {},
) : JPanel(BorderLayout()) {

    private var composeComponent: JComponent? = null
    private var initialized = false

    override fun addNotify() {
        // Call super first so this panel is connected to the AWT hierarchy
        // before we add children (whose addNotify will be triggered by add()).
        super.addNotify()

        if (!initialized) {
            initialized = true
            try {
                // Both createCompose() and add() can throw UnsatisfiedLinkError:
                // createCompose() may trigger Skiko init, and add() triggers
                // addNotify() on the child ComposePanel which loads native Metal/D3D.
                composeComponent = createCompose()
                add(composeComponent, BorderLayout.CENTER)
            } catch (e: Throwable) {
                // First attempt failed - try with software rendering
                LOG.warn("Compose/Skiko initialization failed, retrying with software rendering: ${e.message}")
                removeAll()
                try {
                    System.setProperty("skiko.renderApi", "SOFTWARE")
                    composeComponent = createCompose()
                    add(composeComponent, BorderLayout.CENTER)
                    onSoftwareFallback()
                    LOG.info("Successfully initialized Compose with software rendering fallback")
                } catch (e2: Throwable) {
                    // Software rendering also failed - show fallback panel
                    LOG.error("Compose/Skiko software rendering fallback also failed", e2)
                    removeAll()
                    onInitFailure(e)
                    add(createFallbackPanel(), BorderLayout.CENTER)
                }
            }
        }
    }

    private fun createFallbackPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(20)
            add(JBLabel(
                "<html><div style='text-align:center;'>" +
                    "<b>DevoxxGenie could not initialize the chat UI.</b><br><br>" +
                    "The graphics rendering engine (Skiko) failed to start.<br>" +
                    "Automatic software rendering fallback was attempted but also failed.<br><br>" +
                    "<b>To fix this, try one of:</b><br>" +
                    "1. Update your GPU drivers<br>" +
                    "2. Add <code>-Dskiko.renderApi=SOFTWARE</code> to your IDE VM options<br>" +
                    "&nbsp;&nbsp;&nbsp;(Help &gt; Edit Custom VM Options)<br>" +
                    "3. Restart IntelliJ IDEA after making changes" +
                    "</div></html>",
                SwingConstants.CENTER
            ).apply {
                foreground = UIUtil.getLabelForeground()
            }, BorderLayout.CENTER)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(SafeComposeContainer::class.java)
    }
}
