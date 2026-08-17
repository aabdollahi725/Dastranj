package ir.dastranj.app.ui.transaction

import ir.dastranj.app.data.db.entity.TransactionType
import ir.dastranj.app.domain.money.AmountInWords
import ir.dastranj.app.ui.util.PersianNumbers

/*
 * Android-free by design, for the same reason as AddAccountUiState: the whole of this screen's
 * decision-making — what blocks submission, which nine tiles the grid shows, how the transfer legs
 * behave — is covered by plain JVM tests instead of needing a device.
 */

/** A category as the picker needs it. Mapped from `CategoryEntity` by the ViewModel. */
data class CategoryOption(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isPrimary: Boolean,
)

/** An account as the picker needs it. */
data class AccountOption(
    val id: Long,
    val title: String,
    val isCash: Boolean,
)

/** Which sheet is open over the form. */
enum class TransactionSheet { CATEGORY, ACCOUNT, TRANSFER_FROM, TRANSFER_TO, DATE, NOTE, ATTACHMENT }

/**
 * One tile in the category grid.
 *
 * [opensSheet] marks the «سایر» tile that opens the full picker rather than selecting a category.
 * That tile has no [category], which is what distinguishes it from a real one.
 */
data class CategoryTile(
    val category: CategoryOption?,
    val label: String,
    val iconName: String,
    val colorHex: String,
    val selected: Boolean,
    val opensSheet: Boolean,
)

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    /** Raw toman digits as typed. */
    val amountToman: String = "",
    val selectedCategoryId: Long? = null,
    val accountId: Long? = null,
    val transferToAccountId: Long? = null,
    /** Epoch millis of the chosen day; null means today. */
    val occurredAtMillis: Long? = null,
    val note: String? = null,
    val attachmentPath: String? = null,
    val keypadOpen: Boolean = true,
    val openSheet: TransactionSheet? = null,
    val expenseCategories: List<CategoryOption> = emptyList(),
    val incomeCategories: List<CategoryOption> = emptyList(),
    val accounts: List<AccountOption> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
) {

    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    val amountValue: Long get() = amountToman.toLongOrNull() ?: 0L

    val isAmountEmpty: Boolean get() = amountToman.isEmpty()

    /** «۰» when nothing is typed, so the amount line never looks broken. */
    val amountFormatted: String
        get() = if (isAmountEmpty) "۰" else PersianNumbers.formatGrouped(amountValue)

    val amountInWords: String get() = AmountInWords.spell(amountValue)

    /**
     * The design's `signGlyph`: income adds, expense subtracts, a transfer does neither because it
     * moves money without changing the total.
     *
     * U+2212 MINUS, not a hyphen — it matches the digit weight of the amount beside it.
     */
    val signGlyph: String?
        get() = when {
            isAmountEmpty || isTransfer -> null
            type == TransactionType.INCOME -> "+"
            else -> "−"
        }

    /**
     * The design's `blocked`: an amount is always required, and a category is required for
     * everything except a transfer — a transfer between one's own accounts is not spending, so it
     * has nothing to categorise.
     */
    val canSubmit: Boolean
        get() = !isAmountEmpty &&
            amountValue > 0 &&
            (isTransfer || selectedCategoryId != null) &&
            accountId != null &&
            (!isTransfer || (transferToAccountId != null && transferToAccountId != accountId))

    /**
     * The taxonomy for the current type.
     *
     * Expense and income categories are disjoint sets, so this switches wholesale rather than
     * filtering one combined list — and a transfer shows none at all, because it has nothing to
     * categorise.
     */
    val categoriesForType: List<CategoryOption>
        get() = when (type) {
            TransactionType.EXPENSE -> expenseCategories
            TransactionType.INCOME -> incomeCategories
            TransactionType.TRANSFER -> emptyList()
        }

    val selectedCategory: CategoryOption?
        get() = categoriesForType.firstOrNull { it.id == selectedCategoryId }

    /**
     * The nine grid tiles.
     *
     * Eight primary categories plus one slot that adapts: it shows the currently selected
     * "more" category if there is one, otherwise a «سایر» tile that opens the full picker. That
     * keeps a category chosen from the sheet visible in the grid instead of leaving the grid
     * looking as though nothing is selected — and when the user then picks a primary category, the
     * slot reverts to «سایر», restoring the way back to the sheet.
     */
    fun categoryTiles(): List<CategoryTile> {
        val primary = categoriesForType.filter { it.isPrimary }.take(PRIMARY_TILE_COUNT)
        val selected = selectedCategory

        val tiles = primary.map { category ->
            CategoryTile(
                category = category,
                label = category.name,
                iconName = category.iconName,
                colorHex = category.colorHex,
                selected = category.id == selectedCategoryId,
                opensSheet = false,
            )
        }

        val selectedIsPrimary = selected != null && primary.any { it.id == selected.id }
        val lastTile = if (selected != null && !selectedIsPrimary) {
            CategoryTile(
                category = selected,
                label = selected.name,
                iconName = selected.iconName,
                colorHex = selected.colorHex,
                selected = true,
                opensSheet = false,
            )
        } else {
            CategoryTile(
                category = null,
                label = OTHER_TILE_LABEL,
                iconName = OTHER_TILE_ICON,
                colorHex = OTHER_TILE_COLOR,
                selected = false,
                opensSheet = true,
            )
        }

        return tiles + lastTile
    }

    val selectedAccount: AccountOption? get() = accounts.firstOrNull { it.id == accountId }

    val transferToAccount: AccountOption?
        get() = accounts.firstOrNull { it.id == transferToAccountId }

    companion object {
        const val PRIMARY_TILE_COUNT = 8
        /** Matches the design's 12-character cap on the amount field. */
        const val MAX_AMOUNT_DIGITS = 12

        const val OTHER_TILE_LABEL = "سایر"
        const val OTHER_TILE_ICON = "ellipsis"
        const val OTHER_TILE_COLOR = "#7C8085"
    }
}
