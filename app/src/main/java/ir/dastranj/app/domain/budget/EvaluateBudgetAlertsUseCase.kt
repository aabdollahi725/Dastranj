package ir.dastranj.app.domain.budget

import ir.dastranj.app.data.db.dao.BudgetWithSpend
import ir.dastranj.app.data.notification.BudgetNotifier
import ir.dastranj.app.data.repository.BudgetRepository
import javax.inject.Inject

/**
 * Checks a period's budgets and fires whichever notifications are due.
 *
 * Runs when the Budget screen loads a period rather than on every transaction insert. That is a
 * deliberate trade: a notification can lag the spend that caused it by as long as it takes the user
 * to open the Budget tab, but the app gains no background worker, no observer wired into the
 * transaction path, and no way for a burst of edits to queue up work. Given the alert-once rule
 * means each budget notifies at most twice a month, the lag costs the user nothing.
 */
class EvaluateBudgetAlertsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val notifier: BudgetNotifier,
) {

    suspend operator fun invoke(budgets: List<BudgetWithSpend>) {
        for (budget in budgets) {
            val status = BudgetCalculator.status(
                spentRial = budget.spentRial,
                limitRial = budget.amountRial,
                thresholdPercent = budget.thresholdPercent,
            )

            val alert = BudgetAlertPolicy.alertFor(
                status = status,
                notifiedThreshold = budget.notifiedThreshold,
                notifiedExceeded = budget.notifiedExceeded,
            )
            if (alert == BudgetAlert.NONE) continue

            notifier.notify(
                alert = alert,
                budgetId = budget.id,
                categoryName = budget.categoryName,
            )

            // Persisted whether or not the notification actually appeared. If the permission was
            // refused, the user has chosen not to be told; re-evaluating on every screen open
            // would otherwise retry forever and fire a burst the moment they granted it.
            val (threshold, exceeded) = BudgetAlertPolicy.flagsAfter(
                alert = alert,
                notifiedThreshold = budget.notifiedThreshold,
                notifiedExceeded = budget.notifiedExceeded,
            )
            budgetRepository.setNotified(budget.id, threshold, exceeded)
        }
    }
}
