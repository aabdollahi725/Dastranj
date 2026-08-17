package ir.dastranj.app.domain.budget

/** Where a budget stands against its limit. */
enum class BudgetLevel {
    /** Below the warning threshold. */
    SAFE,

    /** At or past the threshold but not yet over the limit. */
    WARNING,

    /** Past the limit. */
    EXCEEDED,
}

/**
 * One budget's computed standing.
 *
 * @param percentUsed spend as a percentage of the limit, rounded. May exceed 100.
 * @param barFraction how much of the progress bar to fill, clamped to 1. Distinct from
 *   [percentUsed] because a 300% overspend must not draw a bar three times the width of its track.
 * @param overspendRial how far past the limit, or 0 when within it.
 */
data class BudgetStatus(
    val percentUsed: Int,
    val barFraction: Float,
    val level: BudgetLevel,
    val overspendRial: Long,
) {
    val isFlagged: Boolean get() = level != BudgetLevel.SAFE
}

/**
 * Computes a budget's standing from its spend and limit.
 *
 * Pure and Android-free: this is the arithmetic the entire Budget screen and both notifications
 * key off, so it is unit-tested rather than trusted.
 *
 * Ported from `buildRow` in `Dastranj Budget Screen.dc.html`, with the edge cases that a prototype
 * with fixed sample data never had to handle — a zero limit above all, which in JavaScript yields
 * `Infinity` or `NaN` and would render as literal garbage.
 */
object BudgetCalculator {

    /**
     * @param spentRial total spent against the category this period, in rials. Never negative.
     * @param limitRial the budget, in rials.
     * @param thresholdPercent the warning threshold, e.g. 80.
     */
    fun status(spentRial: Long, limitRial: Long, thresholdPercent: Int): BudgetStatus {
        // A zero or negative limit has no meaningful percentage. The design's `spent / budget`
        // would divide by zero here; treat any spend against it as immediately exceeded, and no
        // spend as safe, rather than showing NaN.
        if (limitRial <= 0L) {
            return BudgetStatus(
                percentUsed = if (spentRial > 0L) OVER_LIMIT_PERCENT else 0,
                barFraction = if (spentRial > 0L) 1f else 0f,
                level = if (spentRial > 0L) BudgetLevel.EXCEEDED else BudgetLevel.SAFE,
                overspendRial = spentRial.coerceAtLeast(0L),
            )
        }

        val spent = spentRial.coerceAtLeast(0L)

        // Rounded to match the design, and computed in Double so a large rial figure cannot
        // overflow partway through the multiplication.
        val percent = Math.round(spent.toDouble() / limitRial.toDouble() * 100.0).toInt()

        val level = when {
            // Strictly greater: spending exactly the budget is not overspending.
            percent > 100 -> BudgetLevel.EXCEEDED
            percent >= thresholdPercent -> BudgetLevel.WARNING
            else -> BudgetLevel.SAFE
        }

        return BudgetStatus(
            percentUsed = percent,
            barFraction = (spent.toDouble() / limitRial.toDouble()).coerceIn(0.0, 1.0).toFloat(),
            level = level,
            overspendRial = (spent - limitRial).coerceAtLeast(0L),
        )
    }

    /** Shown for a budget with a non-positive limit, which cannot have a real percentage. */
    private const val OVER_LIMIT_PERCENT = 100
}
