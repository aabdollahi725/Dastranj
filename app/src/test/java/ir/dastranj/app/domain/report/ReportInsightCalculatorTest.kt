package ir.dastranj.app.domain.report

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * One test per branch of the insight card, as agreed.
 */
class ReportInsightCalculatorTest {

    /** Twelve months, `null` past [throughMonth]. */
    private fun year(vararg values: Long?, throughMonth: Int = 11): List<Long?> =
        (0..11).map { index ->
            if (index > throughMonth) null else values.getOrNull(index)
        }

    private fun months(vararg values: Long?): List<Long?> =
        List(12) { index -> values.getOrNull(index) }

    // ---- Branch: NoData ------------------------------------------------------------------------

    @Test
    fun `a year with no data at all reports nothing recorded`() {
        val insight = ReportInsightCalculator.calculate(List(12) { null }, currentMonthIndex = 5)
        assertThat(insight).isEqualTo(ReportInsight.NoData)
    }

    // ---- Branch: BareFigure --------------------------------------------------------------------

    @Test
    fun `one prior month is not an average`() {
        // Farvardin 1,000,000 then Ordibehesht 2,000,000, currently in Khordad.
        // The reference is Ordibehesht, with only Farvardin behind it — not enough to average.
        val insight = ReportInsightCalculator.calculate(
            months(1_000_000, 2_000_000, 500_000),
            currentMonthIndex = 2,
        )

        assertThat(insight).isEqualTo(ReportInsight.BareFigure(monthIndex = 1, amountRial = 2_000_000))
    }

    @Test
    fun `the very first completed month states its figure`() {
        // Currently in Ordibehesht; Farvardin is the reference and has no history behind it.
        val insight = ReportInsightCalculator.calculate(
            months(3_000_000, 800_000),
            currentMonthIndex = 1,
        )

        assertThat(insight).isEqualTo(ReportInsight.BareFigure(monthIndex = 0, amountRial = 3_000_000))
    }

    // ---- Branch: ZeroBaseline ------------------------------------------------------------------

    @Test
    fun `an all-zero history cannot yield a percentage`() {
        val insight = ReportInsightCalculator.calculate(
            months(0, 0, 5_000_000, 100_000),
            currentMonthIndex = 3,
        )

        assertThat(insight).isEqualTo(ReportInsight.ZeroBaseline(monthIndex = 2))
    }

    // ---- Branch: LevelWithAverage --------------------------------------------------------------

    @Test
    fun `a difference under three percent reads as level`() {
        // Mean of 1,000,000 and 1,000,000 is 1,000,000; reference 1,020,000 is +2%.
        val insight = ReportInsightCalculator.calculate(
            months(1_000_000, 1_000_000, 1_020_000, 50_000),
            currentMonthIndex = 3,
        )

        assertThat(insight).isEqualTo(
            ReportInsight.LevelWithAverage(monthIndex = 2, monthsInMean = 2),
        )
    }

    // ---- Branch: ComparedToAverage -------------------------------------------------------------

    @Test
    fun `spending above the mean reports the increase`() {
        // Mean of 1,000,000 and 1,000,000; reference 1,500,000 is +50%.
        val insight = ReportInsightCalculator.calculate(
            months(1_000_000, 1_000_000, 1_500_000, 20_000),
            currentMonthIndex = 3,
        )

        assertThat(insight).isEqualTo(
            ReportInsight.ComparedToAverage(
                monthIndex = 2,
                percentDifference = 50,
                higher = true,
                monthsInMean = 2,
            ),
        )
    }

    @Test
    fun `spending below the mean reports the decrease as a positive number`() {
        // Mean 2,000,000; reference 1,000,000 is -50%, reported as 50 with higher = false.
        val insight = ReportInsightCalculator.calculate(
            months(2_000_000, 2_000_000, 1_000_000, 20_000),
            currentMonthIndex = 3,
        ) as ReportInsight.ComparedToAverage

        assertThat(insight.percentDifference).isEqualTo(50)
        assertThat(insight.higher).isFalse()
    }

