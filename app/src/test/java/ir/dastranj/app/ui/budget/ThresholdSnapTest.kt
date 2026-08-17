package ir.dastranj.app.ui.budget

import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Test

/**
 * The threshold slider's snapping.
 *
 * Compose's `Slider` reports a continuous Float even when configured with discrete steps, so the
 * value is snapped before it is stored. A threshold of 82 would put the budget bar's warning tick
 * somewhere the design never draws it.
 *
 * The snap itself is exercised here as a pure function; the ViewModel applies exactly this rule.
 */
class ThresholdSnapTest {

    private val options = AddBudgetUiState.THRESHOLD_OPTIONS

    private fun snap(raw: Float): Int = options.minBy { abs(it - raw) }

    @Test
    fun `the allowed values are the ten multiples of five from 50 to 95`() {
        assertThat(options).containsExactly(50, 55, 60, 65, 70, 75, 80, 85, 90, 95).inOrder()
    }

    @Test
    fun `slider steps count the stops between the endpoints`() {
        // Compose counts the two endpoints separately, so ten values means eight intermediate
        // stops. Getting this wrong puts the detents at the wrong percentages.
        assertThat(AddBudgetUiState.THRESHOLD_SLIDER_STEPS).isEqualTo(8)
        assertThat(AddBudgetUiState.THRESHOLD_SLIDER_STEPS + 2).isEqualTo(options.size)
    }

    @Test
    fun `exact values snap to themselves`() {
        for (option in options) {
            assertThat(snap(option.toFloat())).isEqualTo(option)
        }
    }

    @Test
    fun `intermediate values snap to the nearest stop`() {
        assertThat(snap(81f)).isEqualTo(80)
        assertThat(snap(83f)).isEqualTo(85)
        assertThat(snap(52.4f)).isEqualTo(50)
        assertThat(snap(52.6f)).isEqualTo(55)
    }

    @Test
    fun `the endpoints hold`() {
        assertThat(snap(49f)).isEqualTo(50)
        assertThat(snap(96f)).isEqualTo(95)
        assertThat(snap(0f)).isEqualTo(50)
        assertThat(snap(1000f)).isEqualTo(95)
    }

    @Test
    fun `every value across the range snaps to an allowed option`() {
        // Sweep the whole range in fine increments — nothing may escape the allowed set.
        var raw = 45f
        while (raw <= 100f) {
            assertThat(options).contains(snap(raw))
            raw += 0.1f
        }
    }

    @Test
    fun `the default threshold is one of the allowed options`() {
        assertThat(options).contains(
            ir.dastranj.app.data.db.entity.BudgetEntity.DEFAULT_THRESHOLD_PERCENT,
        )
    }
}
