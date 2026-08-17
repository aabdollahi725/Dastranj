package ir.dastranj.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** The three variants the add-transaction screen offers: هزینه / درآمد / انتقال. */
enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/**
 * A single transaction.
 *
 * ## Sign convention
 *
 * [amountRial] is always stored **positive**; direction comes from [type]. Storing a signed amount
 * would mean every query has to agree on the convention, and one that disagreed would silently
 * produce a wrong total. The signed value is derived once, in SQL, via a CASE on the type.
 *
 * ## Transfers
 *
 * A transfer is one row with both [accountId] and [toAccountId] set and no category. It leaves the
 * total net worth unchanged, which is why every spending aggregate filters it out rather than
 * treating it as income or expense.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            // Deleting an account must not silently delete its history, and must never leave a row
            // pointing at a missing account. RESTRICT forces the caller to decide.
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        // The report and budget queries all filter by time, usually with a type or category too.
        Index(value = ["occurred_at"]),
        Index(value = ["account_id", "occurred_at"]),
        Index(value = ["category_id", "occurred_at"]),
        Index(value = ["type", "occurred_at"]),
        Index(value = ["to_account_id"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: TransactionType,

    /** Always positive, in **rials**, and always a multiple of 10. */
    @ColumnInfo(name = "amount_rial")
    val amountRial: Long,

    /** The account money leaves (expense, transfer) or enters (income). */
    @ColumnInfo(name = "account_id")
    val accountId: Long,

    /** Destination account. Set only for [TransactionType.TRANSFER]. */
    @ColumnInfo(name = "to_account_id")
    val toAccountId: Long? = null,

    /** Null for transfers, set for expense and income. */
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    /**
     * When the transaction happened — epoch millis, UTC. Distinct from [createdAt] because the user
     * can back-date an entry («دیروز», «۱۷ مرداد»), and every report keys off when it *happened*.
     */
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,

    val note: String? = null,

    /**
     * Relative path of an attached receipt inside app-private storage, or null.
     *
     * A path rather than a blob: keeping images out of the database keeps the encrypted DB small
     * and its queries fast. The file itself lives in internal storage, which is already
     * app-private.
     */
    @ColumnInfo(name = "attachment_path")
    val attachmentPath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
