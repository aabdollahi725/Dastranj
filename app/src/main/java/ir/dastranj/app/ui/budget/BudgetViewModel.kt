package ir.dastranj.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dastranj.app.data.repository.BudgetRepository
import ir.dastranj.app.domain.budget.BudgetCalculator
import ir.dastranj.app.domain.budget.BudgetLevel
import ir.dastranj.app.domain.budget.EvaluateBudgetAlertsUseCase
import ir.dastranj.app.domain.date.JalaliDate
import ir.dastranj.app.domain.date.JalaliDateFormatter
import ir.dastranj.app.ui.util.Money
import ir.dastranj.app.ui.util.PersianNumbers
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row on the Budget screen, already formatted. */
data class BudgetRow(
    val id: Long,
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val spentText: String,
    val limitText: String,
    val percentUsed: Int,
    val barFraction: Float,
    val level: BudgetLevel,
    val thresholdPercent: Int,
    val overspendText: String,
)

data class BudgetUiState(
    val periodYearMonth: Int,
    val monthLabel: String,
    val rows: List<BudgetRow> = emptyList(),
    val loading: Boolean = true,
    /**
     * Future months are never reachable: a budget for a month that has not started has nothing to
     * measure, so the design disables the forward arrow rather than showing an empty period.
     */
    val canGoForward: Boolean = false,
) {
    val showEmptyState: Boolean get() = !loading && rows.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val evaluateAlerts: EvaluateBudgetAlertsUseCase,
) : ViewModel() {

    /** The month currently in view. Starts at today's. */
    private val currentPeriod = MutableStateFlow(todayPeriod())

    val uiState: StateFlow<BudgetUiState> = currentPeriod
        .flatMapLatest { period ->
            // Copy forward any repeating budgets before observing, so the month the user just
            // stepped into is populated by the time the first emission arrives.
            viewModelScope.launch {
                budgetRepository.ensureRepeatedBudgets(period, System.currentTimeMillis())
            }

            budgetRepository.observeForPeriod(period)
                .onEach { budgets -> evaluateAlerts(budgets) }
                .map { budgets ->
                    BudgetUiState(
                        periodYearMonth = period,
                        monthLabel = monthLabel(period),
                        rows = budgets.map { it.toRow() },
                        loading = false,
                        canGoForward = period < todayPeriod(),
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BudgetUiState(
                periodYearMonth = todayPeriod(),
                monthLabel = monthLabel(todayPeriod()),
            ),
        )

    fun goToPreviousMonth() {
        currentPeriod.value = JalaliDate.previousYearMonth(currentPeriod.value)
    }

    fun goToNextMonth() {
        val next = JalaliDate.nextYearMonth(currentPeriod.value)
        // Guarded here as well as in the UI: the forward arrow is disabled, but a state restore
        // could otherwise land the screen on a future month.
        if (next <= todayPeriod()) currentPeriod.value = next
    }

    private fun ir.dastranj.app.data.db.dao.BudgetWithSpend.toRow(): BudgetRow {
        val status = BudgetCalculator.status(spentRial, amountRial, thresholdPercent)
        return BudgetRow(
            id = id,
            categoryName = categoryName,
            iconName = categoryIconName,
            colorHex = categoryColorHex,
            spentText = PersianNumbers.formatGrouped(
                Money.rialToToman(spentRial, site = "budget.spent"),
            ),
            limitText = PersianNumbers.formatGrouped(
                Money.rialToToman(amountRial, site = "budget.limit"),
            ),
            percentUsed = status.percentUsed,
            barFraction = status.barFraction,
            level = status.level,
            thresholdPercent = thresholdPercent,
            overspendText = PersianNumbers.formatGrouped(
                Money.rialToToman(status.overspendRial, site = "budget.overspend"),
            ),
        )
    }

    private fun monthLabel(period: Int): String {
        val (year, month) = JalaliDate.yearMonthParts(period)
        return JalaliDateFormatter.monthName(month) + " " +
            PersianNumbers.toPersianDigits(year.toString())
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        fun todayPeriod(): Int =
            JalaliDateFormatter.fromEpochMillis(System.currentTimeMillis()).yearMonth
    }
}
