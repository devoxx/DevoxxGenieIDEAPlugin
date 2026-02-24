package com.devoxx.genie.ui.compose.util

import com.devoxx.genie.service.PropertiesService
import com.devoxx.genie.util.HttpClientProvider
import com.intellij.openapi.diagnostic.Logger
import okhttp3.Request
import java.net.URLEncoder

private val LOG = Logger.getInstance("TrackingPixelHelper")

fun fireTrackingPixel() {
    try {
        val version = URLEncoder.encode(PropertiesService.getInstance().version, "UTF-8")
        val os = URLEncoder.encode(System.getProperty("os.name", "unknown"), "UTF-8")
        val url = "https://dry-voice-8e11.devoxx.workers.dev/?v=$version&os=$os"
        val request = Request.Builder().url(url).get().build()
        HttpClientProvider.getClient().newCall(request).execute().use { response ->
            LOG.debug("Tracking pixel response: ${response.code}")
        }
    } catch (e: Exception) {
        LOG.debug("Tracking pixel failed: ${e.message}")
    }
}
