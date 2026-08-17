package ir.dastranj.app.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HexColorTest {

    @Test
    fun `parses six-digit colours as opaque`() {
        assertThat(HexColor.parseOrNull("#E4813A")).isEqualTo(0xFFE4813AL)
        assertThat(HexColor.parseOrNull("#000000")).isEqualTo(0xFF000000L)
        assertThat(HexColor.parseOrNull("#FFFFFF")).isEqualTo(0xFFFFFFFFL)
    }

    @Test
    fun `parses eight-digit colours keeping their alpha`() {
        assertThat(HexColor.parseOrNull("#80E4813A")).isEqualTo(0x80E4813AL)
        assertThat(HexColor.parseOrNull("#00000000")).isEqualTo(0x00000000L)
    }

    @Test
    fun `accepts a missing hash and surrounding whitespace`() {
        assertThat(HexColor.parseOrNull("E4813A")).isEqualTo(0xFFE4813AL)
        assertThat(HexColor.parseOrNull("  #E4813A  ")).isEqualTo(0xFFE4813AL)
    }

    @Test
    fun `is case insensitive`() {
        assertThat(HexColor.parseOrNull("#e4813a")).isEqualTo(HexColor.parseOrNull("#E4813A"))
    }

    @Test
    fun `returns null rather than throwing on malformed input`() {
        // This is the whole point: a bad stored colour must not crash the category grid.
        val bad = listOf(
            null, "", "   ", "#", "#12345", "#1234567", "#GGGGGG",
            "#E4813A!", "not a colour", "#E4813A E4813A", "rgb(1,2,3)",
        )
        for (value in bad) {
            assertThat(HexColor.parseOrNull(value)).isNull()
        }
    }

    @Test
    fun `falls back instead of failing`() {
        assertThat(HexColor.parseOr("#GGGGGG", HexColor.FALLBACK)).isEqualTo(HexColor.FALLBACK)
        assertThat(HexColor.parseOr(null, HexColor.FALLBACK)).isEqualTo(HexColor.FALLBACK)
        // A valid value still wins over the fallback.
        assertThat(HexColor.parseOr("#E4813A", HexColor.FALLBACK)).isEqualTo(0xFFE4813AL)
    }

    @Test
    fun `handles values that would overflow a signed int`() {
        // 0xFFE4813A exceeds Int.MAX_VALUE; parsing into an Int would have thrown or wrapped.
        val parsed = HexColor.parseOrNull("#FFE4813A")
        assertThat(parsed).isEqualTo(0xFFE4813AL)
        assertThat(parsed!! > Int.MAX_VALUE.toLong()).isTrue()
    }

    @Test
    fun `every seeded design colour parses`() {
        // The colours actually used by the category seed and bank catalogue.
        val designColours = listOf(
            "#E4813A", "#6C63C7", "#0B6FB4", "#3AA9A0", "#C74B8F", "#D4373C",
            "#8A7A2E", "#B4763A", "#5A5E66", "#0F8A64", "#16A97A", "#7C8085", "#1FA3C4",
        )
        for (colour in designColours) {
            assertThat(HexColor.parseOrNull(colour)).isNotNull()
        }
    }
}
