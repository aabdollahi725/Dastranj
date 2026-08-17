package ir.dastranj.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dastranj.app.data.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home.
 *
 * Deliberately thin: the balance shown on each card is computed by the database query, not here.
 * Summing transactions in Kotlin would both duplicate the sign convention and pull the whole
 * transaction table into memory to draw five cards (CLAUDE.md §7).
 *
 * The formatted strings are built here rather than in the composable so they are produced once per
 * emission instead of once per recomposition, and so the formatting rules stay testable.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Strings the formatter needs but cannot look up itself, since it is Android-free.
     * Supplied once by the screen from resources.
     */
    private var cashLabel: String = ""
    private var currencyUnit: String = ""

    private var started = false

    /**
     * Starts observing once the screen has handed over its localised strings.
     *
     * Guarded so a recomposition cannot start a second collection — that would leave two
     * collectors writing the same state.
     */
    fun start(cashLabel: String, currencyUnit: String) {
        this.cashLabel = cashLabel
        this.currencyUnit = currencyUnit
        if (started) return
        started = true

        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { accounts ->
                _uiState.update {
                    HomeUiState(
                        accounts = accounts.map { account ->
                            val balanceText = ir.dastranj.app.ui.util.PersianNumbers
                                .formatRialAsToman(
                                    account.currentBalanceRial,
                                    site = "home.accountCard",
                                )
                            AccountCard(
                                id = account.id,
                                title = account.title,
                                maskedLabel = HomeCardFormatter.maskedLabel(
                                    last4 = account.last4,
                                    cashLabel = this.cashLabel,
                                ),
                                balanceText = balanceText,
                                cardTheme = account.cardTheme,
                                contentDescription = HomeCardFormatter.contentDescription(
                                    title = account.title,
                                    balanceText = balanceText,
                                    currencyUnit = this.currencyUnit,
                                ),
                            )
                        },
                        loading = false,
                    )
                }
            }
        }
    }
}
