package ir.dastranj.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A per-category spending limit for one Jalali month.
 *
 * ## Period key
 *
 * [periodYearMonth] is `year * 100 + month` (e.g. `140505` for مرداد ۱۴۰۵), produced by
 * `JalaliDate.yearMonth`. An integer key sorts chronologically as a number, which is what the month
 * stepper and the "previous months" queries rely on.
 *
 * ## Why the notification flags are columns
 *
 * The alert-once rule is a property of *this budget in this month*, so it is stored next to the
 * budget rather than in preferences. Once [notifiedThreshold] is set, crossing the threshold again
 * — by editing a transaction, deleting one and re-adding it, or simply reopening the app — must not
 * produce a second notification. Keeping the flag here means the rule survives reinstall of the
 * notification channel and cannot drift out of step with the budget it belongs to.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        // One budget per category per month.
        Index(value = ["category_id", "period_year_month"], unique = true),
        Index(value = ["period_year_month"]),
    ],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    /** `year * 100 + month`, e.g. 140505. */
    @ColumnInfo(name = "period_year_month")
    val periodYearMonth: Int,

    /** The limit, in **rials**. */
    @ColumnInfo(name = "amount_rial")
    val amountRial: Long,

    /**
     * Percentage of the limit at which the warning fires, e.g. 80.
     *
     * Per-budget rather than global so a user can watch a tight budget closely and a loose one
     * loosely.
     */
    @ColumnInfo(name = "threshold_percent")
    val thresholdPercent: Int = DEFAULT_THRESHOLD_PERCENT,

    /** Set once the threshold notification has fired for this budget. Never reset within a month. */
    @ColumnInfo(name = "notified_threshold")
    val notifiedThreshold: Boolean = false,

    /** Set once the "budget exceeded" notification has fired. */
    @ColumnInfo(name = "notified_exceeded")
    val notifiedExceeded: Boolean = false,

    /**
     * When true, this budget is copied forward into the next month.
     *
     * Defaults to **false**: repeating is opt-in per budget rather than opt-out. A budget the user
     * set for one month is a statement about that month, and silently recreating it in the next one
     * would put a limit in front of them that they never asked for.
     *
     * The copy is created lazily when the next month is first opened, not by a background job — the
     * app has no scheduler, and a budget for a month the user has not reached yet has nothing to
     * measure.
     */
    @ColumnInfo(name = "auto_repeat")
    val autoRepeat: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {
    companion object {
        const val DEFAULT_THRESHOLD_PERCENT = 80
    }
}
