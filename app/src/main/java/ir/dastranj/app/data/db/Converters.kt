package ir.dastranj.app.data.db

import androidx.room.TypeConverter
import ir.dastranj.app.data.db.entity.CategoryKind
import ir.dastranj.app.data.db.entity.TransactionType

/**
 * Enums are stored as their **names**, not their ordinals.
 *
 * An ordinal would make the stored data depend on declaration order, so inserting a new enum
 * constant in the middle would silently reinterpret every existing row. The name costs a few bytes
 * and makes the column readable in a database dump.
 *
 * The queries compare against these same names as string literals (`type = 'EXPENSE'`).
 */
class Converters {

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toCategoryKind(value: String): CategoryKind = CategoryKind.valueOf(value)

    @TypeConverter
    fun fromCategoryKind(value: CategoryKind): String = value.name
}
