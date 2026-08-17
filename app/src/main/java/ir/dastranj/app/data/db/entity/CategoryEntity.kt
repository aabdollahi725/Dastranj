package ir.dastranj.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Expense or income. Transfers carry no category. */
enum class CategoryKind { EXPENSE, INCOME }

/**
 * A spending or income category.
 *
 * Seeded from the taxonomy in `Dastranj Add Transaction Screen.dc.html`: 18 expense and 13 income
 * categories, each with a Lucide icon and a colour, split into the eight shown in the grid and the
 * rest behind «بیشتر».
 *
 * Categories are a table rather than an enum because the icon grid's order is user-visible and the
 * merchant map points at them — and because a category the user has spent against can never be
 * deleted outright without orphaning history.
 */
@Entity(
    tableName = "categories",
    indices = [
        Index("kind"),
        Index("sort_order"),
        Index(value = ["seed_key"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Stable machine key for seeded categories — `food`, `transport`, … null for user-created ones.
     *
     * This is what makes the seed idempotent: a later app version can add a category or correct a
     * label without duplicating rows, because the key identifies the row rather than the name does.
     */
    @ColumnInfo(name = "seed_key")
    val seedKey: String?,

    /** Display name, e.g. «خوراک». */
    val name: String,

    val kind: CategoryKind,

    /** Lucide slug, resolved to a drawable at the display layer. */
    @ColumnInfo(name = "icon_name")
    val iconName: String,

    /**
     * Category accent, as `#RRGGBB`.
     *
     * These are per-category identity colours from the design and are deliberately outside the
     * theme's semantic palette — a category keeps its colour in light and dark alike.
     */
    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    /**
     * True for the eight categories shown directly in the picker grid; false for the ones behind
     * «بیشتر».
     */
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    /** Hidden categories stay out of the picker but keep their history. */
    val archived: Boolean = false,
)
