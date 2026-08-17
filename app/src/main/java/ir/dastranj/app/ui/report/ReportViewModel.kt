package ir.dastranj.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dastranj.app.data.db.entity.CategoryKind
import ir.dastranj.app.data.repository.CategoryRepository
import ir.dastranj.app.data.repository.TransactionRepository
import ir.dastranj.app.domain.date.JalaliDate
import ir.dastranj.app.domain.date.JalaliDateFormatter
import ir.dastranj.app.domain.report.ChartAxis
import ir.dastranj.app.domain.report.ReportInsight
import ir.dastranj.app.domain.report.ReportInsightCalculator
import ir.dastranj.app.ui.transaction.CategoryOption
import ir.dastranj.app.ui.util.Money
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val year: Int,
    val categories: List<CategoryOption> = emptyList(),
    /** null means «همهٔ دسته‌ها». */
    val selectedCategoryId: Long? = null,
    /** Twelve monthly totals in toman; null for a month that has not happened. */
    val monthlyTotals: List<Long?> = List(12) { null },
    val axisTopToman: Long = 1_000_000L,
    val selectedMonthIndex: Int? = null,
    val insight: ReportInsight = ReportInsight.NoData,
    val loading: Boolean = true,
) {
    val canGoForward: Boolean get() = year < currentJalaliYear()

    val hasAnyData: Boolean get() = monthlyTotals.any { it != null }

    companion object {
        fun currentJalaliYear(): Int =
            JalaliDateFormatter.fromEpochMillis(System.currentTimeMillis()).year

        fun currentJalaliMonthIndex(): Int =
            JalaliDateFormatter.fromEpochMillis(System.currentTimeMillis()).month - 1
    }
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportUiState(year = ReportUiState.currentJalaliYear()),
    )
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
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
                    )
                }
            }
        }
        loadSeries()
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, selectedMonthIndex = null) }
        loadSeries()
    }

    fun previousYear() {
        _uiState.update { it.copy(year = it.year - 1, selectedMonthIndex = null) }
        loadSeries()
    }

    fun nextYear() {
        val next = _uiState.value.year + 1
        // Guarded here as well as in the UI, so a state restore cannot land on a future year.
        if (next > ReportUiState.currentJalaliYear()) return
        _uiState.update { it.copy(year = next, selectedMonthIndex = null) }
        loadSeries()
    }

    fun toggleMonth(index: Int) = _uiState.update {
        it.copy(selectedMonthIndex = if (it.selectedMonthIndex == index) null else index)
    }

    /**
     * Loads the year's twelve monthly totals.
     *
     * Twelve range queries rather than one grouped query, because SQLite has no idea what a Jalali
     * month is — grouping would have to happen on Gregorian boundaries, which are the wrong ones.
     * Each query is an indexed range scan with a `SUM`, so the aggregation still happens in SQL
     * (CLAUDE.md §7) rather than by pulling transactions into memory.
     */
    private fun loadSeries() {
        val state = _uiState.value
        val year = state.year
        val categoryId = state.selectedCategoryId

        viewModelScope.launch {
            val currentYear = ReportUiState.currentJalaliYear()
            val currentMonthIndex =
                if (year == currentYear) ReportUiState.currentJalaliMonthIndex() else null

            val totals = (1..12).map { month ->
                val monthIndex = month - 1
                // A month that has not started is null, not zero: an unstarted month is not a month
                // with no spending, and conflating them would drag every average down.
                if (currentMonthIndex != null && monthIndex > currentMonthIndex) {
                    null
                } else {
                    val range = JalaliDateFormatter.monthRange(year * 100 + month)
                    val rial = transactionRepository.totalExpense(
                        startMillis = range.first,
                        endMillis = range.last + 1,
                        categoryId = categoryId,
                    )
                    Money.rialToToman(rial, site = "report.monthTotal")
                }
            }

            val maxToman = totals.filterNotNull().maxOrNull() ?: 0L

            _uiState.update {
                it.copy(
                    monthlyTotals = totals,
                    axisTopToman = ChartAxis.axisTop(maxToman),
                    insight = ReportInsightCalculator.calculate(
                        // The calculator works in whatever unit it is given; toman throughout here.
                        monthlyTotals = totals,
                        currentMonthIndex = currentMonthIndex,
                    ),
                    loading = false,
                )
            }
        }
    }
}
