package ir.dastranj.app.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.dastranj.app.data.db.DastranjDatabase
import ir.dastranj.app.data.db.DatabaseKeyProvider
import ir.dastranj.app.data.db.dao.AccountDao
import ir.dastranj.app.data.db.dao.BudgetDao
import ir.dastranj.app.data.db.dao.CategoryDao
import ir.dastranj.app.data.db.dao.MerchantCategoryMapDao
import ir.dastranj.app.data.db.dao.TransactionDao
import ir.dastranj.app.data.seed.CategorySeed
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseKeyProvider(
        @ApplicationContext context: Context,
    ): DatabaseKeyProvider = DatabaseKeyProvider(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): DastranjDatabase {
        // SQLCipher's native library must be loaded before the factory is constructed.
        System.loadLibrary("sqlcipher")

        val factory = SupportOpenHelperFactory(keyProvider.getOrCreatePassphrase())

        return Room.databaseBuilder(context, DastranjDatabase::class.java, DastranjDatabase.NAME)
            .openHelperFactory(factory)
            .addCallback(SeedCallback())
            // No fallbackToDestructiveMigration. The user's financial history is the product; a
            // schema mistake must fail the build's migration test, not silently wipe their data.
            .build()
    }

    @Provides fun provideAccountDao(db: DastranjDatabase): AccountDao = db.accountDao()

    @Provides fun provideCategoryDao(db: DastranjDatabase): CategoryDao = db.categoryDao()

    @Provides fun provideTransactionDao(db: DastranjDatabase): TransactionDao = db.transactionDao()

    @Provides fun provideBudgetDao(db: DastranjDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideMerchantCategoryMapDao(db: DastranjDatabase): MerchantCategoryMapDao =
        db.merchantCategoryMapDao()
}

/**
 * Seeds the category table when the database file is first created.
 *
 * Done as raw inserts inside Room's `onCreate` rather than through the DAO because the DAOs are not
 * available yet at this point in the build — the database is still being constructed.
 *
 * `INSERT OR IGNORE` against the unique `seed_key` index keeps this idempotent even if it somehow
 * runs twice.
 */
private class SeedCallback : androidx.room.RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        for (category in CategorySeed.categories()) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO categories
                    (seed_key, name, kind, icon_name, color_hex, is_primary, sort_order, archived)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                """.trimIndent(),
                arrayOf(
                    category.seedKey,
                    category.name,
                    category.kind.name,
                    category.iconName,
                    category.colorHex,
                    if (category.isPrimary) 1 else 0,
                    category.sortOrder,
                ),
            )
        }
    }
}
