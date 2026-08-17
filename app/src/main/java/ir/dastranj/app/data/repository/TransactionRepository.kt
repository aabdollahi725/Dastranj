package ir.dastranj.app.data.repository

import ir.dastranj.app.data.db.dao.CategoryTotal
import ir.dastranj.app.data.db.dao.MerchantCategoryMapDao
import ir.dastranj.app.data.db.dao.TransactionDao
import ir.dastranj.app.data.db.entity.MerchantCategoryMapEntity
import ir.dastranj.app.data.db.entity.TransactionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val merchantDao: MerchantCategoryMapDao,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun insert(transaction: TransactionEntity): Long =
        withContext(ioDispatcher) { transactionDao.insert(transaction) }

    suspend fun update(transaction: TransactionEntity) =
        withContext(ioDispatcher) { transactionDao.update(transaction) }

    suspend fun delete(transaction: TransactionEntity) =
        withContext(ioDispatcher) { transactionDao.delete(transaction) }

    suspend fun getById(id: Long): TransactionEntity? =
        withContext(ioDispatcher) { transactionDao.getById(id) }

    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeInRange(startMillis, endMillis).flowOn(ioDispatcher)

    fun observeCategoryTotals(startMillis: Long, endMillis: Long): Flow<List<CategoryTotal>> =
        transactionDao.observeCategoryTotals(startMillis, endMillis).flowOn(ioDispatcher)

    suspend fun totalExpense(startMillis: Long, endMillis: Long, categoryId: Long? = null): Long =
        withContext(ioDispatcher) {
            transactionDao.totalExpense(startMillis, endMillis, categoryId)
        }

    suspend fun spentForCategory(categoryId: Long, startMillis: Long, endMillis: Long): Long =
        withContext(ioDispatcher) {
            transactionDao.spentForCategory(categoryId, startMillis, endMillis)
        }

    suspend fun recentNotes(limit: Int = RECENT_NOTE_LIMIT): List<String> =
        withContext(ioDispatcher) { transactionDao.recentNotes(limit) }

    // ---- Merchant → category memory --------------------------------------------------------

    suspend fun suggestCategoryFor(merchantKey: String): Long? =
        withContext(ioDispatcher) { merchantDao.suggestCategoryId(merchantKey) }

    /**
     * Records that the user filed [merchantKey] under [categoryId].
     *
     * Insert-then-update rather than a read-modify-write: [MerchantCategoryMapDao.recordHit] does
     * its increment inside SQL, so two entries saved in quick succession cannot both read the same
     * count and write back the same value, losing one.
     */
    suspend fun rememberMerchant(merchantKey: String, categoryId: Long, nowMillis: Long) {
        withContext(ioDispatcher) {
            merchantDao.insertIgnoringDuplicate(
                MerchantCategoryMapEntity(
                    merchantKey = merchantKey,
                    categoryId = categoryId,
                    lastUsedAt = nowMillis,
                ),
            )
            merchantDao.recordHit(merchantKey, categoryId, nowMillis)
        }
    }

    private companion object {
        const val RECENT_NOTE_LIMIT = 8
    }
}
