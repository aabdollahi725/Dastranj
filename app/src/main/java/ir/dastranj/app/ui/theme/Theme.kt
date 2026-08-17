package ir.dastranj.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/*
 * The custom token surface. CLAUDE.md §2 forbids hard-coded colour, spacing and radius in UI code,
 * so every screen reads from these locals rather than from literals.
 *
 * These are `static` locals: the token set is swapped only when the system theme flips, so paying
 * for per-read change tracking would buy nothing.
 */

private val LocalDastranjColors: ProvidableCompositionLocal<DastranjColors> =
    staticCompositionLocalOf { LightDastranjColors }

private val LocalDastranjTypography: ProvidableCompositionLocal<DastranjTypography> =
    staticCompositionLocalOf { DastranjTypography() }

private val LocalDastranjShapes: ProvidableCompositionLocal<DastranjShapes> =
    staticCompositionLocalOf { DastranjShapes() }

private val LocalDastranjSpacing: ProvidableCompositionLocal<DastranjSpacing> =
    staticCompositionLocalOf { DastranjSpacing() }

private val LocalDastranjMotion: ProvidableCompositionLocal<DastranjMotion> =
    staticCompositionLocalOf { DastranjMotion() }

private val LocalDastranjElevation: ProvidableCompositionLocal<DastranjElevation> =
    staticCompositionLocalOf { DastranjElevation() }

/**
 * Accessor object so call sites read `Dastranj.colors.card` — parallel to `MaterialTheme.*`.
 */
object Dastranj {
    val colors: DastranjColors
        @Composable @ReadOnlyComposable get() = LocalDastranjColors.current

    val type: DastranjTypography
        @Composable @ReadOnlyComposable get() = LocalDastranjTypography.current

    val shapes: DastranjShapes
        @Composable @ReadOnlyComposable get() = LocalDastranjShapes.current

    val spacing: DastranjSpacing
        @Composable @ReadOnlyComposable get() = LocalDastranjSpacing.current

    val motion: DastranjMotion
        @Composable @ReadOnlyComposable get() = LocalDastranjMotion.current

    val elevation: DastranjElevation
        @Composable @ReadOnlyComposable get() = LocalDastranjElevation.current
}

/**
 * The app theme.
 *
 * Two structural decisions worth stating, both from the spec rather than preference:
 *
 * - **Layout direction is forced RTL, not inherited.** PRD §13.3 requires global RTL. Relying on
 *   the locale would leave the layout at the mercy of the device language, and Dastranj is Farsi
 *   only — so RTL is pinned here rather than being a consequence of configuration.
 * - **`darkTheme` follows the system with no in-app override.** PRD §4.2 removed manual theme
 *   selection and CLAUDE.md §2 states the app follows the system theme; there is deliberately no
 *   parameter to force a theme outside of previews and tests.
 */
@Composable
fun DastranjTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkDastranjColors else LightDastranjColors

    // Material 3 still backs a handful of primitives we use (ripple, text selection handles,
    // Switch). Mapping the Dastranj tokens onto its scheme keeps those in the brand rather than
    // dropping Material's purple defaults into an otherwise green product.
    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.brand,
            onPrimary = colors.onInverse,
            background = colors.page,
            onBackground = colors.body,
            surface = colors.card,
            onSurface = colors.body,
            surfaceVariant = colors.sunken,
            onSurfaceVariant = colors.muted,
            error = colors.danger,
            outline = colors.hairline,
        )
    } else {
        lightColorScheme(
            primary = colors.brand,
            onPrimary = Ink0,
            background = colors.page,
            onBackground = colors.body,
            surface = colors.card,
            onSurface = colors.body,
            surfaceVariant = colors.sunken,
            onSurfaceVariant = colors.muted,
            error = colors.danger,
            outline = colors.hairline,
        )
    }

    val typography = DastranjTypography()

    CompositionLocalProvider(
        LocalDastranjColors provides colors,
        LocalDastranjTypography provides typography,
        LocalDastranjShapes provides DastranjShapes(),
        LocalDastranjSpacing provides DastranjSpacing(),
        LocalDastranjMotion provides DastranjMotion(),
        LocalDastranjElevation provides DastranjElevation(),
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = MaterialTheme.typography.copy(
                // Anything that falls through to a Material default still lands on IRANYekanX
                // rather than Roboto, which cannot render Farsi correctly.
                bodyLarge = typography.body,
                bodyMedium = typography.bodySm,
                bodySmall = typography.caption,
                titleLarge = typography.title1,
                titleMedium = typography.title2,
                titleSmall = typography.title3,
                labelLarge = typography.label,
                labelMedium = typography.caption,
                labelSmall = typography.micro,
            ),
            content = content,
        )
    }
}
