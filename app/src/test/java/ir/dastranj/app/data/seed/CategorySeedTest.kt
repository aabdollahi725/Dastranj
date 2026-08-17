package ir.dastranj.app.data.seed

import com.google.common.truth.Truth.assertThat
import ir.dastranj.app.data.db.entity.CategoryKind
import org.junit.Test

/**
 * The seed is written once and then lives in every user's database, so a mistake in it is
 * effectively permanent. These tests guard the properties that a later edit could quietly break.
 */
class CategorySeedTest {

    private val categories = CategorySeed.categories()

    @Test
    fun `seeds the full taxonomy from the design`() {
        val expense = categories.filter { it.kind == CategoryKind.EXPENSE }
        val income = categories.filter { it.kind == CategoryKind.INCOME }

        // 8 primary + 10 more, and 8 primary + 5 more.
        assertThat(expense).hasSize(18)
        assertThat(income).hasSize(13)
    }

    @Test
    fun `each kind has exactly eight primary categories`() {
        // The picker grid shows eight plus the «سایر» slot; a ninth primary would silently fall off.
        for (kind in CategoryKind.entries) {
            val primary = categories.filter { it.kind == kind && it.isPrimary }
            assertThat(primary).hasSize(8)
        }
    }

    @Test
    fun `seed keys are unique`() {
        // The unique index on seed_key is what makes seeding idempotent — a duplicate here would
        // mean a category silently missing from every install.
        val keys = categories.mapNotNull { it.seedKey }

        assertThat(keys).hasSize(categories.size)
        assertThat(keys).containsNoDuplicates()
    }

    @Test
    fun `seed keys are ascii machine identifiers`() {
        for (key in categories.mapNotNull { it.seedKey }) {
            assertThat(key.all { it in 'a'..'z' || it == '_' }).isTrue()
        }
    }

    @Test
    fun `labels shared across kinds have distinct keys`() {
        // «هدیه» and «سایر» appear under both kinds and are genuinely different categories —
        // a gift received is not a gift given. Merging them would merge unrelated spending.
        val gifts = categories.filter { it.name == "هدیه" }
        assertThat(gifts).hasSize(2)
        assertThat(gifts.map { it.seedKey }).containsExactly("gift_expense", "gift_income")

        val others = categories.filter { it.name == "سایر" }
        assertThat(others.map { it.seedKey }).containsExactly("other_expense", "other_income")
    }

    @Test
    fun `sort order is contiguous within each kind`() {
        for (kind in CategoryKind.entries) {
            val orders = categories.filter { it.kind == kind }.map { it.sortOrder }.sorted()
            assertThat(orders).isEqualTo(orders.indices.toList())
        }
    }

    @Test
    fun `every colour is a six-digit hex value`() {
        for (category in categories) {
            assertThat(category.colorHex).matches("#[0-9A-Fa-f]{6}")
        }
    }

    @Test
    fun `no category name is blank`() {
        for (category in categories) {
            assertThat(category.name.isBlank()).isFalse()
            assertThat(category.iconName.isBlank()).isFalse()
        }
    }
}
