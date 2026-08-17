package ir.dastranj.app.domain.money

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AmountInWordsTest {

    @Test
    fun `spells the design's own sample amounts`() {
        // ۴۸۰٬۰۰۰ is the pre-filled amount in the add-transaction design.
        assertThat(AmountInWords.spell(480_000)).isEqualTo("چهارصد و هشتاد هزار تومان")
        // ۱۲٬۴۵۰٬۰۰۰ is the pre-filled balance in the add-account design.
        assertThat(AmountInWords.spell(12_450_000))
            .isEqualTo("دوازده میلیون و چهارصد و پنجاه هزار تومان")
        // ۴٬۸۰۰٬۰۰۰ is the edit-mode balance.
        assertThat(AmountInWords.spell(4_800_000)).isEqualTo("چهار میلیون و هشتصد هزار تومان")
    }

    @Test
    fun `distinguishes amounts that differ only by a zero`() {
        // This is the whole point of the feature: these are trivially confused as digits.
        assertThat(AmountInWords.spell(480_000)).isNotEqualTo(AmountInWords.spell(4_800_000))
        assertThat(AmountInWords.spell(50_000)).isEqualTo("پنجاه هزار تومان")
        assertThat(AmountInWords.spell(500_000)).isEqualTo("پانصد هزار تومان")
        assertThat(AmountInWords.spell(5_000_000)).isEqualTo("پنج میلیون تومان")
    }

    @Test
    fun `spells the quick-amount chips`() {
        // QUICK = [50000, 100000, 200000, 500000] in the add-transaction design.
        assertThat(AmountInWords.spell(50_000)).isEqualTo("پنجاه هزار تومان")
        assertThat(AmountInWords.spell(100_000)).isEqualTo("صد هزار تومان")
        assertThat(AmountInWords.spell(200_000)).isEqualTo("دویست هزار تومان")
        assertThat(AmountInWords.spell(500_000)).isEqualTo("پانصد هزار تومان")
    }

    @Test
    fun `handles the irregular teens`() {
        assertThat(AmountInWords.spell(10)).isEqualTo("ده تومان")
        assertThat(AmountInWords.spell(11)).isEqualTo("یازده تومان")
        assertThat(AmountInWords.spell(15)).isEqualTo("پانزده تومان")
        assertThat(AmountInWords.spell(19)).isEqualTo("نوزده تومان")
        // Twenty is regular again.
        assertThat(AmountInWords.spell(20)).isEqualTo("بیست تومان")
        assertThat(AmountInWords.spell(21)).isEqualTo("بیست و یک تومان")
    }

    @Test
    fun `handles hundreds and their conjunctions`() {
        assertThat(AmountInWords.spell(100)).isEqualTo("صد تومان")
        assertThat(AmountInWords.spell(101)).isEqualTo("صد و یک تومان")
        assertThat(AmountInWords.spell(110)).isEqualTo("صد و ده تومان")
        assertThat(AmountInWords.spell(999)).isEqualTo("نهصد و نود و نه تومان")
    }

    @Test
    fun `skips empty digit groups instead of emitting a stray scale word`() {
        // 1,000,001 has an empty thousands group — it must not read «یک میلیون و هزار و یک».
        assertThat(AmountInWords.spell(1_000_001)).isEqualTo("یک میلیون و یک تومان")
        assertThat(AmountInWords.spell(1_000_000)).isEqualTo("یک میلیون تومان")
    }

    @Test
    fun `spells every scale word`() {
        assertThat(AmountInWords.spell(1_000)).isEqualTo("یک هزار تومان")
        assertThat(AmountInWords.spell(1_000_000)).isEqualTo("یک میلیون تومان")
        assertThat(AmountInWords.spell(1_000_000_000)).isEqualTo("یک میلیارد تومان")
        assertThat(AmountInWords.spell(1_000_000_000_000)).isEqualTo("یک هزار میلیارد تومان")
    }

    @Test
    fun `returns empty for values with nothing to say`() {
        // The field shows its hint rather than a spelled-out zero.
        assertThat(AmountInWords.spell(0)).isEmpty()
        assertThat(AmountInWords.spell(-5)).isEmpty()
        // Beyond the design's scale table.
        assertThat(AmountInWords.spell(1_000_000_000_000_000)).isEmpty()
    }

    @Test
    fun `never emits a dangling or doubled conjunction`() {
        // A malformed join is the most likely bug here, so sweep a wide range for one.
        val samples = buildList {
            addAll(1L..1200L)
            addAll(listOf(5_000L, 10_500L, 100_100L, 909_090L, 1_010_101L, 20_000_000L))
        }
        for (value in samples) {
            val words = AmountInWords.spell(value)
            assertThat(words).isNotEmpty()
            assertThat(words).doesNotContain("  ")
            assertThat(words).doesNotContain("و و")
            assertThat(words.trim()).isEqualTo(words)
            assertThat(words.startsWith("و")).isFalse()
            assertThat(words.endsWith("تومان")).isTrue()
        }
    }
}
