package ir.dastranj.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.dastranj.app.data.db.entity.CategoryEntity
import ir.dastranj.app.data.db.entity.CategoryKind
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query(
        """
        SELECT * FROM categories
        WHERE kind = :kind AND archived = 0
        ORDER BY is_primary DESC, sort_order ASC
        """
    )
    fun observeByKind(kind: CategoryKind): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE seed_key = :seedKey")
    suspend fun getBySeedKey(seedKey: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    /**
     * Seeding uses IGNORE against the unique `seed_key` index, which makes it idempotent: running
     * the seed twice cannot duplicate a category, and a user's edit to a seeded category's name is
     * never overwritten by a later run.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringDuplicates(categories: List<CategoryEntity>)

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}
