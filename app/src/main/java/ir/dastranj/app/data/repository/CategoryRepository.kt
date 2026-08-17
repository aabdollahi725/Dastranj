package ir.dastranj.app.data.repository

import ir.dastranj.app.data.db.dao.CategoryDao
import ir.dastranj.app.data.db.entity.CategoryEntity
import ir.dastranj.app.data.db.entity.CategoryKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /** Ordered primary-first, then by the seed order, which is the picker's grid order. */
    fun observeByKind(kind: CategoryKind): Flow<List<CategoryEntity>> =
        categoryDao.observeByKind(kind).flowOn(ioDispatcher)

    fun observeAll(): Flow<List<CategoryEntity>> =
        categoryDao.observeAll().flowOn(ioDispatcher)

    suspend fun getById(id: Long): CategoryEntity? =
        withContext(ioDispatcher) { categoryDao.getById(id) }

    suspend fun getBySeedKey(seedKey: String): CategoryEntity? =
        withContext(ioDispatcher) { categoryDao.getBySeedKey(seedKey) }
}
