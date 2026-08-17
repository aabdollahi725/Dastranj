package ir.dastranj.app.ui.util

/**
 * Rial ↔ toman conversion.
 *
 * ## The rule (CLAUDE.md §2)
 *
 * There is no rounding. The problem is designed out rather than handled:
 *
 * - **Storage** — all amounts are `Long` rials. Never `Float`, `Double` or `BigDecimal`.
 * - **Input** — the user types toman; [tomanToRial] multiplies by 10. Every value that reaches the
 *   database is therefore a multiple of 10.
 * - **Display** — [rialToToman] divides by 10. Because every stored value is a multiple of 10 this
 *   division is always exact, so no rounding, truncation or remainder ever occurs.
 * - **Aggregates** — sum in rials at the SQL level, then convert once here. Never convert to toman
 *   before summing, or each term rounds independently and the total drifts.
 *
 * ## Why this lives in the UI layer
 *
 * CLAUDE.md §2 requires the conversion to happen only at the display layer — never in `data` or
 * `domain`. Both directions are display-layer concerns: [rialToToman] formats for output and
 * [tomanToRial] parses user input. The file has no Android imports, so it stays JVM-unit-testable
 * despite living under `ui`.
 */
object Money {

    /** Rials per toman. */
    const val RIAL_PER_TOMAN: Long = 10L

    /**
     * The largest toman value that can be stored without overflowing a `Long` of rials.
     * `Long.MAX_VALUE / 10` = 922,337,203,685,477,580.
     */
    const val MAX_TOMAN: Long = Long.MAX_VALUE / RIAL_PER_TOMAN

    /**
     * Reports a stored value that is not a multiple of 10.
     *
     * This must not happen in v1: it means something wrote to the database outside the intended
     * path. It is an error *condition*, not a crash — the user still gets a correct-to-the-toman
     * figure — so it is reported rather than thrown.
     *
     * The amount is deliberately not part of the signal. CLAUDE.md §2 forbids logging any amount
     * even in debug, so the reporter receives no value at all; the caller identifies the site
     * instead.
     */
    fun interface IntegrityReporter {
        fun onNonMultipleOfTen(site: String)
    }

    private var reporter: IntegrityReporter = IntegrityReporter { }

    /** Installed once at app startup. */
    fun setIntegrityReporter(value: IntegrityReporter) {
        reporter = value
    }

    /**
     * Converts stored rials to toman for display.
     *
     * Exact for every value written through [tomanToRial]. If a non-multiple of 10 somehow reaches
     * here, truncates toward zero — never up, so a balance is never overstated — and reports it.
     *
     * @param site short identifier of the call site, used only if the defensive branch fires.
     */
    fun rialToToman(rial: Long, site: String = "unknown"): Long {
        if (rial % RIAL_PER_TOMAN != 0L) {
            reporter.onNonMultipleOfTen(site)
        }
        // Kotlin's Long division already truncates toward zero, which is the required direction for
        // negatives as well as positives.
        return rial / RIAL_PER_TOMAN
    }

    /**
     * Converts user-entered toman to the rials that get stored.
     *
     * @throws IllegalArgumentException if the value would overflow `Long`. The amount-entry field
     *   caps input length well below this, so this guards a programming error, not user input —
     *   and the message carries no amount.
     */
    fun tomanToRial(toman: Long): Long {
        require(toman <= MAX_TOMAN && toman >= -MAX_TOMAN) {
            "toman value exceeds the representable rial range"
        }
        return toman * RIAL_PER_TOMAN
    }
}
