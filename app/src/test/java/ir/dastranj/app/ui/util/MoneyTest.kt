package ir.dastranj.app.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * CLAUDE.md §12 requires a unit test for the rial → toman conversion.
 *
 * The rule is that no rounding ever happens, so these tests exist to prove the invariant that
 * makes that true: every value written through [Money.tomanToRial] is a multiple of 10, therefore
 * every read back through [Money.rialToToman] is exact.
 */
class MoneyTest {

    @Before
    fun resetReporter() {
        Money.setIntegrityReporter { }
    }

    @Test
    fun `toman to rial to toman is lossless across the specified range`() {
        // The exact cases specified for this test.
        val cases = listOf(0L, 1L, 999L, 1_000_000L, Long.MAX_VALUE / 10)

        for (toman in cases) {
            val rial = Money.tomanToRial(toman)
            assertThat(Money.rialToToman(rial)).isEqualTo(toman)
        }
    }

    @Test
    fun `every stored value is a multiple of ten`() {
        val cases = listOf(0L, 1L, 7L, 999L, 1_000_000L, Long.MAX_VALUE / 10)

        for (toman in cases) {
            assertThat(Money.tomanToRial(toman) % 10).isEqualTo(0L)
        }
    }

    @Test
    fun `round trip is lossless for an exhaustive low range`() {
        // Sub-1000 values are where a rounding bug would hide, so cover every one of them.
        for (toman in 0L..1000L) {
            assertThat(Money.rialToToman(Money.tomanToRial(toman))).isEqualTo(toman)
        }
    }

    @Test
    fun `max toman is the largest value representable in rial`() {
        assertThat(Money.MAX_TOMAN).isEqualTo(Long.MAX_VALUE / 10)
        // At the boundary the conversion must still be exact, not silently overflow.
        assertThat(Money.rialToToman(Money.tomanToRial(Money.MAX_TOMAN))).isEqualTo(Money.MAX_TOMAN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toman above the representable range is rejected rather than overflowing`() {
        Money.tomanToRial(Money.MAX_TOMAN + 1)
    }

    @Test
    fun `defensive branch truncates toward zero and never up`() {
        // This must not happen in v1. If it does, the display must still never overstate a balance.
        assertThat(Money.rialToToman(19)).isEqualTo(1L)
        assertThat(Money.rialToToman(11)).isEqualTo(1L)
        assertThat(Money.rialToToman(-19)).isEqualTo(-1L)
        assertThat(Money.rialToToman(-11)).isEqualTo(-1L)
    }

    @Test
    fun `integrity reporter fires only for non-multiples and carries no amount`() {
        val sites = mutableListOf<String>()
        Money.setIntegrityReporter { site -> sites += site }

        Money.rialToToman(20, site = "clean")
        assertThat(sites).isEmpty()

        Money.rialToToman(19, site = "dirty")
        assertThat(sites).containsExactly("dirty")
    }

    @Test
    fun `aggregates convert once after summing in rial`() {
        // CLAUDE.md §2: never convert to toman before summing. Converting each term first would let
        // each one round independently; this asserts the required order produces the exact total.
        val rials = listOf(84_200_000L, 32_100_000L, 12_150_000L, 5_400_000L, 1_250_000L)

        val convertOnce = Money.rialToToman(rials.sum())
        val convertEach = rials.sumOf { Money.rialToToman(it) }

        assertThat(convertOnce).isEqualTo(13_510_000L)
        // With a multiple-of-10 invariant the two agree — which is the point of the invariant.
        assertThat(convertEach).isEqualTo(convertOnce)
    }
}
