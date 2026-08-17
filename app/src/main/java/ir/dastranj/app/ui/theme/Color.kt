package ir.dastranj.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Ports tokens/colors.css verbatim. These are the raw ramps — screens must not reach for them
 * directly; they consume the semantic DastranjColors below (CLAUDE.md §2 forbids hard-coded
 * colour in UI code, and a raw ramp entry is a hard-coded colour by another name).
 */

// Sabz (سبز) — the one Dastranj brand hue. Growth, "in the black", calm.
internal val Sabz50 = Color(0xFFEDFBF4)
internal val Sabz100 = Color(0xFFD3F5E4)
internal val Sabz200 = Color(0xFFA6E9C8)
internal val Sabz300 = Color(0xFF6FDAA7)
internal val Sabz400 = Color(0xFF35C78A)
internal val Sabz500 = Color(0xFF16A97A)
internal val Sabz600 = Color(0xFF0F8A64)
internal val Sabz700 = Color(0xFF0B6B4E)
internal val Sabz800 = Color(0xFF084F3A)
internal val Sabz900 = Color(0xFF053426)

// Ink — near-black, never pure #000.
internal val Ink900 = Color(0xFF1C1D1F)
internal val Ink800 = Color(0xFF2A2C2F)
internal val Ink700 = Color(0xFF3D4045)
internal val Ink600 = Color(0xFF5A5E66)
internal val Ink500 = Color(0xFF787D86)
internal val Ink400 = Color(0xFF9AA0A8)
internal val Ink300 = Color(0xFFC3C7CD)
internal val Ink200 = Color(0xFFE1E4E7)
internal val Ink150 = Color(0xFFECEEF0)
internal val Ink100 = Color(0xFFF2F3F5)
internal val Ink50 = Color(0xFFF7F8F9)
internal val Ink0 = Color(0xFFFFFFFF)

// Accents.
internal val Anar500 = Color(0xFFF0453A) // pomegranate / expense
internal val Anar400 = Color(0xFFF97066)
internal val Anar100 = Color(0xFFFEE4E2)
internal val Tala500 = Color(0xFFFFC531) // gold / savings
internal val Tala400 = Color(0xFFFFD666)
internal val Tala100 = Color(0xFFFFF4D6)
internal val Abi500 = Color(0xFF3B82C4) // blue / informational
internal val Abi100 = Color(0xFFE3EFFA)

/**
 * The semantic colour surface every screen builds against.
 *
 * Mirrors the `--sc-*` custom properties the design files define, because those — not the raw
 * ramps — are what each `.dc.html` screen actually consumed. Keeping the same names makes the
 * design files diffable against this file.
 */
@Immutable
data class DastranjColors(
    val page: Color,
    val card: Color,
    val sunken: Color,
    val inverse: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val faint: Color,
    val onInverse: Color,
    val hairline: Color,
    val ring: Color,
    val brand: Color,
    val brandInk: Color,
    val brandTint: Color,
    val cta: Color,
    val ctaInk: Color,
    val moneyIn: Color,
    val moneyOut: Color,
    val moneySave: Color,
    val moneyNeutral: Color,
    val danger: Color,
    val dangerTint: Color,
    val warning: Color,
    val warningTint: Color,
    val info: Color,
    val infoTint: Color,
    val scrim: Color,
    val isDark: Boolean,
) {
    /**
     * The brand's single flourish. The DS guide caps it at one use per screen — in v1 that is the
     * FAB, and the primary CTA on the three add-screens.
     */
    val brandGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(Color(0xFF17B98C), Color(0xFF45D94F)),
        )
}

/** Light values — the `[data-sc-theme]` block shared by every design file. */
internal val LightDastranjColors = DastranjColors(
    page = Color(0xFFF1F2F3),
    card = Color(0xFFFFFFFF),
    sunken = Color(0xFFF2F3F5),
    inverse = Ink900,
    title = Color(0xFF1C1D1F),
    body = Color(0xFF2A2C2F),
    muted = Color(0xFF787D86),
    faint = Color(0xFF9AA0A8),
    onInverse = Ink0,
    hairline = Color(0xFFECEEF0),
    // The design files set --sc-ring fully transparent in light: the inset ring is a dark-mode
    // affordance, so drawing it in light would add a hairline the design does not have.
    ring = Color(0x001C1D1F),
    brand = Color(0xFF16A97A),
    brandInk = Color(0xFF0B6B4E),
    brandTint = Color(0xFFEDFBF4),
    cta = Color(0xFF1C1D1F),
    ctaInk = Color(0xFFFFFFFF),
    moneyIn = Sabz600,
    moneyOut = Anar500,
    moneySave = Tala500,
    moneyNeutral = Ink500,
    danger = Anar500,
    dangerTint = Anar100,
    warning = Tala500,
    warningTint = Tala100,
    info = Abi500,
    infoTint = Abi100,
    scrim = Color(0x731C1D1F), // rgba(28,29,31,.45)
    isDark = false,
)

/** Dark values — the `[data-sc-theme="dark"]` block shared by every design file. */
internal val DarkDastranjColors = DastranjColors(
    page = Color(0xFF1C1D1F),
    card = Color(0xFF2A2C2F),
    sunken = Color(0x0FFFFFFF), // rgba(255,255,255,.06)
    inverse = Ink0,
    title = Color(0xFFFFFFFF),
    body = Color(0xFFE1E4E7),
    muted = Color(0xFF9AA0A8),
    faint = Color(0xFF787D86),
    onInverse = Ink900,
    hairline = Color(0x14FFFFFF), // rgba(255,255,255,.08)
    ring = Color(0x1AFFFFFF), // rgba(255,255,255,.10)
    brand = Color(0xFF35C78A),
    brandInk = Color(0xFF35C78A),
    brandTint = Color(0x2935C78A), // rgba(53,199,138,.16)
    cta = Color(0xFFFFFFFF),
    ctaInk = Color(0xFF1C1D1F),
    // Money semantics step one rung lighter in dark so they clear contrast on the dark card,
    // per the dark-mode note in chat2 ("رمپ ink و تُن‌های روشن‌تر سبز/گلد/انار (۴۰۰)").
    moneyIn = Sabz400,
    moneyOut = Anar400,
    moneySave = Tala400,
    moneyNeutral = Ink400,
    danger = Anar400,
    dangerTint = Color(0x29F97066),
    warning = Tala400,
    warningTint = Color(0x29FFD666),
    info = Color(0xFF7FB3E3),
    infoTint = Color(0x293B82C4),
    scrim = Color(0xA6000000),
    isDark = true,
)
