package ir.dastranj.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dastranj.app.data.db.entity.BudgetEntity
import ir.dastranj.app.data.db.entity.CategoryKind
import ir.dastranj.app.data.repository.BudgetRepository
import ir.dastranj.app.data.repository.CategoryRepository
import ir.dastranj.app.domain.date.JalaliDateFormatter
import ir.dastranj.app.domain.money.AmountInWords
import ir.dastranj.app.ui.transaction.CategoryOption
import ir.dastranj.app.ui.util.Money
import ir.dastranj.app.ui.util.PersianNumbers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBudgetUiState(
    val categories: List<CategoryOption> = emptyList(),
    /** Categories that already have a budget this period and so cannot be added again. */
    val takenCategoryIds: Set<Long> = emptySet(),
    val selectedCategoryId: Long? = null,
    val amountToman: String = "",
    val thresholdPercent: Int = BudgetEntity.DEFAULT_THRESHOLD_PERCENT,
    val autoRepeat: Boolean = false,
    val categorySheetOpen: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val amountValue: Long get() = amountToman.toLongOrNull() ?: 0L

    val amountFormatted: String
        get() = if (amountToman.isEmpty()) "۰" else PersianNumbers.formatGrouped(amountValue)

    val amountInWords: String get() = AmountInWords.spell(amountValue)

    val selectedCategory: CategoryOption?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    /** Only categories without a budget this period are offered. */
    val availableCategories: List<CategoryOption>
        get() = categories.filter { it.id !in takenCategoryIds }

    val canSave: Boolean
        get() = selectedCategoryId != null && amountValue > 0 && !saving

    companion object {
        /** The design's threshold slider runs 50–95 in steps of 5. */
        const val THRESHOLD_MIN = 50
        const val THRESHOLD_MAX = 95
        const val THRESHOLD_STEP = 5

        /** The ten allowed values: 50, 55, … 95. */
        val THRESHOLD_OPTIONS = (THRESHOLD_MIN..THRESHOLD_MAX step THRESHOLD_STEP).toList()

        /**
         * Intermediate stops for Compose's `Slider`, which counts the two endpoints separately.
         * Ten allowed values means eight stops between them.
         */
        val THRESHOLD_SLIDER_STEPS = THRESHOLD_OPTIONS.size - 2

        const val MAX_AMOUNT_DIGITS = 12
    }
}

@HiltViewModel
class AddBudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBudgetUiState())
    val uiState: StateFlow<AddBudgetUiState> = _uiState.asStateFlow()

    /** Budgets are limits on spending, so only expense categories are offered. */
    private val period = JalaliDateFormatter
        .fromEpochMillis(System.currentTimeMillis())
        .yearMonth

    init {
        viewModelScope.launch {
            val taken = budgetRepository.getForPeriod(period).map { it.categoryId }.toSet()
            categoryRepository.observeByKind(CategoryKind.EXPENSE).collect { categories ->
                _uiState.update { state ->
                    state.copy(
                        categories = categories.map {
                            CategoryOption(
                                id = it.id,
                                name = it.name,
                                iconName = it.iconName,
                                colorHex = it.colorHex,
                                isPrimary = it.isPrimary,
                            )
                        },
                        takenCategoryIds = taken,
                    )
                }
            }
        }
    }

    fun openCategorySheet() = _uiState.update { it.copy(categorySheetOpen = true) }

    fun closeCategorySheet() = _uiState.update { it.copy(categorySheetOpen = false) }

    fun selectCategory(id: Long) = _uiState.update {
        it.copy(selectedCategoryId = id, categorySheetOpen = false)
    }

    fun onKeypadDigit(digit: Char) = _uiState.update { state ->
        if (state.amountToman.isEmpty() && digit == '0') return@update state
        if (state.amountToman.length >= AddBudgetUiState.MAX_AMOUNT_DIGITS) return@update state
        state.copy(amountToman = state.amountToman + digit)
    }

    fun onKeypadDelete() = _uiState.update {
        it.copy(amountToman = it.amountToman.dropLast(1))
    }

    /**
     * Snaps a raw slider position to the nearest allowed threshold.
     *
     * The slider reports a continuous Float even when configured with steps, and rounding it here
     * rather than trusting the widget keeps the stored value exactly one of [THRESHOLD_OPTIONS] —
     * a threshold of 82 would put the budget's warning tick somewhere the design never draws it.
     */
    fun setThresholdFromSlider(rawValue: Float) {
        val snapped = AddBudgetUiState.THRESHOLD_OPTIONS.minBy { option ->
            kotlin.math.abs(option - rawValue)
        }
        setThreshold(snapped)
    }

    fun setThreshold(percent: Int) = _uiState.update {
        it.copy(thresholdPercent = percent.coerceIn(AddBudgetUiState.THRESHOLD_MIN, AddBudgetUiState.THRESHOLD_MAX))
    }

    fun setAutoRepeat(enabled: Boolean) = _uiState.update { it.copy(autoRepeat = enabled) }

    fun save() {
        val state = _uiState.value
        val categoryId = state.selectedCategoryId ?: return
        if (!state.canSave) return

        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            budgetRepository.create(
                categoryId = categoryId,
                periodYearMonth = period,
                // The one place toman becomes rials.
                amountRial = Money.tomanToRial(state.amountValue),
                thresholdPercent = state.thresholdPercent,
                autoRepeat = state.autoRepeat,
                nowMillis = System.currentTimeMillis(),
            )
            _uiState.update { it.copy(saving = false, saved = true) }
        }
    }
}
