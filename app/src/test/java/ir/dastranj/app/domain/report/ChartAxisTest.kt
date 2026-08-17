package ir.dastranj.app.domain.report

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChartAxisTest {

    private val million = 1_000_000L

    @Test
    fun `picks the next rung above the maximum`() {
        assertThat(ChartAxis.axisTop(1_200_000)).isEqualTo(millionsOf(1.5))
        assertThat(ChartAxis.axisTop(2_300_000)).isEqualTo(millionsOf(2.5))
        assertThat(ChartAxis.axisTop(7_000_000)).isEqualTo(millionsOf(8.0))
        assertThat(ChartAxis.axisTop(13_000_000)).isEqualTo(millionsOf(15.0))
    }

    @Test
    fun `an exact rung value still leaves headroom`() {
        // Without the design's 1.001 factor, a maximum of exactly 2,000,000 would pick the 2.0 rung
        // and the tallest bar would sit flush against the top gridline, reading as overflow.
        assertThat(ChartAxis.axisTop(2_000_000)).isEqualTo(millionsOf(2.5))
        assertThat(ChartAxis.axisTop(1_000_000)).isEqualTo(millionsOf(1.5))
    }

    @Test
    fun `small values get the lowest rung`() {
        assertThat(ChartAxis.axisTop(1)).isEqualTo(millionsOf(0.5))
        assertThat(ChartAxis.axisTop(400_000)).isEqualTo(millionsOf(0.5))
    }

    @Test
    fun `an empty series still yields a usable axis`() {
        // A zero axis top would mean a zero-height chart and a division by zero downstream.
        assertThat(ChartAxis.axisTop(0)).isEqualTo(million)
        assertThat(ChartAxis.axisTop(-5)).isEqualTo(million)
    }

    @Test
    fun `values past the top of the ladder round up to a whole million`() {
        assertThat(ChartAxis.axisTop(123_400_000)).isEqualTo(124 * million)
        assertThat(ChartAxis.axisTop(100_000_000)).isEqualTo(100 * million)
    }

    @Test
    fun `the axis top is never below the maximum`() {
        // The invariant the whole chart depends on: no bar may exceed the axis.
        val samples = listOf(
            1L, 999L, 500_000L, 1_000_000L, 2_000_000L, 2_500_001L,
            9_999_999L, 60_000_000L, 99_999_999L, 250_000_000L,
        )
        for (max in samples) {
            assertThat(ChartAxis.axisTop(max)).isAtLeast(max)
        }
    }

    @Test
    fun `bar fraction scales against the axis top`() {
        val top = ChartAxis.axisTop(2_000_000) // 2.5 million
        assertThat(ChartAxis.barFraction(2_500_000, top)).isEqualTo(1f)
        assertThat(ChartAxis.barFraction(1_250_000, top)).isEqualTo(0.5f)
        assertThat(ChartAxis.barFraction(0, top)).isEqualTo(0f)
    }

    @Test
    fun `bar fraction cannot escape the plot`() {
        assertThat(ChartAxis.barFraction(999_999_999, million)).isEqualTo(1f)
        assertThat(ChartAxis.barFraction(-500, million)).isEqualTo(0f)
        assertThat(ChartAxis.barFraction(100, 0)).isEqualTo(0f)
    }

    @Test
    fun `every real series produces a fraction within range`() {
        val series = listOf(0L, 320_000L, 2_320_000L, 850_000L, 1_200_000L, 47_000_000L)
        val top = ChartAxis.axisTop(series.max())

        for (value in series) {
            val fraction = ChartAxis.barFraction(value, top)
            assertThat(fraction).isAtLeast(0f)
            assertThat(fraction).isAtMost(1f)
        }
    }

    private fun millionsOf(value: Double): Long = Math.round(value * 1_000_000)
}
