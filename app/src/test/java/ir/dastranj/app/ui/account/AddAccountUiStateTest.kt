package ir.dastranj.app.ui.account

import com.google.common.truth.Truth.assertThat
import ir.dastranj.app.data.seed.BankCatalog
import org.junit.Test

class AddAccountUiStateTest {

    private val mellat = BankCatalog.banks.first { it.id == "mellat" }

    private fun filled() = AddAccountUiState(
        selectedBank = mellat,
        title = "حساب حقوق",
        last4 = "8241",
    )

    @Test
    fun `save requires a bank, a title and all four digits`() {
        assertThat(filled().canSave).isTrue()

        assertThat(filled().copy(selectedBank = null).canSave).isFalse()
        assertThat(filled().copy(title = "").canSave).isFalse()
        assertThat(filled().copy(last4 = "824").canSave).isFalse()
    }

    @Test
    fun `a whitespace-only title does not count as a title`() {
        assertThat(filled().copy(title = "   ").canSave).isFalse()
    }

    @Test
    fun `balance is optional`() {
        // An account whose opening balance is unknown is still a usable account.
        assertThat(filled().copy(balanceToman = "").canSave).isTrue()
    }

    @Test
    fun `balance is formatted with persian digits and separators`() {
        val state = filled().copy(balanceToman = "12450000")
        assertThat(state.balanceFormatted).isEqualTo("۱۲٬۴۵۰٬۰۰۰")
    }

    @Test
    fun `an empty balance formats as zero rather than blank`() {
        // The preview card always shows a figure; a blank would make the card look broken.
        assertThat(filled().balanceFormatted).isEqualTo("۰")
    }

    @Test
    fun `balance is spelled out to catch a mistyped zero`() {
        assertThat(filled().copy(balanceToman = "12450000").balanceInWords)
            .isEqualTo("دوازده میلیون و چهارصد و پنجاه هزار تومان")
        // Nothing typed means no words — the field shows its hint instead.
        assertThat(filled().balanceInWords).isEmpty()
    }

    @Test
    fun `bank search matches both the full name and the short name`() {
        val state = AddAccountUiState(bankQuery = "ملت")
        assertThat(state.filteredBanks.map { it.id }).contains("mellat")

        // «ملی» must not also drag in «ملت».
        val melli = AddAccountUiState(bankQuery = "ملی")
        assertThat(melli.filteredBanks.map { it.id }).containsExactly("melli")
    }

    @Test
    fun `an empty query lists every bank`() {
        assertThat(AddAccountUiState().filteredBanks).hasSize(BankCatalog.banks.size)
        // Whitespace is not a search.
        assertThat(AddAccountUiState(bankQuery = "   ").filteredBanks)
            .hasSize(BankCatalog.banks.size)
    }

    @Test
    fun `an unmatched query yields an empty list rather than everything`() {
        assertThat(AddAccountUiState(bankQuery = "ززز").filteredBanks).isEmpty()
    }

    @Test
    fun `the cash wallet is not offered in the bank picker`() {
        // It is an account with bankId "cash", but it is not a bank the user picks.
        assertThat(AddAccountUiState().filteredBanks.map { it.id })
            .doesNotContain(BankCatalog.CASH_ID)
    }

    @Test
    fun `every bank has a distinct stable key`() {
        val ids = BankCatalog.banks.map { it.id }
        assertThat(ids).containsNoDuplicates()
        // Keys are machine identifiers, so they must stay ASCII — a Farsi key would defeat the
        // point of not storing the label.
        for (id in ids) {
            assertThat(id.all { it in 'a'..'z' || it == '_' }).isTrue()
        }
    }

    @Test
    fun `an unknown bank id resolves to a fallback rather than throwing`() {
        // An account whose bank left a later catalogue must still render.
        assertThat(BankCatalog.byId("no_such_bank")).isNotNull()
        assertThat(BankCatalog.byId(BankCatalog.CASH_ID).id).isEqualTo(BankCatalog.CASH_ID)
    }
}
