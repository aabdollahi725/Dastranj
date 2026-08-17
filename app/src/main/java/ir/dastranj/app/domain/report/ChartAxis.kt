package ir.dastranj.app.domain.report

/**
 * Chooses the chart's top gridline value.
 *
 * A bar chart whose axis top is simply the largest value gives every chart a bar that touches the
 * ceiling, which makes months incomparable between categories. Rounding up to a "nice" number
 * instead means the axis label is readable and the bar heights mean something.
 *
 * The ladder is the design's own `STEPS`, in **millions of toman**. It is deliberately uneven —
 * dense at the bottom where most Iranian household categories sit, sparse above — so a category
 * spending 600,000 toman and one spending 40,000,000 both get a sensibly scaled axis.
 *
 * Pure and Android-free.
 */
object ChartAxis {

    /** Rungs in millions of toman, from the design. */
    private val STEPS = doubleArrayOf(
        0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0,
        15.0, 16.0, 18.0, 20.0, 25.0, 30.0, 40.0, 50.0, 60.0, 80.0, 100.0,
    )

    private const val MILLION = 1_000_000L

    /**
     * The 1.001 factor in the design's `x >= m * 1.001` matters: without it a maximum of exactly
     * 2,000,000 would select the 2.0 rung and the tallest bar would sit flush against the top
     * gridline, reading as though it had overflowed the chart.
     */
    private const val HEADROOM = 1.001

    /**
     * @param maxToman the largest monthly total in the series, in toman.
     * @return the axis top in toman, always at least one million so an empty or tiny series still
     *   gets a sane axis rather than a zero-height chart.
     */
    fun axisTop(maxToman: Long): Long {
        if (maxToman <= 0L) return MILLION

        val millions = maxToman.toDouble() / MILLION
        val rung = STEPS.firstOrNull { it >= millions * HEADROOM }

        return if (rung != null) {
            Math.round(rung * MILLION)
        } else {
            // Past the top of the ladder: round up to the next whole million.
            Math.ceil(millions).toLong() * MILLION
        }
    }

    /**
     * The fraction of the plot height a bar should occupy.
     *
     * Separate from the drawing code so the scaling is testable, and clamped so a value above the
     * axis top — which [axisTop] makes impossible, but a future caller might not — cannot draw a
     * bar outside the plot.
     */
    fun barFraction(valueToman: Long, axisTopToman: Long): Float {
        if (axisTopToman <= 0L) return 0f
        return (valueToman.toDouble() / axisTopToman.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
}
