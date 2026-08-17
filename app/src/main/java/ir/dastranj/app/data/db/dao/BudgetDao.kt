package ir.dastranj.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.dastranj.app.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * A budget joined to its category and its spend for the period.
 *
 * The spend is computed in the same query as the budget so the two can never be read from different
 * moments — a budget row showing last refresh's spend is exactly the kind of stale-money bug that
 * erodes trust in the number.
 */
data class BudgetWithSpend(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryIconName: String,
    val categoryColorHex: String,
    val periodYearMonth: Int,
    val amountRial: Long,
    val spentRial: Long,
    val thresholdPercent: Int,
    val notifiedThreshold: Boolean,
    val notifiedExceeded: Boolean,
    val autoRepeat: Boolean,
)

@Dao
interface BudgetDao {

    /**
     * Every budget for one month, with its spend.
     *
     * The correlated subquery bounds spending by the period's own epoch-millis range, which the
     * caller derives from `JalaliDateFormatter.monthRange` — so the Jalali month boundary is applied
     * in exactly one place rather than being re-derived per query.
     */
    @Query(
        """
        SELECT
            b.id AS id,
            b.category_id AS categoryId,
            c.name AS categoryName,
            c.icon_name AS categoryIconName,
            c.color_hex AS categoryColorHex,
            b.period_year_month AS periodYearMonth,
            b.amount_rial AS amountRial,
            COALESCE((
                SELECT SUM(t.amount_rial)
                FROM transactions t
                WHERE t.type = 'EXPENSE'
                  AND t.category_id = b.category_id
                  AND t.occurred_at >= :startMillis
                  AND t.occurred_at < :endMillis
            ), 0) AS spentRial,
            b.threshold_percent AS thresholdPercent,
            b.notified_threshold AS notifiedThreshold,
            b.notified_exceeded AS notifiedExceeded,
            b.auto_repeat AS autoRepeat
        FROM budgets b
        INNER JOIN categories c ON c.id = b.category_id
        WHERE b.period_year_month = :periodYearMonth
        ORDER BY spentRial DESC, b.id ASC
        """
    )
    fun observeForPeriod(
        periodYearMonth: Int,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<BudgetWithSpend>>

    @Query("SELECT * FROM budgets WHERE period_year_month = :periodYearMonth")
    suspend fun getForPeriod(periodYearMonth: Int): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query(
        "SELECT * FROM budgets WHERE category_id = :categoryId AND period_year_month = :periodYearMonth"
    )
    suspend fun getForCategoryAndPeriod(categoryId: Long, periodYearMonth: Int): BudgetEntity?

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM budgets WHERE period_year_month = :periodYearMonth")
    suspend fun countForPeriod(periodYearMonth: Int): Int

    /** Budgets marked to repeat, used to seed the following month on first open. */
    @Query("SELECT * FROM budgets WHERE period_year_month = :periodYearMonth AND auto_repeat = 1")
    suspend fun getRepeatableForPeriod(periodYearMonth: Int): List<BudgetEntity>

    /**
     * IGNORE on the unique (category, period) index makes the auto-repeat copy safe to run more than
     * once: if the user already created a budget for that category this month by hand, theirs wins
     * and the copy is silently skipped.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringDuplicates(budgets: List<BudgetEntity>)

    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    /**
     * Records that a notification has fired. Separate from [update] so marking a budget as notified
     * cannot accidentally write a stale amount back over a concurrent edit.
     */
    @Query("UPDATE budgets SET notified_threshold = :threshold, notified_exceeded = :exceeded WHERE id = :id")
    suspend fun setNotified(id: Long, threshold: Boolean, exceeded: Boolean)
}
