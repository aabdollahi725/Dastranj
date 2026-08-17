package ir.dastranj.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ir.dastranj.app.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * An account together with its derived current balance.
 *
 * Not an entity — it is the shape of a query result, which is why the balance can be trusted: it is
 * recomputed from the transactions on every read rather than stored and maintained.
 */
data class AccountWithBalance(
    val id: Long,
    val bankId: String,
    val title: String,
    val last4: String?,
    val cardTheme: String,
    val archived: Boolean,
    val sortOrder: Int,
    val initialBalanceRial: Long,
    val currentBalanceRial: Long,
)

@Dao
interface AccountDao {

    /**
     * Accounts for the Home row, each with its live balance.
     *
     * The balance is the opening balance plus the signed sum of every transaction touching the
     * account. The four CASE arms are the whole sign convention in one place:
     * an expense and the outgoing leg of a transfer subtract; income and the incoming leg add.
     *
     * `SUM` returns NULL when an account has no transactions, hence the COALESCE — without it a new
     * account's balance would come back NULL rather than its opening figure.
     */
    @Query(
        """
        SELECT
            a.id AS id,
            a.bank_id AS bankId,
            a.title AS title,
            a.last4 AS last4,
            a.card_theme AS cardTheme,
            a.archived AS archived,
            a.sort_order AS sortOrder,
            a.initial_balance_rial AS initialBalanceRial,
            a.initial_balance_rial + COALESCE((
                SELECT SUM(
                    CASE
                        WHEN t.type = 'EXPENSE'  AND t.account_id    = a.id THEN -t.amount_rial
                        WHEN t.type = 'INCOME'   AND t.account_id    = a.id THEN  t.amount_rial
                        WHEN t.type = 'TRANSFER' AND t.account_id    = a.id THEN -t.amount_rial
                        WHEN t.type = 'TRANSFER' AND t.to_account_id = a.id THEN  t.amount_rial
                        ELSE 0
                    END
                )
                FROM transactions t
                WHERE t.account_id = a.id OR t.to_account_id = a.id
            ), 0) AS currentBalanceRial
        FROM accounts a
        WHERE a.archived = 0
        ORDER BY a.sort_order ASC, a.id ASC
        """
    )
    fun observeActiveAccountsWithBalance(): Flow<List<AccountWithBalance>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts WHERE archived = 0")
    fun observeActiveAccountCount(): Flow<Int>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM accounts")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    /**
     * Only safe once the account has no transactions — the foreign keys are RESTRICT, so this
     * throws rather than orphaning history. Archiving is the normal path.
     */
    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :id OR to_account_id = :id")
    suspend fun transactionCount(id: Long): Int
}
