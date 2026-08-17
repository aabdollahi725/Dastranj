package ir.dastranj.app.ui.transaction

import com.google.common.truth.Truth.assertThat
import ir.dastranj.app.data.db.entity.TransactionType
import org.junit.Test

class AddTransactionUiStateTest {

    private fun category(id: Long, name: String, primary: Boolean) =
        CategoryOption(id, name, "pizza", "#E4813A", primary)

    /** Eight primary categories plus two behind «بیشتر», mirroring the seeded expense set. */
    private val cats = buildList {
        addAll((1L..8L).map { category(it, "دستهٔ $it", primary = true) })
        add(category(9L, "آموزش", primary = false))
        add(category(10L, "سفر", primary = false))
    }

    /** A distinct income taxonomy, so switching type cannot silently reuse expense categories. */
    private val incomeCats = (11L..18L).map { category(it, "درآمد $it", primary = true) }

    private val accounts = listOf(
        AccountOption(1L, "بانک ملت", isCash = false),
        AccountOption(2L, "بانک سامان", isCash = false),
        AccountOption(3L, "کیف نقدی", isCash = true),
    )

    private fun base() = AddTransactionUiState(
        expenseCategories = cats,
        incomeCategories = incomeCats,
        accounts = accounts,
        accountId = 1L,
    )

    // ---- Submission gating ---------------------------------------------------------------------

    @Test
    fun `an expense needs an amount and a category`() {
        assertThat(base().canSubmit).isFalse()
        assertThat(base().copy(amountToman = "480000").canSubmit).isFalse()
        assertThat(base().copy(amountToman = "480000", selectedCategoryId = 1L).canSubmit).isTrue()
    }

    @Test
    fun `a zero amount does not submit`() {
        // "0" is non-empty but worthless — the guard is on the value, not just the string.
        val state = base().copy(amountToman = "0", selectedCategoryId = 1L)
        assertThat(state.canSubmit).isFalse()
    }

    @Test
    fun `a transfer needs no category but does need a distinct destination`() {
        val transfer = base().copy(type = TransactionType.TRANSFER, amountToman = "480000")

        // No category required.
        assertThat(transfer.copy(transferToAccountId = 2L).canSubmit).isTrue()
        // But a destination is.
        assertThat(transfer.canSubmit).isFalse()
        // And it must differ from the source, or the transfer is a no-op.
        assertThat(transfer.copy(transferToAccountId = 1L).canSubmit).isFalse()
    }

    @Test
    fun `nothing submits without an account`() {
        val state = base().copy(accountId = null, amountToman = "480000", selectedCategoryId = 1L)
        assertThat(state.canSubmit).isFalse()
    }

    // ---- The nine-tile grid --------------------------------------------------------------------

    @Test
    fun `the grid is always nine tiles`() {
        assertThat(base().categoryTiles()).hasSize(9)
        assertThat(base().copy(selectedCategoryId = 9L).categoryTiles()).hasSize(9)
    }

    @Test
    fun `the ninth tile opens the sheet when no more-category is selected`() {
        val last = base().categoryTiles().last()

        assertThat(last.opensSheet).isTrue()
        assertThat(last.category).isNull()
        assertThat(last.label).isEqualTo(AddTransactionUiState.OTHER_TILE_LABEL)
    }

    @Test
    fun `a category chosen from the sheet takes over the ninth tile`() {
        // Otherwise the grid would look as though nothing were selected.
        val last = base().copy(selectedCategoryId = 9L).categoryTiles().last()

        assertThat(last.opensSheet).isFalse()
        assertThat(last.category?.id).isEqualTo(9L)
        assertThat(last.label).isEqualTo("آموزش")
        assertThat(last.selected).isTrue()
    }

    @Test
    fun `picking a primary category restores the way back to the sheet`() {
        // This is what stops the user from getting stranded on a more-category.
        val afterSheet = base().copy(selectedCategoryId = 9L)
        assertThat(afterSheet.categoryTiles().last().opensSheet).isFalse()

        val afterPrimary = afterSheet.copy(selectedCategoryId = 3L)
        assertThat(afterPrimary.categoryTiles().last().opensSheet).isTrue()
    }

    @Test
    fun `exactly one tile is marked selected`() {
        val tiles = base().copy(selectedCategoryId = 3L).categoryTiles()

        assertThat(tiles.count { it.selected }).isEqualTo(1)
        assertThat(tiles.first { it.selected }.category?.id).isEqualTo(3L)
    }

    @Test
    fun `no tile is selected before a category is chosen`() {
        assertThat(base().categoryTiles().none { it.selected }).isTrue()
    }

    @Test
    fun `the grid swaps taxonomies with the type`() {
        val expense = base().categoryTiles().mapNotNull { it.category?.id }
        val income = base().copy(type = TransactionType.INCOME).categoryTiles()
            .mapNotNull { it.category?.id }

        // Disjoint sets — an income entry must never offer an expense category.
        assertThat(expense).containsNoneIn(income)
        assertThat(income).isNotEmpty()
    }

    @Test
    fun `a transfer offers no categories at all`() {
        val transfer = base().copy(type = TransactionType.TRANSFER)

        assertThat(transfer.categoriesForType).isEmpty()
        // The grid degenerates to the «سایر» slot alone rather than showing stale tiles.
        assertThat(transfer.categoryTiles().mapNotNull { it.category }).isEmpty()
    }

    @Test
    fun `a category id from the other taxonomy does not resolve`() {
        // Guards the type-switch clearing: an expense id must not survive into income.
        val state = base().copy(type = TransactionType.INCOME, selectedCategoryId = 1L)

        assertThat(state.selectedCategory).isNull()
        assertThat(state.categoryTiles().none { it.selected }).isTrue()
    }

    // ---- Amount presentation -------------------------------------------------------------------

    @Test
    fun `an empty amount shows a zero rather than a blank`() {
        assertThat(base().amountFormatted).isEqualTo("۰")
        assertThat(base().amountInWords).isEmpty()
    }

    @Test
    fun `the amount is grouped and spelled out`() {
        val state = base().copy(amountToman = "480000")

        assertThat(state.amountFormatted).isEqualTo("۴۸۰٬۰۰۰")
        assertThat(state.amountInWords).isEqualTo("چهارصد و هشتاد هزار تومان")
    }

    @Test
    fun `the sign glyph follows the direction of the money`() {
        val filled = base().copy(amountToman = "480000")

        assertThat(filled.copy(type = TransactionType.EXPENSE).signGlyph).isEqualTo("−")
        assertThat(filled.copy(type = TransactionType.INCOME).signGlyph).isEqualTo("+")
        // A transfer changes no total, so it carries no sign.
        assertThat(filled.copy(type = TransactionType.TRANSFER).signGlyph).isNull()
        // Nothing typed yet, nothing to sign.
        assertThat(base().signGlyph).isNull()
    }

    @Test
    fun `the minus glyph is the typographic minus, not a hyphen`() {
        val glyph = base().copy(amountToman = "1", type = TransactionType.EXPENSE).signGlyph
        assertThat(glyph).isEqualTo("−")
        assertThat(glyph).isNotEqualTo("-")
    }
}
