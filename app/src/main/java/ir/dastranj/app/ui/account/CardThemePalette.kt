package ir.dastranj.app.ui.account

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import ir.dastranj.app.R
import ir.dastranj.app.ui.theme.Dastranj

/**
 * The colours of one account-card theme.
 *
 * These are **not** semantic theme colours and deliberately do not flip with dark mode. A card
 * theme is the user's choice about that one card, so «جوهری» stays dark on a light page and
 * «سفید» stays light on a dark one — which is what makes the four themes distinguishable at all.
 *
 * The one exception is [CardTheme.WHITE], whose background follows the card surface: a pure-white
 * card on a dark page would be the only blinding element in the app, and the design's own token for
 * it (`--ac-card`) is a surface reference rather than a fixed white.
 */
@Immutable
data class CardPalette(
    val background: Color,
    val ink: Color,
    val subdued: Color,
    val hairline: Color,
    val tileBackground: Color,
)

/** Values ported from the `THEMES` table in `Dastranj Add Account Screen.dc.html`. */
@Composable
@ReadOnlyComposable
fun CardTheme.palette(): CardPalette = when (this) {
    CardTheme.WHITE -> CardPalette(
        background = Dastranj.colors.card,
        ink = Dastranj.colors.title,
        subdued = Dastranj.colors.muted,
        hairline = Dastranj.colors.title.copy(alpha = 0.10f),
        tileBackground = Dastranj.colors.sunken,
    )
    CardTheme.GREEN -> CardPalette(
        background = Color(0xFFEDFBF4), // --sabz-50
        ink = Color(0xFF053426), // --sabz-900
        subdued = Color(0xFF0B6B4E), // --sabz-700
        hairline = Color(0xFF0B6B4E).copy(alpha = 0.16f),
        tileBackground = Color(0xFF0F8A64).copy(alpha = 0.12f),
    )
    CardTheme.GOLD -> CardPalette(
        background = Color(0xFFFFF4D6), // --tala-100
        ink = Color(0xFF1C1D1F),
        subdued = Color(0xFF5A5E66),
        hairline = Color(0xFF1C1D1F).copy(alpha = 0.12f),
        tileBackground = Color(0xFF8A6200).copy(alpha = 0.12f),
    )
    CardTheme.INK -> CardPalette(
        background = Color(0xFF1C1D1F), // --ink-900
        ink = Color(0xFFFFFFFF),
        subdued = Color(0xFFC3C7CD), // --ink-300
        hairline = Color(0xFFFFFFFF).copy(alpha = 0.18f),
        tileBackground = Color(0xFFFFFFFF).copy(alpha = 0.12f),
    )
}

@StringRes
fun CardTheme.labelRes(): Int = when (this) {
    CardTheme.WHITE -> R.string.add_account_theme_white
    CardTheme.GREEN -> R.string.add_account_theme_green
    CardTheme.GOLD -> R.string.add_account_theme_gold
    CardTheme.INK -> R.string.add_account_theme_ink
}

/** Resolves a stored [CardTheme.storageKey], falling back to the default rather than throwing. */
fun cardThemeFromKey(key: String?): CardTheme =
    CardTheme.entries.firstOrNull { it.storageKey == key } ?: CardTheme.WHITE
