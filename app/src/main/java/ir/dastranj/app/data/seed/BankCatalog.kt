package ir.dastranj.app.data.seed

/**
 * A bank the user can attach an account to.
 *
 * @param id stable machine key — this is what `AccountEntity.bankId` stores. It never changes, so a
 *   display-name correction is a one-line edit here rather than a data migration.
 * @param displayName full name as shown in the bank sheet.
 * @param shortName used to pre-fill the account title as «حساب <short>».
 * @param brandColorHex the bank's identity colour, from the design.
 * @param generic true for «سایر», which pre-fills «حساب من» instead of «حساب سایر».
 */
data class Bank(
    val id: String,
    val displayName: String,
    val shortName: String,
    val brandColorHex: String,
    val generic: Boolean = false,
)

/**
 * The bank list offered on the add-account screen.
 *
 * Taken verbatim from `Dastranj Add Account Screen.dc.html`, which is the design's own list and the
 * authority on in-page content. Machine keys are added here because the design carries only Farsi
 * labels, and storing a label would make a rename a migration.
 *
 * **Provisional**: a fuller seed list is expected. Because accounts store [Bank.id] and not the
 * label, extending this list is additive — existing accounts are unaffected.
 */
object BankCatalog {

    /** Used by the cash wallet, which is not chosen from the bank sheet. */
    const val CASH_ID = "cash"

    val banks: List<Bank> = listOf(
        Bank("mellat", "بانک ملت", "ملت", "#D4373C"),
        Bank("melli", "بانک ملی", "ملی", "#0F8A64"),
        Bank("saderat", "بانک صادرات", "صادرات", "#0B6FB4"),
        Bank("tejarat", "بانک تجارت", "تجارت", "#6C63C7"),
        Bank("saman", "بانک سامان", "سامان", "#0B6FB4"),
        Bank("pasargad", "بانک پاسارگاد", "پاسارگاد", "#B4763A"),
        Bank("parsian", "بانک پارسیان", "پارسیان", "#C74B8F"),
        Bank("resalat", "بانک رسالت", "رسالت", "#3AA9A0"),
        Bank("blubank", "بلوبانک", "بلوبانک", "#1FA3C4"),
        Bank("other", "سایر", "سایر", "#7C8085", generic = true),
    )

    /** The cash wallet — «کیف نقدی» on Home. Not part of [banks] because it is not a bank. */
    val cash = Bank(CASH_ID, "کیف نقدی", "نقدی", "#7C8085", generic = true)

    private val byId: Map<String, Bank> = (banks + cash).associateBy { it.id }

    /**
     * Resolves a stored [AccountEntity.bankId].
     *
     * Falls back to «سایر» rather than throwing: an account whose bank was dropped from a later
     * catalogue must still render, since the user's money does not disappear with the listing.
     */
    fun byId(id: String): Bank = byId[id] ?: banks.last()
}
