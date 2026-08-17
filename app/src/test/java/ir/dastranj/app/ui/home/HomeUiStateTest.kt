package ir.dastranj.app.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeUiStateTest {

    private val cash = "کیف نقدی"
    private val unit = "تومان"

    private fun card(id: Long = 1L) = AccountCard(
        id = id,
        title = "بانک ملت",
        maskedLabel = "•••• ۴۵۵۶",
        balanceText = "۸۴٬۲۰۰٬۰۰۰",
        balanceToman = 84_200_000L,
        cardTheme = "white",
        contentDescription = "بانک ملت — ۸۴٬۲۰۰٬۰۰۰ تومان",
    )

    @Test
    fun `the empty state waits for loading to finish`() {
        // Otherwise the empty card flashes on every cold start, telling a user with five accounts
        // that they have none.
        val loading = HomeUiState(accounts = emptyList(), loading = true)

        assertThat(loading.showEmptyState).isFalse()
        assertThat(loading.showAccountRow).isFalse()
    }

    @Test
    fun `the empty state shows once loading finishes with no accounts`() {
        val loaded = HomeUiState(accounts = emptyList(), loading = false)

        assertThat(loaded.showEmptyState).isTrue()
        assertThat(loaded.showAccountRow).isFalse()
    }

    @Test
    fun `the row shows once accounts arrive`() {
        val loaded = HomeUiState(accounts = listOf(card()), loading = false)

        assertThat(loaded.showAccountRow).isTrue()
        assertThat(loaded.showEmptyState).isFalse()
    }

    @Test
    fun `the row and the empty state are never both visible`() {
        val states = listOf(
            HomeUiState(emptyList(), loading = true),
            HomeUiState(emptyList(), loading = false),
            HomeUiState(listOf(card()), loading = true),
            HomeUiState(listOf(card()), loading = false),
        )
        for (state in states) {
            assertThat(state.showAccountRow && state.showEmptyState).isFalse()
        }
    }

    @Test
    fun `a card with digits is masked with persian numerals`() {
        assertThat(HomeCardFormatter.maskedLabel("4556", cash)).isEqualTo("•••• ۴۵۵۶")
    }

    @Test
    fun `leading zeros in the mask survive`() {
        // Stored as text precisely so «۰۴۵۶» does not become «۴۵۶».
        assertThat(HomeCardFormatter.maskedLabel("0456", cash)).isEqualTo("•••• ۰۴۵۶")
    }

    @Test
    fun `an account with no card shows the wallet label instead of an empty mask`() {
        assertThat(HomeCardFormatter.maskedLabel(null, cash)).isEqualTo(cash)
        assertThat(HomeCardFormatter.maskedLabel("", cash)).isEqualTo(cash)
        assertThat(HomeCardFormatter.maskedLabel("   ", cash)).isEqualTo(cash)
    }

    @Test
    fun `the spoken description names the account and its unit`() {
        // A bare number in a list of accounts is ambiguous to a screen reader.
        val spoken = HomeCardFormatter.contentDescription("بانک ملت", "۸۴٬۲۰۰٬۰۰۰", unit)

        assertThat(spoken).isEqualTo("بانک ملت — ۸۴٬۲۰۰٬۰۰۰ تومان")
        assertThat(spoken).contains(unit)
    }
}
