package ir.dastranj.app.ui.account

import ir.dastranj.app.data.seed.Bank
import ir.dastranj.app.data.seed.BankCatalog
import ir.dastranj.app.domain.money.AmountInWords
import ir.dastranj.app.ui.util.PersianNumbers

/*
 * Deliberately free of Android and Compose imports.
 *
 * The screen's whole validation surface lives here — what makes the save button live, how the
 * keypad edits each field, how the amount is formatted and spelled. Keeping it Android-free means
 * all of it is covered by plain JVM unit tests rather than needing an instrumented device.
 */

/** The four card themes offered by the design. */
enum class CardTheme(val storageKey: String) {
    WHITE("white"),
    GREEN("green"),
    GOLD("gold"),
    INK("ink"),
}

/** Which field the on-screen numeric keypad is driving, if any. */
enum class NumericField { LAST4, BALANCE }

data class AddAccountUiState(
    val selectedBank: Bank? = null,
    val title: String = "",
    val last4: String = "",
    /** Raw toman digits as typed — never formatted, so the caret and keypad stay simple. */
    val balanceToman: String = "",
    val cardTheme: CardTheme = CardTheme.WHITE,
    val focusedNumericField: NumericField? = null,
    val bankSheetOpen: Boolean = false,
    val bankQuery: String = "",
    val showLast4Error: Boolean = false,
    val saving: Boolean = false,
    val savedAccountId: Long? = null,
) {
    /** Grouped Persian digits for the preview card, e.g. ۱۲٬۴۵۰٬۰۰۰. */
    val balanceFormatted: String
        get() = PersianNumbers.formatGrouped(balanceToman.toLongOrNull() ?: 0L)

    /** «دوازده میلیون و چهارصد و پنجاه هزار تومان», or empty when nothing is typed. */
    val balanceInWords: String
        get() = AmountInWords.spell(balanceToman.toLongOrNull() ?: 0L)

    /**
     * The design's `blocked`: a bank, a non-blank title, and all four digits.
     *
     * The balance is deliberately not part of this — it is optional, and an account with an unknown
     * opening balance is still a useful account.
     */
    val canSave: Boolean
        get() = selectedBank != null && title.isNotBlank() && last4.length == LAST4_LENGTH

    val filteredBanks: List<Bank>
        get() {
            val query = bankQuery.trim()
            if (query.isEmpty()) return BankCatalog.banks
            return BankCatalog.banks.filter {
                it.displayName.contains(query) || it.shortName.contains(query)
            }
        }

    companion object {
        const val LAST4_LENGTH = 4
        /** Matches the design's 12-character cap on the balance field. */
        const val MAX_BALANCE_DIGITS = 12
    }
}

