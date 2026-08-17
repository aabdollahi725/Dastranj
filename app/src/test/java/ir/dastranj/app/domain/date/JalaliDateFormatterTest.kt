package ir.dastranj.app.domain.date

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

/**
 * CLAUDE.md §12 requires a Jalali conversion test covering leap years and month boundaries.
 */
class JalaliDateFormatterTest {

    @Test
    fun `nowruz anchors convert both ways`() {
        val nowruz = mapOf(
            1399 to LocalDate.of(2020, 3, 20),
            1400 to LocalDate.of(2021, 3, 21),
            1401 to LocalDate.of(2022, 3, 21),
            1402 to LocalDate.of(2023, 3, 21),
            1403 to LocalDate.of(2024, 3, 20),
            1404 to LocalDate.of(2025, 3, 21),
            1405 to LocalDate.of(2026, 3, 21),
        )

        for ((year, gregorian) in nowruz) {
            assertThat(JalaliDateFormatter.fromGregorian(gregorian))
                .isEqualTo(JalaliDate(year, 1, 1))
            assertThat(JalaliDateFormatter.toGregorian(JalaliDate(year, 1, 1)))
                .isEqualTo(gregorian)
        }
    }

    @Test
    fun `known real world dates convert correctly`() {
        // The PRD carries both calendars in its own header: ۲۶ مرداد ۱۴۰۵ / 2026-08-17.
        assertThat(JalaliDateFormatter.fromGregorian(LocalDate.of(2026, 8, 17)))
            .isEqualTo(JalaliDate(1405, 5, 26))

        // 22 Bahman 1357.
        assertThat(JalaliDateFormatter.fromGregorian(LocalDate.of(1979, 2, 11)))
            .isEqualTo(JalaliDate(1357, 11, 22))
    }

    @Test
    fun `round trip is exact for every day across 110 years`() {
        var date = LocalDate.of(1950, 1, 1)
        val end = LocalDate.of(2060, 1, 1)
        val mismatches = mutableListOf<LocalDate>()

        while (date.isBefore(end)) {
            val jalali = JalaliDateFormatter.fromGregorian(date)
            if (JalaliDateFormatter.toGregorian(jalali) != date) mismatches += date
            date = date.plusDays(1)
        }

        assertThat(mismatches).isEmpty()
    }

    @Test
    fun `month lengths follow the calendar's structure`() {
        // Months 1-6 are 31 days, 7-11 are 30.
        for (month in 1..6) {
            assertThat(JalaliDateFormatter.daysInMonth(1405, month)).isEqualTo(31)
        }
        for (month in 7..11) {
            assertThat(JalaliDateFormatter.daysInMonth(1405, month)).isEqualTo(30)
        }
    }

    @Test
    fun `esfand length matches the leap year flag and abuts nowruz`() {
        for (year in 1390..1420) {
            val leap = JalaliDateFormatter.isLeapYear(year)
            val esfandDays = JalaliDateFormatter.daysInMonth(year, 12)

            assertThat(esfandDays).isEqualTo(if (leap) 30 else 29)

            // The month boundary case: the last day of Esfand must be the day before Nowruz. This
            // is what catches an off-by-one in the leap calculation.
            val lastDayOfYear = JalaliDateFormatter.toGregorian(JalaliDate(year, 12, esfandDays))
            val nextNowruz = JalaliDateFormatter.toGregorian(JalaliDate(year + 1, 1, 1))
            assertThat(lastDayOfYear.plusDays(1)).isEqualTo(nextNowruz)
        }
    }

    @Test
    fun `leap year density matches the 33 year cycle`() {
        val leapCount = (1390..1420).count { JalaliDateFormatter.isLeapYear(it) }
        // A 31-year window of the cycle contains 7 or 8 leap years.
        assertThat(leapCount).isIn(listOf(7, 8))
    }

    @Test
    fun `month range covers exactly one jalali month`() {
        val range = JalaliDateFormatter.monthRange(140505)

        assertThat(JalaliDateFormatter.fromEpochMillis(range.first))
            .isEqualTo(JalaliDate(1405, 5, 1))
        assertThat(JalaliDateFormatter.fromEpochMillis(range.last))
            .isEqualTo(JalaliDate(1405, 5, 31))
        // Exclusive upper bound — the next millisecond is already the following month.
        assertThat(JalaliDateFormatter.fromEpochMillis(range.last + 1))
            .isEqualTo(JalaliDate(1405, 6, 1))
    }

    @Test
    fun `every day of a month falls inside that month's range`() {
        // The budget aggregation depends on this: a day that escapes its month's range would be
        // counted against the wrong budget period.
        for (yearMonth in listOf(140501, 140507, 140512, 140312)) {
            val (year, month) = JalaliDate.yearMonthParts(yearMonth)
            val range = JalaliDateFormatter.monthRange(yearMonth)

            for (day in 1..JalaliDateFormatter.daysInMonth(year, month)) {
                val millis = JalaliDateFormatter.toEpochMillis(JalaliDate(year, month, day))
                assertThat(millis).isIn(range.first..range.last)
            }
        }
    }

    @Test
    fun `year month helpers roll over at the year boundary`() {
        assertThat(JalaliDate(1405, 5, 26).yearMonth).isEqualTo(140505)
        assertThat(JalaliDate.nextYearMonth(140512)).isEqualTo(140601)
        assertThat(JalaliDate.previousYearMonth(140601)).isEqualTo(140512)
        assertThat(JalaliDate.yearMonthParts(140505)).isEqualTo(1405 to 5)
    }

    @Test
    fun `year month keys sort chronologically as integers`() {
        // The budget queries order by this key, so numeric order must equal calendar order.
        val keys = listOf(140601, 140512, 140505, 140412)
        assertThat(keys.sorted()).isEqualTo(listOf(140412, 140505, 140512, 140601))
    }

    @Test
    fun `month names are the jalali months in order`() {
        assertThat(JalaliDateFormatter.monthName(1)).isEqualTo("فروردین")
        assertThat(JalaliDateFormatter.monthName(5)).isEqualTo("مرداد")
        assertThat(JalaliDateFormatter.monthName(12)).isEqualTo("اسفند")
        assertThat(JalaliDateFormatter.monthNames()).hasSize(12)
    }
}
