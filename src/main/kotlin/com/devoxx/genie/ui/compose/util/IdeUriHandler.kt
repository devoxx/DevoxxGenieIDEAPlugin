package com.devoxx.genie.ui.compose.util

import androidx.compose.ui.platform.UriHandler
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import java.util.Locale

/**
 * Opens links clicked in chat markdown (e.g. the /search "Sources" list) with IntelliJ's [BrowserUtil], so
 * they use the browser configured in the IDE. Compose's default handler relies on java.awt.Desktop, which is
 * unreliable inside the IDE (notably on Linux).
 *
 * Chat content comes from an LLM, so only http(s) and mailto links are opened; anything else (file:,
 * javascript:, relative paths) is ignored.
 */
class IdeUriHandler @JvmOverloads constructor(
    private val browse: (String) -> Unit = { BrowserUtil.browse(it) },
) : UriHandler {

    override fun openUri(uri: String) {
        val trimmed = uri.trim()
        val scheme = trimmed.substringBefore(':', missingDelimiterValue = "").lowercase(Locale.ROOT)
        if (scheme !in ALLOWED_SCHEMES) {
            LOG.info("Ignoring chat link with unsupported scheme: '$scheme'")
            return
        }
        browse(trimmed)
    }

    private companion object {
        val LOG = Logger.getInstance(IdeUriHandler::class.java)
        val ALLOWED_SCHEMES = setOf("http", "https", "mailto")
    }
}
