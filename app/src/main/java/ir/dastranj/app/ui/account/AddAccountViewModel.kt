package ir.dastranj.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dastranj.app.data.repository.AccountRepository
import ir.dastranj.app.data.seed.Bank
import ir.dastranj.app.data.seed.BankCatalog
import ir.dastranj.app.ui.util.Money
import ir.dastranj.app.ui.util.PersianNumbers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    fun openBankSheet() = _uiState.update {
        // Closing the keypad first stops the sheet from opening behind it.
        it.copy(bankSheetOpen = true, focusedNumericField = null)
    }

    fun closeBankSheet() = _uiState.update { it.copy(bankSheetOpen = false, bankQuery = "") }

    fun onBankQueryChange(query: String) = _uiState.update { it.copy(bankQuery = query) }

    /**
     * Picking a bank pre-fills the title, but only when the user has not written one — overwriting
     * a title they typed would lose their work.
     */
    fun selectBank(bank: Bank) = _uiState.update { state ->
        state.copy(
            selectedBank = bank,
            bankSheetOpen = false,
            bankQuery = "",
            title = state.title.ifBlank {
                if (bank.generic) GENERIC_TITLE_PLACEHOLDER else bank.shortName
            },
        )
    }

    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }

    fun selectTheme(theme: CardTheme) = _uiState.update { it.copy(cardTheme = theme) }

    fun focusField(field: NumericField?) = _uiState.update { it.copy(focusedNumericField = field) }

    /**
     * Accepts only digits and normalises Persian ones, so a paste or a hardware keyboard cannot get
     * a non-numeric character into the field.
     */
    fun onLast4Change(raw: String) = _uiState.update {
        val digits = PersianNumbers.toLatinDigits(raw)
            .filter(Char::isDigit)
            .take(AddAccountUiState.LAST4_LENGTH)
        // Clear the error as soon as the user starts fixing it, rather than making them resubmit.
        it.copy(last4 = digits, showLast4Error = false)
    }

    fun onBalanceChange(raw: String) = _uiState.update {
        val digits = PersianNumbers.toLatinDigits(raw)
            .filter(Char::isDigit)
            .take(AddAccountUiState.MAX_BALANCE_DIGITS)
        it.copy(balanceToman = digits.trimStart('0').ifEmpty { "" })
    }

    /** Appends one digit from the on-screen keypad to whichever field has focus. */
    fun onKeypadDigit(digit: Char) = _uiState.update { state ->
        when (state.focusedNumericField) {
            NumericField.LAST4 -> state.copy(
                last4 = (state.last4 + digit).take(AddAccountUiState.LAST4_LENGTH),
                showLast4Error = false,
            )
            NumericField.BALANCE -> {
                // A leading zero is meaningless in an amount, so it is simply ignored.
                val next = if (state.balanceToman.isEmpty() && digit == '0') {
                    ""
                } else {
                    (state.balanceToman + digit).take(AddAccountUiState.MAX_BALANCE_DIGITS)
                }
                state.copy(balanceToman = next)
            }
            null -> state
        }
    }

    fun onKeypadDelete() = _uiState.update { state ->
        when (state.focusedNumericField) {
            NumericField.LAST4 -> state.copy(last4 = state.last4.dropLast(1), showLast4Error = false)
            NumericField.BALANCE -> state.copy(balanceToman = state.balanceToman.dropLast(1))
            null -> state
        }
    }

    fun save() {
        val state = _uiState.value
        val bank = state.selectedBank

        if (bank == null || state.title.isBlank()) return
        if (state.last4.length != AddAccountUiState.LAST4_LENGTH) {
            _uiState.update { it.copy(showLast4Error = true) }
            return
        }
        if (state.saving) return

        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            // The one place toman becomes rials. Everything below this line is rials.
            val balanceRial = Money.tomanToRial(state.balanceToman.toLongOrNull() ?: 0L)

            val id = accountRepository.create(
                bankId = bank.id,
                title = state.title,
                last4 = state.last4,
                initialBalanceRial = balanceRial,
                cardTheme = state.cardTheme.storageKey,
                nowMillis = System.currentTimeMillis(),
            )

            _uiState.update { it.copy(saving = false, savedAccountId = id) }
        }
    }

    private companion object {
        const val GENERIC_TITLE_PLACEHOLDER = "من"
    }
}
