package ir.dastranj.app.domain.report

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChartHitTestTest {

    private val width = 360f
    private val bars = 12
    private val slot = width / bars // 30px per month

    @Test
    fun `the first month sits at the right edge`() {
        // Farvardin is index 0 and must be the rightmost bar in an RTL chart.
        assertThat(ChartHitTest.monthIndexAt(x = 355f, width, bars)).isEqualTo(0)
        assertThat(ChartHitTest.monthIndexAt(x = 331f, width, bars)).isEqualTo(0)
    }

    @Test
    fun `the last month sits at the left edge`() {
        assertThat(ChartHitTest.monthIndexAt(x = 5f, width, bars)).isEqualTo(11)
        assertThat(ChartHitTest.monthIndexAt(x = 29f, width, bars)).isEqualTo(11)
    }

    @Test
    fun `each slot maps to its own month`() {
        // Tap the middle of every slot and expect the months in order, right to left.
        for (index in 0 until bars) {
            val centre = width - (index + 0.5f) * slot
            assertThat(ChartHitTest.monthIndexAt(centre, width, bars)).isEqualTo(index)
        }
    }

    @Test
    fun `mirroring is not accidentally left-to-right`() {
        // The failure this whole file exists to catch: a tap near the right edge must not return
        // the last month.
        val nearRightEdge = ChartHitTest.monthIndexAt(x = width - 1f, width, bars)
        assertThat(nearRightEdge).isEqualTo(0)
        assertThat(nearRightEdge).isNotEqualTo(bars - 1)
    }

    @Test
    fun `taps at the exact edges stay in range`() {
        assertThat(ChartHitTest.monthIndexAt(x = 0f, width, bars)).isEqualTo(11)
        assertThat(ChartHitTest.monthIndexAt(x = width, width, bars)).isEqualTo(0)
    }

    @Test
    fun `out of bounds taps are clamped rather than crashing`() {
        // A tap can land slightly outside during a drag.
        assertThat(ChartHitTest.monthIndexAt(x = -50f, width, bars)).isEqualTo(11)
        assertThat(ChartHitTest.monthIndexAt(x = width + 50f, width, bars)).isEqualTo(0)
    }

    @Test
    fun `degenerate geometry returns null instead of dividing by zero`() {
        assertThat(ChartHitTest.monthIndexAt(10f, width = 0f, barCount = 12)).isNull()
        assertThat(ChartHitTest.monthIndexAt(10f, width = 360f, barCount = 0)).isNull()
        assertThat(ChartHitTest.monthIndexAt(10f, width = 360f, barCount = -1)).isNull()
    }

    @Test
    fun `every point across the width maps to a valid month`() {
        var x = 0f
        while (x <= width) {
            val index = ChartHitTest.monthIndexAt(x, width, bars)
            assertThat(index).isNotNull()
            assertThat(index!!).isAtLeast(0)
            assertThat(index).isAtMost(bars - 1)
            x += 0.5f
        }
    }

    @Test
    fun `the mapping is monotonic from right to left`() {
        // Walking leftwards must never step backwards through the months.
        var previous = -1
        var x = width
        while (x >= 0f) {
            val index = ChartHitTest.monthIndexAt(x, width, bars)!!
            assertThat(index).isAtLeast(previous)
            previous = index
            x -= 1f
        }
        assertThat(previous).isEqualTo(bars - 1)
    }
}
