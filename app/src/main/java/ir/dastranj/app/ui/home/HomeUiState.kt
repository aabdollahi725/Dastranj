package ir.dastranj.app.ui.home

import ir.dastranj.app.ui.util.PersianNumbers

/*
 * Android-free, so the presentation rules below are unit-tested rather than eyeballed.
 */

/** One account card in the Home row. */
data class AccountCard(
    val id: Long,
    val title: String,
    /** Already-formatted «•••• ۴۵۵۶», or «کیف نقدی» for the cash wallet. */
    val maskedLabel: String,
    /** Grouped Persian toman figure, without the unit. */
    val balanceText: String,
    val cardTheme: String,
    /** Spoken form: «بانک ملت — ۸۴٬۲۰۰٬۰۰۰ تومان». */
    val contentDescription: String,
)

data class HomeUiState(
    val accounts: List<AccountCard> = emptyList(),
    val loading: Boolean = true,
) {
    /**
     * The empty state shows only once loading has finished.
     *
     * Without the loading guard, the empty card would flash on every cold start before the first
     * database emission arrives — telling a user with five accounts that they have none.
     */
    val showEmptyState: Boolean get() = !loading && accounts.isEmpty()

    val showAccountRow: Boolean get() = !loading && accounts.isNotEmpty()
}

/**
 * Builds the display strings for one account card.
 *
 * Kept out of the composable so the formatting rules — the mask, the cash-wallet special case, the
 * spoken description — are testable and stated once.
 */
object HomeCardFormatter {

    /**
     * @param last4 the stored four digits, or null for an account with no card.
     * @param cashLabel «کیف نقدی», supplied by the caller from resources.
     */
    fun maskedLabel(last4: String?, cashLabel: String): String =
        if (last4.isNullOrBlank()) cashLabel else "•••• " + PersianNumbers.toPersianDigits(last4)

    /**
     * The spoken form for the card.
     *
     * Deliberately the toman figure with its unit rather than the raw digits, because a screen
     * reader announcing an unlabelled number in a list of accounts is ambiguous.
     */
    fun contentDescription(title: String, balanceText: String, currencyUnit: String): String =
        "$title — $balanceText $currencyUnit"
}
