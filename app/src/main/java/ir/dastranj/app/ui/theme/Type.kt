package ir.dastranj.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import ir.dastranj.app.R

/**
 * IRANYekanX FaNum — the only family in the product (PRD §13.3).
 *
 * The FaNum cut renders Persian numerals natively in the numeral slots, which is why the DS guide
 * says never to hand-substitute digits for *display* text. Data that is formatted before it
 * reaches the text layer still goes through [ir.dastranj.app.ui.util.PersianNumbers] — see the
 * note there about why both exist.
 *
 * Five working weights, matching the DS guide. The Thin/UltraLight/Light/ExtraBold/ExtraBlack cuts
 * exist in the source bundle but the guide marks them decorative-only, so they are not shipped —
 * that keeps five TTFs out of the APK for PRD §13.4's <10MB target.
 */
val IranYekanX = FontFamily(
    Font(R.font.iranyekanx_regular, FontWeight.Normal),
    Font(R.font.iranyekanx_medium, FontWeight.Medium),
    Font(R.font.iranyekanx_demibold, FontWeight.SemiBold),
    Font(R.font.iranyekanx_bold, FontWeight.Bold),
    Font(R.font.iranyekanx_black, FontWeight.Black),
)

/*
 * Persian glyphs sit low and have tall ascenders, so line-height runs generous and
 * letter-spacing is always 0. Trim = None keeps the generous leading actually visible instead of
 * letting Compose clip the first and last line's extra space, which is what makes Farsi text
 * cramped in a naive port.
 */
private val PersianLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun dastranjStyle(
    size: Int,
    lineHeightMultiplier: Double,
    weight: FontWeight,
) = TextStyle(
    fontFamily = IranYekanX,
    fontSize = size.sp,
    lineHeight = (size * lineHeightMultiplier).sp,
    fontWeight = weight,
    letterSpacing = 0.sp,
    lineHeightStyle = PersianLineHeightStyle,
)

/**
 * The type scale from tokens/typography.css. Nothing below 11sp ships.
 *
 * Sizes are in sp, not dp, so the whole scale grows with the system font setting — PRD §13.5
 * requires the layout to survive 200% zoom, which is only possible if type is scalable to begin
 * with.
 */
@Immutable
data class DastranjTypography(
    val display: TextStyle = dastranjStyle(34, 1.45, FontWeight.ExtraBold),
    val amount: TextStyle = dastranjStyle(30, 1.30, FontWeight.Black),
    val title1: TextStyle = dastranjStyle(22, 1.55, FontWeight.Bold),
    val title2: TextStyle = dastranjStyle(18, 1.60, FontWeight.SemiBold),
    val title3: TextStyle = dastranjStyle(16, 1.65, FontWeight.SemiBold),
    val body: TextStyle = dastranjStyle(15, 1.85, FontWeight.Normal),
    val bodySm: TextStyle = dastranjStyle(14, 1.80, FontWeight.Normal),
    val label: TextStyle = dastranjStyle(13, 1.70, FontWeight.Medium),
    val caption: TextStyle = dastranjStyle(12, 1.65, FontWeight.Normal),
    val micro: TextStyle = dastranjStyle(11, 1.55, FontWeight.Normal),
)
