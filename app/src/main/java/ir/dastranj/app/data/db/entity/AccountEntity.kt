package ir.dastranj.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bank account or the cash wallet.
 *
 * Fields come from `Dastranj Add Account Screen.dc.html`: bank, title, last four digits, initial
 * balance, and card theme, plus the archive action that screen exposes.
 *
 * ## Balance
 *
 * Only [initialBalanceRial] is stored. The **current** balance is never a column: it is derived as
 * the initial balance plus the signed sum of this account's transactions. Storing it would create a
 * second source of truth that has to be kept in step with every insert, update and delete — and any
 * drift between the two would be a wrong balance shown to the user, which is the one thing this app
 * cannot get wrong.
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index("archived"),
        Index("sort_order"),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Stable machine key for the bank — `mellat`, `melli`, `saderat`, … never the Farsi label.
     *
     * The display name and brand colour live in the bank catalogue in code, so renaming a bank is a
     * catalogue edit rather than a data migration.
     */
    @ColumnInfo(name = "bank_id")
    val bankId: String,

    /** User-chosen name, e.g. «حساب حقوق». The add-account screen pre-fills «حساب <bank>». */
    val title: String,

    /**
     * Last four digits of the card, or null when not supplied.
     *
     * Stored as text, not a number: leading zeros are significant and «۰۴۵۶» must survive a round
     * trip. Never the full PAN — the app has no use for it and storing one would be a liability.
     */
    @ColumnInfo(name = "last4")
    val last4: String?,

    /** Opening balance in **rials**. May be negative for an overdrawn account. */
    @ColumnInfo(name = "initial_balance_rial")
    val initialBalanceRial: Long,

    /** One of the four card themes: سفید / سبز / طلایی / جوهری. */
    @ColumnInfo(name = "card_theme")
    val cardTheme: String,

    /**
     * Archived accounts drop off Home but keep their transactions, so historical reports stay
     * correct. The add-account screen's «بایگانی» action sets this.
     */
    val archived: Boolean = false,

    /** Manual ordering of the Home cards. */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** Epoch millis, UTC. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
