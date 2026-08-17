package ir.dastranj.app.domain.budget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * CLAUDE.md §12 requires a unit test for the budget spend calculation.
 */
class BudgetCalculatorTest {

    private fun status(spent: Long, limit: Long, threshold: Int = 80) =
        BudgetCalculator.status(spent, limit, threshold)

    @Test
    fun `matches the design's sample rows`() {
        // خوراک: ۲٬۳۲۰٬۰۰۰ of ۲٬۰۰۰٬۰۰۰ — over.
        val food = status(23_200_000, 20_000_000)
        assertThat(food.percentUsed).isEqualTo(116)
        assertThat(food.level).isEqualTo(BudgetLevel.EXCEEDED)
        assertThat(food.overspendRial).isEqualTo(3_200_000)

        // حمل‌ونقل: ۸۵۰٬۰۰۰ of ۱٬۰۰۰٬۰۰۰ — at the threshold.
        val transport = status(8_500_000, 10_000_000)
        assertThat(transport.percentUsed).isEqualTo(85)
        assertThat(transport.level).isEqualTo(BudgetLevel.WARNING)

        // خرید: ۱٬۲۰۰٬۰۰۰ of ۳٬۰۰۰٬۰۰۰ — safe.
        val shopping = status(12_000_000, 30_000_000)
        assertThat(shopping.percentUsed).isEqualTo(40)
        assertThat(shopping.level).isEqualTo(BudgetLevel.SAFE)
    }

    @Test
    fun `spending exactly the budget is not overspending`() {
        // The boundary the design gets right with `pct > 100`, and the one most likely to be
        // broken by a later edit to `>=`.
        val exact = status(10_000_000, 10_000_000)

        assertThat(exact.percentUsed).isEqualTo(100)
        assertThat(exact.level).isEqualTo(BudgetLevel.WARNING)
        assertThat(exact.overspendRial).isEqualTo(0)
    }

    @Test
    fun `the threshold boundary is inclusive`() {
        assertThat(status(7_900_000, 10_000_000).level).isEqualTo(BudgetLevel.SAFE)
        assertThat(status(8_000_000, 10_000_000).level).isEqualTo(BudgetLevel.WARNING)
    }

    @Test
    fun `a custom threshold moves the boundary`() {
        assertThat(status(5_000_000, 10_000_000, threshold = 50).level)
            .isEqualTo(BudgetLevel.WARNING)
        assertThat(status(5_000_000, 10_000_000, threshold = 90).level)
            .isEqualTo(BudgetLevel.SAFE)
    }

    @Test
    fun `the bar never overflows its track`() {
        // A 300% overspend must not draw a bar three times the width of the track.
        val huge = status(30_000_000, 10_000_000)

        assertThat(huge.percentUsed).isEqualTo(300)
        assertThat(huge.barFraction).isEqualTo(1f)
    }

    @Test
    fun `zero spend is safe and empty`() {
        val none = status(0, 10_000_000)

        assertThat(none.percentUsed).isEqualTo(0)
        assertThat(none.barFraction).isEqualTo(0f)
        assertThat(none.level).isEqualTo(BudgetLevel.SAFE)
        assertThat(none.isFlagged).isFalse()
    }

    @Test
    fun `a zero limit does not divide by zero`() {
        // The prototype's `spent / budget` yields Infinity here and would render as garbage.
        val spentAgainstNothing = status(5_000_000, 0)
        assertThat(spentAgainstNothing.level).isEqualTo(BudgetLevel.EXCEEDED)
        assertThat(spentAgainstNothing.overspendRial).isEqualTo(5_000_000)
        assertThat(spentAgainstNothing.barFraction).isEqualTo(1f)

        val nothingAgainstNothing = status(0, 0)
        assertThat(nothingAgainstNothing.level).isEqualTo(BudgetLevel.SAFE)
        assertThat(nothingAgainstNothing.percentUsed).isEqualTo(0)
    }

    @Test
    fun `a negative limit is treated like a zero one`() {
        assertThat(status(1, -100).level).isEqualTo(BudgetLevel.EXCEEDED)
    }

    @Test
    fun `very large amounts do not overflow`() {
        // Rials are Long; a naive `spent * 100` would overflow well before this.
        val large = status(900_000_000_000_000_000L, 450_000_000_000_000_000L)

        assertThat(large.percentUsed).isEqualTo(200)
        assertThat(large.level).isEqualTo(BudgetLevel.EXCEEDED)
        assertThat(large.overspendRial).isEqualTo(450_000_000_000_000_000L)
    }

    @Test
    fun `percentage is rounded, not truncated`() {
        // 2/3 is 66.67%, which should read as ۶۷٪.
        assertThat(status(2_000_000, 3_000_000).percentUsed).isEqualTo(67)
        // 1/3 is 33.3%, which should read as ۳۳٪.
        assertThat(status(1_000_000, 3_000_000).percentUsed).isEqualTo(33)
    }

    @Test
    fun `bar fraction is always within range`() {
        val samples = listOf(0L, 1L, 5_000_000L, 10_000_000L, 30_000_000L)
        for (spent in samples) {
            val fraction = status(spent, 10_000_000).barFraction
            assertThat(fraction).isAtLeast(0f)
            assertThat(fraction).isAtMost(1f)
        }
    }
}
