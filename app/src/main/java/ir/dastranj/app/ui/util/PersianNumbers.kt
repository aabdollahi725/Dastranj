package ir.dastranj.app.ui.util

/**
 * Persian digit and thousands-separator formatting (PRD §13.3).
 *
 * ## Why this exists when the font already renders Persian digits
 *
 * The FaNum cut of IRANYekanX maps the numeral slots to ۰۱۲۳۴۵۶۷۸۹, so Latin digits *rendered in
 * that font* already look Persian. That is a rendering trick, not a data property, and it is not
 * enough on its own:
 *
 * - `contentDescription` and any string read by TalkBack are not rendered in the font at all, so a
 *   screen reader would announce Latin digits.
 * - The thousands separator must be the Persian `٬` (U+066C), not a comma. No font substitutes
 *   that.
 * - Tests assert on strings, not on glyphs.
 *
 * So numbers are converted here, and the font's native mapping becomes a belt-and-braces second
 * layer rather than the only one.
 */
object PersianNumbers {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** U+066C ARABIC THOUSANDS SEPARATOR — the correct grouping mark for Farsi. */
    const val THOUSANDS_SEPARATOR = '٬'

    /** Replaces every ASCII digit in [input] with its Persian counterpart. */
    fun toPersianDigits(input: String): String {
        if (input.isEmpty()) return input
        val out = StringBuilder(input.length)
        for (ch in input) {
            out.append(if (ch in '0'..'9') PERSIAN_DIGITS[ch - '0'] else ch)
        }
        return out.toString()
    }

    /** Replaces every Persian digit in [input] with its ASCII counterpart, for parsing input. */
    fun toLatinDigits(input: String): String {
        if (input.isEmpty()) return input
        val out = StringBuilder(input.length)
        for (ch in input) {
            val index = PERSIAN_DIGITS.indexOf(ch)
            out.append(if (index >= 0) ('0' + index) else ch)
        }
        return out.toString()
    }

    /**
     * Groups [value] in threes with `٬` and converts to Persian digits.
     *
     * Grouping is done by hand rather than with `NumberFormat`, because the platform's Farsi
     * grouping separator has varied across Android versions and vendor ROMs; the design calls for
     * `٬` unconditionally.
     */
    fun formatGrouped(value: Long): String {
        val negative = value < 0
        // Build from the magnitude as a string so Long.MIN_VALUE cannot overflow on negation.
        val digits = value.toString().removePrefix("-")

        val grouped = StringBuilder(digits.length + digits.length / 3)
        for ((count, index) in (digits.indices.reversed()).withIndex()) {
            if (count > 0 && count % 3 == 0) grouped.append(THOUSANDS_SEPARATOR)
            grouped.append(digits[index])
        }
        grouped.reverse()

        val body = toPersianDigits(grouped.toString())
        // U+2212 MINUS SIGN, not the ASCII hyphen — it aligns with the digit weight.
        return if (negative) "−$body" else body
    }

    /**
     * Formats stored rials as a grouped toman figure, without a unit.
     *
     * This is the only path display code should use for a stored amount: it applies the
     * [Money] conversion and the grouping together, so no caller can format rials by mistake.
     */
    fun formatRialAsToman(rial: Long, site: String = "unknown"): String =
        formatGrouped(Money.rialToToman(rial, site))

    /**
     * Parses a user-typed toman string into toman, tolerating Persian digits, `٬` separators and
     * incidental whitespace. Returns null when the field holds nothing numeric.
     */
    fun parseTomanInput(input: String): Long? {
        val normalized = toLatinDigits(input)
            .filter { it in '0'..'9' }
        if (normalized.isEmpty()) return null
        return normalized.toLongOrNull()
    }
}
