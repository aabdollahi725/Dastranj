package ir.dastranj.app.data.repository

import ir.dastranj.app.data.db.dao.AccountDao
import ir.dastranj.app.data.db.dao.AccountWithBalance
import ir.dastranj.app.data.db.entity.AccountEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * The `ui` layer's only route to account data.
 *
 * CLAUDE.md §4 keeps `data` behind repositories so no ViewModel ever holds a DAO — that is what
 * stops a screen from quietly issuing its own query and bypassing the aggregation rules.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /** Accounts for Home, each with the balance derived from its transactions. */
    fun observeActiveAccounts(): Flow<List<AccountWithBalance>> =
        accountDao.observeActiveAccountsWithBalance().flowOn(ioDispatcher)

    fun observeActiveAccountCount(): Flow<Int> =
        accountDao.observeActiveAccountCount().flowOn(ioDispatcher)

    suspend fun getById(id: Long): AccountEntity? =
        withContext(ioDispatcher) { accountDao.getById(id) }

    /**
     * Creates an account.
     *
     * [initialBalanceRial] arrives already converted — the toman → rial step belongs to the display
     * layer (CLAUDE.md §2), so the repository takes rials and asks no questions.
     *
     * @return the new account's id.
     */
    suspend fun create(
        bankId: String,
        title: String,
        last4: String?,
        initialBalanceRial: Long,
        cardTheme: String,
        nowMillis: Long,
    ): Long = withContext(ioDispatcher) {
        accountDao.insert(
            AccountEntity(
                bankId = bankId,
                title = title.trim(),
                last4 = last4,
                initialBalanceRial = initialBalanceRial,
                cardTheme = cardTheme,
                sortOrder = accountDao.nextSortOrder(),
                createdAt = nowMillis,
            ),
        )
    }

    suspend fun update(account: AccountEntity) = withContext(ioDispatcher) {
        accountDao.update(account)
    }

    /**
     * Archives rather than deletes.
     *
     * An archived account leaves Home but keeps its transactions, so historical reports stay
     * correct. Hard deletion is only offered when the account has never been used.
     */
    suspend fun archive(id: Long) = withContext(ioDispatcher) {
        accountDao.setArchived(id, archived = true)
    }

    /** True when the account has no transactions and can safely be deleted outright. */
    suspend fun canDelete(id: Long): Boolean = withContext(ioDispatcher) {
        accountDao.transactionCount(id) == 0
    }
}
