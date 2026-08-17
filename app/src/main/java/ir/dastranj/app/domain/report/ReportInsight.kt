package ir.dastranj.app.domain.report

/**
 * What the insight card should say. Rendering to Farsi happens at the display layer.
 *
 * A sealed result rather than a string, so every branch is named, exhaustively handled, and
 * testable without a resource lookup.
 */
sealed interface ReportInsight {

    /** No month in the selected year has any data. */
    data object NoData : ReportInsight

    /**
     * There is a figure for the reference month, but too little history to compare it against.
     *
     * @param monthIndex 0-based Jalali month of the figure.
     */
    data class BareFigure(val monthIndex: Int, val amountRial: Long) : ReportInsight

    /** History exists but is all zero, so a percentage comparison is meaningless. */
    data class ZeroBaseline(val monthIndex: Int) : ReportInsight

    /** Within [ReportInsightCalculator.NOISE_THRESHOLD_PERCENT] of the mean. */
    data class LevelWithAverage(val monthIndex: Int, val monthsInMean: Int) : ReportInsight

    /**
     * Meaningfully above or below the mean.
     *
     * @param percentDifference always positive; [higher] carries the direction.
     */
    data class ComparedToAverage(
        val monthIndex: Int,
        val percentDifference: Int,
        val higher: Boolean,
        val monthsInMean: Int,
    ) : ReportInsight

    /** A past year: there is no "current" month, so the lowest month is the useful fact. */
    data class LowestMonth(val monthIndex: Int) : ReportInsight
}

/**
 * Builds the insight from a year's twelve monthly totals.
 *
 * ## Two deliberate departures from the design's `insightText`
 *
 * The design compares **the current month** against the mean of every month before it, with no
 * minimum. Both parts were changed:
 *
 * 1. **The reference is the last *completed* month, not the current one.** On the 2nd of the month
 *    the current month holds two days of spending; comparing that against full-month averages
 *    reports a dramatic "۹۴٪ کمتر" that says nothing except that the month has barely started. A
 *    completed month is the only one that can be compared like for like.
 *
 * 2. **The mean needs at least two months.** A single prior month is not an average, and calling it
 *    one lends a coincidence the authority of a trend — the first comparison a new user sees would
 *    be one arbitrary month against another.
 *
 * Where there is too little history for either rule, the card states the plain figure instead of
 * manufacturing a comparison.
 */
object ReportInsightCalculator {

    /** Differences smaller than this read as noise rather than as a change. */
    const val NOISE_THRESHOLD_PERCENT = 3

    /** Below this many prior months, no average is claimed. */
    const val MIN_MONTHS_FOR_MEAN = 2

    /**
     * @param monthlyTotals twelve entries, index 0 = Farvardin. `null` means the month has not
     *   happened yet; `0` means it happened with no spending. The distinction matters — a future
     *   month must not drag an average down.
     * @param currentMonthIndex 0-based index of the month in progress, or null when the selected
     *   year is a past one.
     */
    fun calculate(monthlyTotals: List<Long?>, currentMonthIndex: Int?): ReportInsight {
        require(monthlyTotals.size == MONTHS_IN_YEAR) { "expected twelve monthly totals" }

        if (monthlyTotals.all { it == null }) return ReportInsight.NoData

        // A past year has no month in progress, so the lowest month is the fact worth stating.
        if (currentMonthIndex == null) return lowestMonth(monthlyTotals)

        // Change 1: the reference is the last completed month.
        val referenceIndex = currentMonthIndex - 1
        if (referenceIndex < 0) {
            // Farvardin of the current year — nothing has completed yet.
            return lowestMonth(monthlyTotals)
        }

        val referenceValue = monthlyTotals.getOrNull(referenceIndex)
            ?: return lowestMonth(monthlyTotals)

        // Months strictly before the reference, excluding those that have not happened.
        val history = monthlyTotals.take(referenceIndex).filterNotNull()

        // Change 2: a single month is not an average.
        if (history.size < MIN_MONTHS_FOR_MEAN) {
            return ReportInsight.BareFigure(referenceIndex, referenceValue)
        }

        val mean = history.sum().toDouble() / history.size
        if (mean <= 0.0) return ReportInsight.ZeroBaseline(referenceIndex)

        val difference = Math.round((referenceValue / mean - 1.0) * 100.0).toInt()

        return if (kotlin.math.abs(difference) < NOISE_THRESHOLD_PERCENT) {
            ReportInsight.LevelWithAverage(referenceIndex, history.size)
        } else {
            ReportInsight.ComparedToAverage(
                monthIndex = referenceIndex,
                percentDifference = kotlin.math.abs(difference),
                higher = difference > 0,
                monthsInMean = history.size,
            )
        }
    }

    private fun lowestMonth(monthlyTotals: List<Long?>): ReportInsight {
        var lowestIndex = -1
        var lowestValue = Long.MAX_VALUE

        monthlyTotals.forEachIndexed { index, value ->
            if (value != null && value < lowestValue) {
                lowestValue = value
                lowestIndex = index
            }
        }

        return if (lowestIndex < 0) ReportInsight.NoData
        else ReportInsight.LowestMonth(lowestIndex)
    }

    private const val MONTHS_IN_YEAR = 12
}
