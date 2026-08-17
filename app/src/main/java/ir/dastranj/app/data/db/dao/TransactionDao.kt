package ir.dastranj.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ir.dastranj.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** One month's expense total, for the report chart's twelve bars. */
data class MonthlyTotal(
    val periodYearMonth: Int,
    val totalRial: Long,
)

/** One category's share of a period, for the report breakdown. */
data class CategoryTotal(
    val categoryId: Long,
    val totalRial: Long,
)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        WHERE occurred_at >= :startMillis AND occurred_at < :endMillis
        ORDER BY occurred_at DESC, id DESC
        """
    )
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    /**
     * Total spent against one category in one period — the numerator of every budget bar.
     *
     * Transfers are excluded by the type filter: moving money between one's own accounts is not
     * spending, and counting it would let a user "blow" a budget without buying anything.
     *
     * Returns rials. Callers convert to toman once, at the display layer.
     */
    @Query(
        """
        SELECT COALESCE(SUM(amount_rial), 0)
        FROM transactions
        WHERE type = 'EXPENSE'
          AND category_id = :categoryId
          AND occurred_at >= :startMillis
          AND occurred_at < :endMillis
        """
    )
    suspend fun spentForCategory(categoryId: Long, startMillis: Long, endMillis: Long): Long

    /** Live variant of [spentForCategory], for the budget screen. */
    @Query(
        """
        SELECT COALESCE(SUM(amount_rial), 0)
        FROM transactions
        WHERE type = 'EXPENSE'
          AND category_id = :categoryId
          AND occurred_at >= :startMillis
          AND occurred_at < :endMillis
        """
    )
    fun observeSpentForCategory(
        categoryId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<Long>

    /**
     * Total expense per category over a range, largest first.
     *
     * Aggregation happens in SQL rather than in Kotlin so the whole transaction list never has to
     * be loaded to draw a summary — CLAUDE.md §7's rule against summing in memory.
     */
    @Query(
        """
        SELECT category_id AS categoryId, COALESCE(SUM(amount_rial), 0) AS totalRial
        FROM transactions
        WHERE type = 'EXPENSE'
          AND category_id IS NOT NULL
          AND occurred_at >= :startMillis
          AND occurred_at < :endMillis
        GROUP BY category_id
        ORDER BY totalRial DESC
        """
    )
    fun observeCategoryTotals(startMillis: Long, endMillis: Long): Flow<List<CategoryTotal>>

    /**
     * Total expense in a range, optionally narrowed to one category.
     *
     * A single query serves both the "all categories" and "one category" cases: passing null for
     * [categoryId] disables that arm of the WHERE clause. Two near-identical queries would be two
     * places for the transfer-exclusion rule to drift.
     */
    @Query(
        """
        SELECT COALESCE(SUM(amount_rial), 0)
        FROM transactions
        WHERE type = 'EXPENSE'
          AND occurred_at >= :startMillis
          AND occurred_at < :endMillis
          AND (:categoryId IS NULL OR category_id = :categoryId)
        """
    )
    suspend fun totalExpense(startMillis: Long, endMillis: Long, categoryId: Long?): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    /** The most recent notes, for the add-screen's note suggestions. */
    @Query(
        """
        SELECT DISTINCT note FROM transactions
        WHERE note IS NOT NULL AND note != ''
        ORDER BY occurred_at DESC
        LIMIT :limit
        """
    )
    suspend fun recentNotes(limit: Int): List<String>
}
