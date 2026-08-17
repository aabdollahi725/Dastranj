package ir.dastranj.app.domain.merchant

/**
 * Normalises a note into the key used by the merchant → category map.
 *
 * The map is only useful if the same shop reaches it as the same key. Farsi text makes that
 * non-trivial, because the identical-looking word can be typed several ways:
 *
 * - **Arabic vs Persian letters.** Many keyboards emit the Arabic `ي` and `ك`; Persian uses `ی` and
 *   `ک`. They render almost identically but are different code points.
 * - **ZWNJ.** «کافه‌لمیز» and «کافه لمیز» differ only by U+200C.
 * - **Digits.** «شعبه ۲» and «شعبه 3» are the same merchant, a different branch.
 * - **Diacritics.** Optional in Farsi and inconsistently typed.
 *
 * All of these are folded away here. The result is never shown to the user — it is a lookup key
 * only — so aggressive normalisation costs nothing in display quality.
 */
object MerchantKey {

    /** Arabic forms that must fold to their Persian equivalents. */
    private val LETTER_FOLDING = mapOf(
        'ي' to 'ی', // Arabic yeh
        'ى' to 'ی', // alef maksura
        'ك' to 'ک', // Arabic kaf
        'ة' to 'ه', // teh marbuta
        'أ' to 'ا',
        'إ' to 'ا',
        'آ' to 'ا',
        'ٱ' to 'ا',
        'ؤ' to 'و',
        'ئ' to 'ی',
    )

    /** Harakat and other combining marks, which are optional in written Farsi. */
    private val DIACRITICS = setOf(
        'ً', 'ٌ', 'ٍ', 'َ', 'ُ', 'ِ', 'ّ', 'ْ',
        'ٓ', 'ٔ', 'ٕ', 'ٰ',
    )

    private const val ZWNJ = '‌'
    private const val ZWJ = '‍'

    /**
     * @return the normalised key, or null if nothing usable remains — an empty note, or one made
     *   only of digits and punctuation, must not create a map entry.
     */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        val out = StringBuilder(raw.length)
        for (ch in raw) {
            when {
                ch in DIACRITICS -> Unit
                // Joiners become spaces so «کافه‌لمیز» and «کافه لمیز» converge.
                ch == ZWNJ || ch == ZWJ -> out.append(' ')
                // Branch numbers are noise for identifying the merchant.
                ch.isDigit() || ch in '۰'..'۹' || ch in '٠'..'٩' -> out.append(' ')
                ch.isWhitespace() -> out.append(' ')
                ch.isLetter() -> out.append(LETTER_FOLDING[ch] ?: ch.lowercaseChar())
                // Everything else — punctuation, symbols, currency marks — is dropped.
                else -> out.append(' ')
            }
        }

        val collapsed = out.toString().trim().replace(WHITESPACE_RUN, " ")
        return collapsed.ifEmpty { null }
    }

    private val WHITESPACE_RUN = Regex("\\s+")
}
