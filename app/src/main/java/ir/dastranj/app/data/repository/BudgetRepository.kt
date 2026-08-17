package ir.dastranj.app.data.repository

import ir.dastranj.app.data.db.dao.BudgetDao
import ir.dastranj.app.data.db.dao.BudgetWithSpend
import ir.dastranj.app.data.db.entity.BudgetEntity
import ir.dastranj.app.domain.date.JalaliDate
import ir.dastranj.app.domain.date.JalaliDateFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Budgets for one Jalali month, each with its spend.
     *
     * The period's epoch-millis bounds come from [JalaliDateFormatter.monthRange], so the Jalali
     * month boundary is derived in exactly one place rather than re-implemented per query.
     */
    fun observeForPeriod(periodYearMonth: Int): Flow<List<BudgetWithSpend>> {
        val range = JalaliDateFormatter.monthRange(periodYearMonth)
        return budgetDao
            // The DAO takes an exclusive upper bound; monthRange's `last` is inclusive.
            .observeForPeriod(periodYearMonth, range.first, range.last + 1)
            .flowOn(ioDispatcher)
    }

    suspend fun getForPeriod(periodYearMonth: Int): List<BudgetEntity> =
        withContext(ioDispatcher) { budgetDao.getForPeriod(periodYearMonth) }

    suspend fun getById(id: Long): BudgetEntity? =
        withContext(ioDispatcher) { budgetDao.getById(id) }

    suspend fun hasAnyBudget(): Boolean = withContext(ioDispatcher) { budgetDao.count() > 0 }

    suspend fun create(
        categoryId: Long,
        periodYearMonth: Int,
        amountRial: Long,
        thresholdPercent: Int,
        autoRepeat: Boolean,
        nowMillis: Long,
    ): Long = withContext(ioDispatcher) {
        budgetDao.insert(
            BudgetEntity(
                categoryId = categoryId,
                periodYearMonth = periodYearMonth,
                amountRial = amountRial,
                thresholdPercent = thresholdPercent,
                autoRepeat = autoRepeat,
                createdAt = nowMillis,
            ),
        )
    }

    suspend fun update(budget: BudgetEntity) = withContext(ioDispatcher) { budgetDao.update(budget) }

    suspend fun delete(budget: BudgetEntity) = withContext(ioDispatcher) { budgetDao.delete(budget) }

    suspend fun setNotified(id: Long, threshold: Boolean, exceeded: Boolean) =
        withContext(ioDispatcher) { budgetDao.setNotified(id, threshold, exceeded) }

    /**
     * Copies the previous month's repeating budgets into [periodYearMonth], if it has none yet.
     *
     * Lazy rather than scheduled: the app has no background worker (PRD §6 removed the only
     * component that would have needed one), and a budget for a month the user has not opened yet
     * has nothing to measure. Calling this when the Budget screen shows a month is enough.
     *
     * Three properties make it safe to call on every screen open:
     *
     * - it does nothing once the period already has budgets, so a user who deleted a copied budget
     *   does not get it back on their next visit;
     * - the copies start with their notification flags clear, which is what makes the alert-once
     *   rule reset naturally each month;
     * - the insert ignores conflicts on the unique (category, period) index, so a budget the user
     *   created by hand always wins over a copy.
     */
    suspend fun ensureRepeatedBudgets(periodYearMonth: Int, nowMillis: Long) {
        withContext(ioDispatcher) {
            if (budgetDao.countForPeriod(periodYearMonth) > 0) return@withContext

            val previous = JalaliDate.previousYearMonth(periodYearMonth)
            val repeatable = budgetDao.getRepeatableForPeriod(previous)
            if (repeatable.isEmpty()) return@withContext

            budgetDao.insertAllIgnoringDuplicates(
                repeatable.map { source ->
                    source.copy(
                        id = 0,
                        periodYearMonth = periodYearMonth,
                        // A fresh month starts un-notified — this is where alert-once resets.
                        notifiedThreshold = false,
                        notifiedExceeded = false,
                        createdAt = nowMillis,
                    )
                },
            )
        }
    }
}
