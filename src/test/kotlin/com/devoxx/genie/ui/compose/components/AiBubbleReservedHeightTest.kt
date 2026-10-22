package com.devoxx.genie.ui.compose.components

import androidx.compose.ui.unit.IntSize
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AiBubbleReservedHeightTest {

    @Test
    fun `nothing is reserved before the bubble was ever measured`() {
        assertThat(reservedMinHeight(cachedSize = null, availableWidth = 600)).isZero()
    }

    @Test
    fun `cached height is reserved when the bubble re-enters at the same width`() {
        assertThat(reservedMinHeight(cachedSize = IntSize(600, 1800), availableWidth = 600)).isEqualTo(1800)
    }

    @Test
    fun `height measured in a narrow panel is not reserved after the panel is widened`() {
        assertThat(reservedMinHeight(cachedSize = IntSize(400, 4200), availableWidth = 1100)).isZero()
    }

    @Test
    fun `height measured in a wide panel is not reserved after the panel is narrowed`() {
        assertThat(reservedMinHeight(cachedSize = IntSize(1100, 900), availableWidth = 400)).isZero()
    }

    @Test
    fun `re-entering bubble keeps its cached height while the markdown is still rendering`() {
        val reservation = BubbleHeightReservation()

        assertThat(reservation.heightFor(naturalHeight = 40, availableWidth = 600, cachedSize = IntSize(600, 1800)))
            .isEqualTo(1800)
    }

    @Test
    fun `bubble follows its content once the content caught up with the cached height`() {
        val reservation = BubbleHeightReservation()
        val cached = IntSize(600, 1800)

        reservation.heightFor(naturalHeight = 1800, availableWidth = 600, cachedSize = cached)

        assertThat(reservation.heightFor(naturalHeight = 1200, availableWidth = 600, cachedSize = cached))
            .isEqualTo(1200)
    }

    @Test
    fun `transient height recorded mid-resize does not keep the bubble tall`() {
        val reservation = BubbleHeightReservation()

        reservation.heightFor(naturalHeight = 4200, availableWidth = 1100, cachedSize = null)

        assertThat(reservation.heightFor(naturalHeight = 1500, availableWidth = 1100, cachedSize = IntSize(1100, 4200)))
            .isEqualTo(1500)
    }

    @Test
    fun `stale cached height is dropped once the reservation window expires`() {
        val reservation = BubbleHeightReservation()
        val cached = IntSize(600, 1800)

        reservation.heightFor(naturalHeight = 40, availableWidth = 600, cachedSize = cached)
        reservation.expire()

        assertThat(reservation.heightFor(naturalHeight = 1200, availableWidth = 600, cachedSize = cached))
            .isEqualTo(1200)
    }

    @Test
    fun `nothing is reserved at a different width even on re-entry`() {
        val reservation = BubbleHeightReservation()

        assertThat(reservation.heightFor(naturalHeight = 40, availableWidth = 1100, cachedSize = IntSize(600, 1800)))
            .isEqualTo(40)
    }
}
