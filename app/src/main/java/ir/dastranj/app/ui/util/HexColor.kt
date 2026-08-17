package ir.dastranj.app.ui.util

/**
 * Parses `#RRGGBB` / `#AARRGGBB` colour strings into packed ARGB.
 *
 * ## Why not `android.graphics.Color.parseColor`
 *
 * That method throws `IllegalArgumentException` on anything malformed. Category and bank colours
 * are *stored data*: the seeded ones are well-formed and covered by a test, but a user-created
 * category — or a row edited outside the app — could carry anything. A throw there would crash the
 * whole category grid while rendering, taking out the app's central screen because one tile had a
 * bad colour string.
 *
 * So this is total: it never throws, and callers supply a fallback.
 *
 * Android-free, so the parsing is unit-tested on the JVM.
 */
object HexColor {

    /**
     * @return packed ARGB as a `Long`, or null when [hex] is not a valid colour.
     *
     * A `Long` rather than an `Int` because ARGB values above `0x7FFFFFFF` overflow a signed `Int`;
     * the Compose wrapper narrows it at the call site.
     */
    fun parseOrNull(hex: String?): Long? {
        if (hex == null) return null

        val body = hex.trim().removePrefix("#")
        if (body.length != RGB_LENGTH && body.length != ARGB_LENGTH) return null
        if (!body.all { it.isHexDigit() }) return null

        val value = body.toLongOrNull(radix = 16) ?: return null

        // A six-digit value carries no alpha, so it is opaque.
        return if (body.length == RGB_LENGTH) value or OPAQUE_ALPHA else value
    }

    /** [parseOrNull] with a caller-supplied fallback. */
    fun parseOr(hex: String?, fallback: Long): Long = parseOrNull(hex) ?: fallback

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private const val RGB_LENGTH = 6
    private const val ARGB_LENGTH = 8
    private const val OPAQUE_ALPHA = 0xFF000000L

    /** Neutral grey — the design's «سایر» colour, used when a stored value cannot be parsed. */
    const val FALLBACK: Long = 0xFF7C8085L
}
