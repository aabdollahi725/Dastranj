package ir.dastranj.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ir.dastranj.app.data.db.dao.AccountDao
import ir.dastranj.app.data.db.dao.BudgetDao
import ir.dastranj.app.data.db.dao.CategoryDao
import ir.dastranj.app.data.db.dao.MerchantCategoryMapDao
import ir.dastranj.app.data.db.dao.TransactionDao
import ir.dastranj.app.data.db.entity.AccountEntity
import ir.dastranj.app.data.db.entity.BudgetEntity
import ir.dastranj.app.data.db.entity.CategoryEntity
import ir.dastranj.app.data.db.entity.MerchantCategoryMapEntity
import ir.dastranj.app.data.db.entity.TransactionEntity

/**
 * The app's single database, encrypted at rest with SQLCipher (PRD §12).
 *
 * `exportSchema = true` writes the schema JSON into `app/schemas`, which is checked in. That file is
 * what makes future migrations reviewable and is required for Room's migration tests — losing it
 * means the next schema change cannot be verified against the shipped one.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        MerchantCategoryMapEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DastranjDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    abstract fun merchantCategoryMapDao(): MerchantCategoryMapDao

    companion object {
        const val NAME = "dastranj.db"
    }
}
