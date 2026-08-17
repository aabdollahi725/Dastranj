package ir.dastranj.app.domain.date

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The single Jalali conversion class (CLAUDE.md §2: "تبدیل فقط از طریق یک کلاس واحد؛ منطق تبدیل
 * تکرار نشود").
 *
 * ## Why hand-written rather than a library
 *
 * `domain` must not depend on Android (CLAUDE.md §4), which rules out `android.icu.util`. The
 * conversion is a well-defined ~100-line algorithm, so a dependency would buy nothing and would
 * still need wrapping in this class. `java.time` is used for the Gregorian side only — that is JDK,
 * not Android, and is available unconditionally at minSdk 26.
 *
 * ## Algorithm
 *
 * The standard Birashk-style arithmetic conversion via Julian Day Number, using the leap-year
 * breaks table. Accurate across Jalali years 1178–1633, which spans every date the app can hold.
 *
 * ## Time zone
 *
 * Conversion is pinned to **Asia/Tehran**, not the device zone. A stored `occurredAt` is a UTC
 * instant, and turning an instant into a calendar day requires a zone. Using the device zone would
 * mean a user who travels sees a transaction move to a different Jalali day — and, worse, to a
 * different `periodYearMonth`, silently moving it between budget periods. Dastranj ships to one
 * country through one store, so a fixed zone keeps stored data and every aggregate reproducible.
 */
object JalaliDateFormatter {

    /** See the class note on why this is fixed rather than the device default. */
    val zone: ZoneId = ZoneId.of("Asia/Tehran")

    /** Month names, index 0 = Farvardin. */
    private val MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /**
     * Leap-year break points. Straight from the reference implementation of the algorithm; the
     * values are a property of the calendar, not a tunable.
     */
    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    private class YearCalc(val leap: Int, val gy: Int, val march: Int)

    /**
     * For a Jalali year: its leap offset, the corresponding Gregorian year, and the March day on
     * which Farvardin 1 falls.
     */
    private fun jalCal(jy: Int): YearCalc {
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]

        require(jy >= jp && jy < BREAKS[BREAKS.size - 1]) { "Jalali year out of supported range" }

        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp

        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1

        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG

        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4

        return YearCalc(leap, gy, march)
    }

    /** True when [year] is a Jalali leap year, i.e. Esfand has 30 days. */
    fun isLeapYear(year: Int): Boolean = jalCal(year).leap == 0

    /** Days in a Jalali month: 31 for months 1–6, 30 for 7–11, 29 or 30 for Esfand. */
    fun daysInMonth(year: Int, month: Int): Int {
        require(month in 1..12) { "Jalali month out of range" }
        return when {
            month <= 6 -> 31
            month <= 11 -> 30
            else -> if (isLeapYear(year)) 30 else 29
        }
    }

    /** Gregorian calendar date → Julian Day Number. */
    private fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Int {
        var d = ((gy + (gm - 8) / 6 + 100100) * 1461) / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d -= ((((gy + 100100 + (gm - 8) / 6) / 100) * 3) / 4) - 752
        return d
    }

    /** Julian Day Number → Gregorian calendar date. */
    private fun jdnToGregorian(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += (((4 * jdn + 183187720) / 146097) * 3) / 4 * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = ((i % 153) / 5) + 1
        val gm = ((i / 153) % 12) + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return Triple(gy, gm, gd)
    }

    /** Jalali date → Julian Day Number. */
    private fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return gregorianToJdn(r.gy, 3, r.march) + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
    }

    /** Julian Day Number → Jalali date. */
    private fun jdnToJalali(jdn: Int): JalaliDate {
        val (gy, _, _) = jdnToGregorian(jdn)
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = gregorianToJdn(gy, 3, r.march)
        var k = jdn - jdn1f

        if (k >= 0) {
            if (k <= 185) {
                return JalaliDate(jy, 1 + k / 31, (k % 31) + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, (k % 30) + 1)
    }

    // ---- Public conversion surface -------------------------------------------------------------

    /** Converts a Gregorian [LocalDate] to its Jalali equivalent. */
    fun fromGregorian(date: LocalDate): JalaliDate =
        jdnToJalali(gregorianToJdn(date.year, date.monthValue, date.dayOfMonth))

    /** Converts a Jalali date to its Gregorian equivalent. */
    fun toGregorian(date: JalaliDate): LocalDate {
        val (gy, gm, gd) = jdnToGregorian(jalaliToJdn(date.year, date.month, date.day))
        return LocalDate.of(gy, gm, gd)
    }

    /**
     * Converts a stored `occurredAt` / `createdAt` (epoch millis, UTC) to the Jalali day it falls
     * on in [zone].
     */
    fun fromEpochMillis(epochMillis: Long): JalaliDate =
        fromGregorian(Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate())

    /**
     * Converts a Jalali date to the epoch millis of its **start of day** in [zone].
     *
     * Start of day rather than noon so that a day's range is `[startOf(d), startOf(d+1))`, which is
     * what the month-range queries in `data` use as their bounds.
     */
    fun toEpochMillis(date: JalaliDate): Long =
        toGregorian(date).atStartOfDay(zone).toInstant().toEpochMilli()

    /** The inclusive-start, exclusive-end epoch-millis range covering a whole Jalali month. */
    fun monthRange(yearMonth: Int): LongRange {
        val (year, month) = JalaliDate.yearMonthParts(yearMonth)
        val start = toEpochMillis(JalaliDate(year, month, 1))
        val nextYm = JalaliDate.nextYearMonth(yearMonth)
        val (ny, nm) = JalaliDate.yearMonthParts(nextYm)
        val end = toEpochMillis(JalaliDate(ny, nm, 1))
        // Exclusive upper bound; callers use `occurredAt >= start AND occurredAt < end`.
        return start until end
    }

    /** The Jalali month name, e.g. "مرداد". */
    fun monthName(month: Int): String {
        require(month in 1..12) { "Jalali month out of range" }
        return MONTH_NAMES[month - 1]
    }

    /** All twelve month names in order, for the report chart's axis. */
    fun monthNames(): List<String> = MONTH_NAMES.toList()
}
