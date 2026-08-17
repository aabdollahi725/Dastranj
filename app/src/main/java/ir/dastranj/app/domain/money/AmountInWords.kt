package ir.dastranj.app.domain.money

/**
 * Spells a toman amount out in Persian words — «چهارصد و هشتاد هزار تومان».
 *
 * Both add-screens show this under the amount field. It is the app's guard against the single most
 * expensive typo in a money app: an extra or missing zero. A user can misread ۴۸۰٬۰۰۰ as ۴٬۸۰۰٬۰۰۰
 * at a glance, but «چهارصد و هشتاد هزار» and «چهار میلیون و هشتصد هزار» are impossible to confuse.
 *
 * Ported from the shared `words()` implementation in the add-account and add-transaction design
 * files, which use identical tables.
 *
 * The input is **toman**, matching what the user typed — not the stored rial value.
 */
object AmountInWords {

    private val ONES = arrayOf(
        "", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه",
    )
    private val TEENS = arrayOf(
        "ده", "یازده", "دوازده", "سیزده", "چهارده",
        "پانزده", "شانزده", "هفده", "هجده", "نوزده",
    )
    private val TENS = arrayOf(
        "", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود",
    )
    private val HUNDREDS = arrayOf(
        "", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد",
    )

    /** Scale words, indexed by group of three digits. */
    private val SCALES = arrayOf("", " هزار", " میلیون", " میلیارد", " هزار میلیارد")

    private const val CONJUNCTION = " و "

    /** The largest amount that has a scale word. Beyond this the design's table runs out. */
    private const val MAX_SUPPORTED = 999_999_999_999_999L

    /**
     * @param toman the amount as typed, in toman.
     * @return the spelled-out amount including the «تومان» unit, or an empty string for zero,
     *   negative, or unsupported input — the field shows its hint instead in those cases.
     */
    fun spell(toman: Long): String {
        if (toman <= 0 || toman > MAX_SUPPORTED) return ""

        // Split into groups of three digits, least significant first.
        val groups = mutableListOf<Int>()
        var remaining = toman
        while (remaining > 0) {
            groups += (remaining % 1000).toInt()
            remaining /= 1000
        }

        val parts = mutableListOf<String>()
        for (index in groups.indices.reversed()) {
            val group = groups[index]
            if (group != 0) parts += spellGroup(group) + SCALES[index]
        }

        return parts.joinToString(CONJUNCTION) + " تومان"
    }

    /** Spells a value 1..999. */
    private fun spellGroup(value: Int): String {
        val hundreds = value / 100
        val rest = value % 100
        val parts = mutableListOf<String>()

        if (hundreds > 0) parts += HUNDREDS[hundreds]

        when {
            rest == 0 -> Unit
            // The teens are irregular in Persian and need their own table.
            rest < 10 -> parts += ONES[rest]
            rest < 20 -> parts += TEENS[rest - 10]
            else -> {
                val tens = rest / 10
                val ones = rest % 10
                parts += if (ones > 0) TENS[tens] + CONJUNCTION + ONES[ones] else TENS[tens]
            }
        }

        return parts.joinToString(CONJUNCTION)
    }
}