    @Test
    fun `the noise boundary is exclusive at three percent`() {
        // +3% is a real change; +2% is noise.
        val atThree = ReportInsightCalculator.calculate(
            months(1_000_000, 1_000_000, 1_030_000, 1),
            currentMonthIndex = 3,
        )
        assertThat(atThree).isInstanceOf(ReportInsight.ComparedToAverage::class.java)

        val underThree = ReportInsightCalculator.calculate(
            months(1_000_000, 1_000_000, 1_020_000, 1),
            currentMonthIndex = 3,
        )
        assertThat(underThree).isInstanceOf(ReportInsight.LevelWithAverage::class.java)
    }

    // ---- Branch: LowestMonth -------------------------------------------------------------------

    @Test
    fun `a past year reports its lowest month`() {
        val insight = ReportInsightCalculator.calculate(
            months(5_000_000, 2_000_000, 8_000_000, 3_000_000, 9_000_000,
                6_000_000, 7_000_000, 4_000_000, 5_500_000, 6_500_000, 7_500_000, 8_500_000),
            currentMonthIndex = null,
        )

        assertThat(insight).isEqualTo(ReportInsight.LowestMonth(monthIndex = 1))
    }

    @Test
    fun `farvardin of the current year falls back to the lowest month`() {
        // Nothing has completed yet, so there is no reference month to compare.
        val insight = ReportInsightCalculator.calculate(
            months(4_000_000),
            currentMonthIndex = 0,
        )

        assertThat(insight).isEqualTo(ReportInsight.LowestMonth(monthIndex = 0))
    }

    // ---- The two agreed changes, stated as their own tests --------------------------------------

    @Test
    fun `the reference is the last completed month, never the one in progress`() {
        // Mordad is in progress with a trivial part-month figure. If it were the reference, the
        // card would announce a dramatic drop that only means the month just started.
        val partialCurrentMonth = 30_000L
        val insight = ReportInsightCalculator.calculate(
            months(2_000_000, 2_000_000, 2_000_000, 2_000_000, partialCurrentMonth),
            currentMonthIndex = 4,
        )

        // Tir (index 3) is the reference, not Mordad (index 4).
        val compared = insight as ReportInsight.LevelWithAverage
        assertThat(compared.monthIndex).isEqualTo(3)
        // And the in-progress month is excluded from the mean behind it.
        assertThat(compared.monthsInMean).isEqualTo(3)
    }

    @Test
    fun `the in-progress month never enters the mean`() {
        val insight = ReportInsightCalculator.calculate(
            months(1_000_000, 1_000_000, 3_000_000, 999_999_999),
            currentMonthIndex = 3,
        ) as ReportInsight.ComparedToAverage

        // The mean is over Farvardin and Ordibehesht only — the huge in-progress figure is not in
        // it, so the reported difference is +200%, not something distorted by it.
        assertThat(insight.monthsInMean).isEqualTo(2)
        assertThat(insight.percentDifference).isEqualTo(200)
        assertThat(insight.higher).isTrue()
    }

    @Test
    fun `future months are excluded from the mean`() {
        // Nulls must not be counted as zeros, which would drag the average down.
        val insight = ReportInsightCalculator.calculate(
            year(2_000_000, 2_000_000, 2_020_000, 1, throughMonth = 3),
            currentMonthIndex = 3,
        ) as ReportInsight.LevelWithAverage

        // Only the two real months behind the reference, not the eight empty ones.
        assertThat(insight.monthsInMean).isEqualTo(2)
    }

    @Test
    fun `a zero month counts in the mean but a missing one does not`() {
        // A month with genuinely no spending is real data and belongs in the average.
        //
        // Mean of 2,000,000 and 0 is 1,000,000, so a reference of 1,000,000 is exactly level.
        // Were the zero dropped, the mean would be 2,000,000 and this would report «۵۰٪ کمتر» —
        // so the branch itself is the assertion.
        val insight = ReportInsightCalculator.calculate(
            months(2_000_000, 0, 1_000_000, 5),
            currentMonthIndex = 3,
        )

        assertThat(insight).isEqualTo(
            ReportInsight.LevelWithAverage(monthIndex = 2, monthsInMean = 2),
        )
    }

    @Test
    fun `an invalid series length is rejected`() {
        val tooShort = runCatching {
            ReportInsightCalculator.calculate(List(11) { 0L }, currentMonthIndex = 3)
        }
        assertThat(tooShort.isFailure).isTrue()
    }
}
