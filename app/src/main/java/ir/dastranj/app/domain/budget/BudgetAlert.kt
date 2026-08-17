package ir.dastranj.app.domain.budget

/** Which notification, if any, a budget change should produce. */
enum class BudgetAlert { NONE, THRESHOLD_REACHED, EXCEEDED }

/**
 * Decides whether a budget change warrants a notification.
 *
 * The rule is **alert once per budget per month, per kind** (PRD §11). That matters more than it
 * looks: spend is recomputed on every transaction insert, edit and delete, so without the
 * already-notified flags a user who edits an over-budget transaction three times gets three
 * identical "you have exceeded your budget" notifications for the same overspend.
 *
 * Kept pure so the whole decision table is unit-tested; the caller does the notifying and persists
 * the flags.
 */
object BudgetAlertPolicy {

    /**
     * @param notifiedThreshold whether the threshold notification has already fired this period.
     * @param notifiedExceeded whether the exceeded notification has already fired this period.
     */
    fun alertFor(
        status: BudgetStatus,
        notifiedThreshold: Boolean,
        notifiedExceeded: Boolean,
    ): BudgetAlert = when (status.level) {
        // Exceeding takes precedence: a budget that jumps straight past its limit should say so,
        // not report that it is merely "near the cap".
        BudgetLevel.EXCEEDED -> if (notifiedExceeded) BudgetAlert.NONE else BudgetAlert.EXCEEDED
        BudgetLevel.WARNING -> if (notifiedThreshold) BudgetAlert.NONE else BudgetAlert.THRESHOLD_REACHED
        BudgetLevel.SAFE -> BudgetAlert.NONE
    }

    /**
     * The flags to persist after handling [alert].
     *
     * Crossing the limit also marks the threshold as notified. Otherwise a user who leaps from 40%
     * to 120% in one transaction would get the "exceeded" notification, then a redundant "near the
     * cap" one later if a refund dropped them back into the warning band.
     *
     * Flags are never cleared within a period. Deleting a transaction can take a budget back under
     * its limit, but the user has already been told; re-arming the alert would let a single
     * edit-and-undo cycle notify them repeatedly.
     */
    fun flagsAfter(
        alert: BudgetAlert,
        notifiedThreshold: Boolean,
        notifiedExceeded: Boolean,
    ): Pair<Boolean, Boolean> = when (alert) {
        BudgetAlert.NONE -> notifiedThreshold to notifiedExceeded
        BudgetAlert.THRESHOLD_REACHED -> true to notifiedExceeded
        BudgetAlert.EXCEEDED -> true to true
    }
}
