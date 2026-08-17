package ir.dastranj.app.domain.date

/**
 * A date on the Jalali (Solar Hijri) calendar.
 *
 * @param year e.g. 1405
 * @param month 1..12, Farvardin..Esfand
 * @param day 1..31
 */
data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<JalaliDate> {

    init {
        require(month in 1..12) { "Jalali month out of range" }
        require(day in 1..31) { "Jalali day out of range" }
    }

    /**
     * The `periodYearMonth` key used by `Budget` — `year * 100 + month`, e.g. 140505.
     *
     * An `Int` key rather than a string: it sorts and compares correctly as a number, which is what
     * the budget queries need, and it cannot be built with an ambiguous separator.
     */
    val yearMonth: Int get() = year * 100 + month

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        /** Splits a `periodYearMonth` key back into its year and month. */
        fun yearMonthParts(yearMonth: Int): Pair<Int, Int> =
            yearMonth / 100 to yearMonth % 100

        /** Advances a `periodYearMonth` key by one month, rolling the year over at Esfand. */
        fun nextYearMonth(yearMonth: Int): Int {
            val (y, m) = yearMonthParts(yearMonth)
            return if (m == 12) (y + 1) * 100 + 1 else y * 100 + (m + 1)
        }

        /** Steps a `periodYearMonth` key back by one month. */
        fun previousYearMonth(yearMonth: Int): Int {
            val (y, m) = yearMonthParts(yearMonth)
            return if (m == 1) (y - 1) * 100 + 12 else y * 100 + (m - 1)
        }
    }
}
