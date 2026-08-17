package ir.dastranj.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Remembers which category the user chose for a given merchant or note, so the picker can
 * pre-select it next time.
 *
 * This is the app's only learned state. It is local, derived entirely from the user's own choices,
 * and never leaves the device.
 *
 * ## Why the key is normalised
 *
 * [merchantKey] holds a normalised form of the note — trimmed, case-folded, Persian/Arabic
 * characters unified, digits stripped. Without normalisation «کافه لمیز» and «کافه لمیز ۲» would be
 * different merchants and the map would never accumulate enough hits to be useful.
 */
@Entity(
    tableName = "merchant_category_map",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            // If a category is removed, the learned mapping is worthless — drop it rather than
            // block the delete.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["merchant_key"], unique = true),
        Index(value = ["category_id"]),
    ],
)
data class MerchantCategoryMapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Normalised merchant/note text. See the class note. */
    @ColumnInfo(name = "merchant_key")
    val merchantKey: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    /**
     * How many times the user has confirmed this pairing.
     *
     * Kept so a single mis-tap does not permanently re-point a merchant: the suggestion follows the
     * highest count, so one wrong entry is outvoted by the history rather than overriding it.
     */
    @ColumnInfo(name = "hit_count")
    val hitCount: Int = 1,

    /** Epoch millis, UTC. Breaks ties when two categories have the same count. */
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long,
)
