package com.devoxx.genie.ui.compose.util

import com.intellij.ide.PowerSaveMode
import com.intellij.ide.RemoteDesktopService

/**
 * Central switch and timing constants for the plugin's Compose micro-animations.
 *
 * Durations are deliberately short (≤200ms): this is an IDE tool window, not a
 * consumer app, and longer transitions read as lag rather than polish.
 *
 * The IntelliJ Platform does not expose a single "disable animations" user setting
 * in [com.intellij.ide.ui.UISettings]; the platform's own animation engine
 * (JBAnimator) instead skips animations while power-save mode is active or when
 * running over a remote-desktop session. We follow the same convention here so the
 * plugin behaves consistently with the rest of the IDE.
 */
object IdeAnimations {

    /** Duration of the Welcome ↔ Chat screen crossfade. */
    const val SCREEN_TRANSITION_MS: Int = 180

    /** Duration of the message bubble entrance (fade-in + slight upward slide). */
    const val MESSAGE_ENTRANCE_MS: Int = 140

    /**
     * Whether UI animations should run at all. Mirrors the platform convention used
     * by JBAnimator: no animations in power-save mode or remote-desktop sessions.
     * Falls back to `true` when the platform services are unavailable (unit tests,
     * headless environments).
     */
    @JvmStatic
    fun enabled(): Boolean = try {
        !PowerSaveMode.isEnabled() && !RemoteDesktopService.isRemoteSession()
    } catch (_: Throwable) {
        true
    }
}
