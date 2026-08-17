package ir.dastranj.app.domain.budget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The alert-once rule (PRD §11). Spend is recomputed on every insert, edit and delete, so these
 * flags are the only thing standing between the user and a stream of duplicate notifications.
 */
class BudgetAlertPolicyTest {

    private fun status(spent: Long, limit: Long = 10_000_000, threshold: Int = 80) =
        BudgetCalculator.status(spent, limit, threshold)

    @Test
    fun `a safe budget never alerts`() {
        val alert = BudgetAlertPolicy.alertFor(status(1_000_000), false, false)
        assertThat(alert).isEqualTo(BudgetAlert.NONE)
    }

    @Test
    fun `crossing the threshold alerts once`() {
        val crossing = status(8_500_000)

        val first = BudgetAlertPolicy.alertFor(crossing, notifiedThreshold = false, notifiedExceeded = false)
        assertThat(first).isEqualTo(BudgetAlert.THRESHOLD_REACHED)

        val (threshold, exceeded) = BudgetAlertPolicy.flagsAfter(first, false, false)
        assertThat(threshold).isTrue()
        assertThat(exceeded).isFalse()

        // Every later recomputation at the same level stays silent.
        val second = BudgetAlertPolicy.alertFor(crossing, threshold, exceeded)
        assertThat(second).isEqualTo(BudgetAlert.NONE)
    }

    @Test
    fun `exceeding alerts once, after the threshold already did`() {
        val over = status(12_000_000)

        val alert = BudgetAlertPolicy.alertFor(over, notifiedThreshold = true, notifiedExceeded = false)
        assertThat(alert).isEqualTo(BudgetAlert.EXCEEDED)

        val (threshold, exceeded) = BudgetAlertPolicy.flagsAfter(alert, true, false)
        assertThat(BudgetAlertPolicy.alertFor(over, threshold, exceeded)).isEqualTo(BudgetAlert.NONE)
    }

    @Test
    fun `a jump straight past the limit reports exceeding, not the threshold`() {
        // 40% to 120% in one transaction: the user should be told they are over, not "near".
        val leap = status(12_000_000)

        val alert = BudgetAlertPolicy.alertFor(leap, notifiedThreshold = false, notifiedExceeded = false)
        assertThat(alert).isEqualTo(BudgetAlert.EXCEEDED)
    }

    @Test
    fun `exceeding also consumes the threshold alert`() {
        // Otherwise a refund that drops the budget back into the warning band would produce a
        // redundant "near the cap" notification after the user already knows they overspent.
        val alert = BudgetAlert.EXCEEDED
        val (threshold, exceeded) = BudgetAlertPolicy.flagsAfter(alert, false, false)

        assertThat(threshold).isTrue()
        assertThat(exceeded).isTrue()

        val backInWarning = status(8_500_000)
        assertThat(BudgetAlertPolicy.alertFor(backInWarning, threshold, exceeded))
            .isEqualTo(BudgetAlert.NONE)
    }

    @Test
    fun `flags are not cleared by dropping back to safe`() {
        // An edit-and-undo cycle must not re-arm the alert and notify again.
        val (threshold, exceeded) = BudgetAlertPolicy.flagsAfter(BudgetAlert.EXCEEDED, false, false)

        val safeAgain = status(1_000_000)
        assertThat(BudgetAlertPolicy.alertFor(safeAgain, threshold, exceeded))
            .isEqualTo(BudgetAlert.NONE)

        // And going over again still stays silent for this period.
        val overAgain = status(12_000_000)
        assertThat(BudgetAlertPolicy.alertFor(overAgain, threshold, exceeded))
            .isEqualTo(BudgetAlert.NONE)
    }

    @Test
    fun `flagsAfter leaves flags untouched when nothing fired`() {
        assertThat(BudgetAlertPolicy.flagsAfter(BudgetAlert.NONE, false, false))
            .isEqualTo(false to false)
        assertThat(BudgetAlertPolicy.flagsAfter(BudgetAlert.NONE, true, false))
            .isEqualTo(true to false)
    }

    @Test
    fun `repeated recomputation never alerts twice for one crossing`() {
        // Simulates the real sequence: many recomputations as the user edits transactions.
        var threshold = false
        var exceeded = false
        var alertCount = 0

        val spends = listOf(1_000_000L, 8_500_000L, 8_600_000L, 9_000_000L, 8_400_000L, 8_900_000L)
        for (spent in spends) {
            val alert = BudgetAlertPolicy.alertFor(status(spent), threshold, exceeded)
            if (alert != BudgetAlert.NONE) alertCount++
            val flags = BudgetAlertPolicy.flagsAfter(alert, threshold, exceeded)
            threshold = flags.first
            exceeded = flags.second
        }

        assertThat(alertCount).isEqualTo(1)
    }
}
