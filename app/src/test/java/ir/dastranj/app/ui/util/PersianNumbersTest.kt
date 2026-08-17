package ir.dastranj.app.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * CLAUDE.md §12 requires a test for Persian digit formatting.
 */
class PersianNumbersTest {

    @Test
    fun `grouping uses the persian separator and persian digits`() {
        assertThat(PersianNumbers.formatGrouped(84_200_000)).isEqualTo("۸۴٬۲۰۰٬۰۰۰")
        assertThat(PersianNumbers.formatGrouped(1000)).isEqualTo("۱٬۰۰۰")
        assertThat(PersianNumbers.formatGrouped(999)).isEqualTo("۹۹۹")
        assertThat(PersianNumbers.formatGrouped(0)).isEqualTo("۰")
    }

    @Test
    fun `the separator is the arabic thousands separator not a comma`() {
        // U+066C. A comma here is the single most visible number-formatting error in the product.
        assertThat(PersianNumbers.THOUSANDS_SEPARATOR.code).isEqualTo(0x066C)
        assertThat(PersianNumbers.formatGrouped(1_000_000)).doesNotContain(",")
    }

    @Test
    fun `negatives use the typographic minus sign`() {
        // U+2212, which aligns with the digit weight, not the ASCII hyphen.
        assertThat(PersianNumbers.formatGrouped(-1500)).isEqualTo("−۱٬۵۰۰")
    }

    @Test
    fun `formatting the extreme negative does not overflow`() {
        // Negating Long.MIN_VALUE overflows, so the implementation formats from the string form.
        assertThat(PersianNumbers.formatGrouped(Long.MIN_VALUE))
            .isEqualTo("−۹٬۲۲۳٬۳۷۲٬۰۳۶٬۸۵۴٬۷۷۵٬۸۰۸")
    }

    @Test
    fun `stored rial renders as the toman figure the design shows`() {
        // The Home account card in the design shows ۸۴٬۲۰۰٬۰۰۰ تومان.
        assertThat(PersianNumbers.formatRialAsToman(842_000_000)).isEqualTo("۸۴٬۲۰۰٬۰۰۰")
    }

    @Test
    fun `input parsing accepts persian digits and separators`() {
        assertThat(PersianNumbers.parseTomanInput("۴۸۰٬۰۰۰")).isEqualTo(480_000L)
        assertThat(PersianNumbers.parseTomanInput("480000")).isEqualTo(480_000L)
        assertThat(PersianNumbers.parseTomanInput("۴۸۰,۰۰۰")).isEqualTo(480_000L)
        assertThat(PersianNumbers.parseTomanInput(" ۱۲ ۳۴۵ ")).isEqualTo(12_345L)
    }

    @Test
    fun `input parsing returns null when there is nothing numeric`() {
        assertThat(PersianNumbers.parseTomanInput("")).isNull()
        assertThat(PersianNumbers.parseTomanInput("   ")).isNull()
        assertThat(PersianNumbers.parseTomanInput("تومان")).isNull()
    }

    @Test
    fun `digit conversion round trips`() {
        assertThat(PersianNumbers.toPersianDigits("1405")).isEqualTo("۱۴۰۵")
        assertThat(PersianNumbers.toLatinDigits("۱۴۰۵")).isEqualTo("1405")
        assertThat(PersianNumbers.toLatinDigits(PersianNumbers.toPersianDigits("1405")))
            .isEqualTo("1405")
    }

    @Test
    fun `non digit characters pass through untouched`() {
        assertThat(PersianNumbers.toPersianDigits("۲۶ مرداد ۱۴۰۵"))
            .isEqualTo("۲۶ مرداد ۱۴۰۵")
        assertThat(PersianNumbers.toPersianDigits("card 4556")).isEqualTo("card ۴۵۵۶")
    }
}
