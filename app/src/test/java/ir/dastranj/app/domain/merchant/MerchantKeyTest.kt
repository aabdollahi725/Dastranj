package ir.dastranj.app.domain.merchant

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MerchantKeyTest {

    @Test
    fun `arabic and persian letter forms converge`() {
        // The same shop typed on an Arabic-layout keyboard and a Persian one.
        val arabic = MerchantKey.normalize("كافي شاپ")
        val persian = MerchantKey.normalize("کافی شاپ")

        assertThat(arabic).isEqualTo(persian)
        assertThat(arabic).isNotNull()
    }

    @Test
    fun `zwnj and a plain space converge`() {
        assertThat(MerchantKey.normalize("کافه‌لمیز"))
            .isEqualTo(MerchantKey.normalize("کافه لمیز"))
    }

    @Test
    fun `branch numbers do not split a merchant`() {
        val base = MerchantKey.normalize("سوپرمارکت رفاه")

        assertThat(MerchantKey.normalize("سوپرمارکت رفاه ۲")).isEqualTo(base)
        assertThat(MerchantKey.normalize("سوپرمارکت رفاه 3")).isEqualTo(base)
    }

    @Test
    fun `punctuation and extra whitespace are folded away`() {
        val base = MerchantKey.normalize("نان فانتزی")

        assertThat(MerchantKey.normalize("  نان   فانتزی  ")).isEqualTo(base)
        assertThat(MerchantKey.normalize("نان، فانتزی!")).isEqualTo(base)
    }

    @Test
    fun `diacritics are ignored`() {
        assertThat(MerchantKey.normalize("مَغازه")).isEqualTo(MerchantKey.normalize("مغازه"))
    }

    @Test
    fun `latin text is case folded`() {
        assertThat(MerchantKey.normalize("Cafe Naderi"))
            .isEqualTo(MerchantKey.normalize("cafe naderi"))
    }

    @Test
    fun `nothing usable yields null`() {
        // These must not create a map entry.
        assertThat(MerchantKey.normalize(null)).isNull()
        assertThat(MerchantKey.normalize("")).isNull()
        assertThat(MerchantKey.normalize("   ")).isNull()
        assertThat(MerchantKey.normalize("۱۲۳")).isNull()
        assertThat(MerchantKey.normalize("!!! ---")).isNull()
    }

    @Test
    fun `distinct merchants stay distinct`() {
        // Normalisation must not be so aggressive that different shops collide.
        assertThat(MerchantKey.normalize("کافه لمیز"))
            .isNotEqualTo(MerchantKey.normalize("کافه نادری"))
        assertThat(MerchantKey.normalize("داروخانه"))
            .isNotEqualTo(MerchantKey.normalize("فروشگاه"))
    }

    @Test
    fun `normalisation is idempotent`() {
        // Re-normalising a stored key must produce the same key, or lookups would miss.
        val once = MerchantKey.normalize("كافه‌لميز ۲")
        val twice = MerchantKey.normalize(once)

        assertThat(twice).isEqualTo(once)
    }
}
