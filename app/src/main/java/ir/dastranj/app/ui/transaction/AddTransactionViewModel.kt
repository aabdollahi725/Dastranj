package ir.dastranj.app.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dastranj.app.data.db.entity.CategoryKind
import ir.dastranj.app.data.db.entity.TransactionType
import ir.dastranj.app.data.repository.AccountRepository
import ir.dastranj.app.data.repository.CategoryRepository
import ir.dastranj.app.data.repository.TransactionRepository
import ir.dastranj.app.data.seed.BankCatalog
import ir.dastranj.app.domain.merchant.MerchantKey
import ir.dastranj.app.domain.transaction.AddTransactionUseCase
import ir.dastranj.app.domain.transaction.NewTransaction
import ir.dastranj.app.ui.util.Money
import ir.dastranj.app.ui.util.PersianNumbers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                categoryRepository.observeByKind(CategoryKind.EXPENSE),
                categoryRepository.observeByKind(CategoryKind.INCOME),
                accountRepository.observeActiveAccounts(),
            ) { expense, income, accounts ->
                Triple(expense, income, accounts)
            }.collect { (expense, income, accounts) ->
                _uiState.update { state ->
                    val options = accounts.map { account ->
                        AccountOption(
                            id = account.id,
                            title = account.title,
                            isCash = account.bankId == BankCatalog.CASH_ID,
                        )
                    }
                    state.copy(
                        expenseCategories = expense.toOptions(),
                        incomeCategories = income.toOptions(),
                        accounts = options,
                        // Default to the first account so the common case needs no picking.
                        accountId = state.accountId ?: options.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    private fun List<ir.dastranj.app.data.db.entity.CategoryEntity>.toOptions() = map {
        CategoryOption(
            id = it.id,
            name = it.name,
            iconName = it.iconName,
            colorHex = it.colorHex,
            isPrimary = it.isPrimary,
        )
    }

    /**
     * Switching type clears the category, because the expense and income taxonomies are disjoint —
     * keeping «خوراک» selected after switching to income would leave an expense category attached
     * to an income entry.
     */
    fun setType(type: TransactionType) = _uiState.update {
        if (it.type == type) it else it.copy(
            type = type,
            selectedCategoryId = null,
            keypadOpen = true,
            openSheet = null,
        )
    }

    fun onKeypadDigit(digit: Char) = _uiState.update { state ->
        // A leading zero is meaningless in an amount.
        if (state.amountToman.isEmpty() && digit == '0') return@update state
        if (state.amountToman.length >= AddTransactionUiState.MAX_AMOUNT_DIGITS) return@update state
        state.copy(amountToman = state.amountToman + digit)
    }

    fun onKeypadDelete() = _uiState.update {
        it.copy(amountToman = it.amountToman.dropLast(1))
    }

    /** The design's quick-amount chips (۵۰٬۰۰۰ … ۵۰۰٬۰۰۰) replace the amount outright. */
    fun setQuickAmount(toman: Long) = _uiState.update {
        it.copy(amountToman = toman.toString())
    }

    fun focusAmount() = _uiState.update { it.copy(keypadOpen = true, openSheet = null) }

    fun openSheet(sheet: TransactionSheet) = _uiState.update {
        it.copy(openSheet = sheet, keypadOpen = false)
    }

    fun closeSheet() = _uiState.update { it.copy(openSheet = null, keypadOpen = true) }

    fun selectCategory(categoryId: Long) = _uiState.update {
        it.copy(selectedCategoryId = categoryId, openSheet = null)
    }

    fun selectAccount(accountId: Long) = _uiState.update { state ->
        state.copy(
            accountId = accountId,
            // Keep the two legs distinct: picking the destination as the source swaps them rather
            // than silently creating a transfer to itself.
            transferToAccountId = state.transferToAccountId
                .takeIf { it != accountId } ?: state.accountId,
            openSheet = null,
            keypadOpen = true,
        )
    }

    fun selectTransferTo(accountId: Long) = _uiState.update { state ->
        state.copy(
            transferToAccountId = accountId,
            accountId = state.accountId.takeIf { it != accountId } ?: state.transferToAccountId,
            openSheet = null,
            keypadOpen = true,
        )
    }

    fun swapTransferLegs() = _uiState.update {
        it.copy(accountId = it.transferToAccountId, transferToAccountId = it.accountId)
    }

    fun setDate(epochMillis: Long?) = _uiState.update {
        it.copy(occurredAtMillis = epochMillis, openSheet = null, keypadOpen = true)
    }

    fun setNote(note: String?) {
        _uiState.update { it.copy(note = note, openSheet = null, keypadOpen = true) }
        suggestCategoryFor(note)
    }

    fun setAttachment(path: String?) = _uiState.update {
        it.copy(attachmentPath = path, openSheet = null, keypadOpen = true)
    }

    /**
     * Pre-selects the category this merchant was last filed under.
     *
     * Only fills an *empty* selection — a suggestion must never overwrite a category the user has
     * already chosen for this entry.
     */
    private fun suggestCategoryFor(note: String?) {
        val key = MerchantKey.normalize(note) ?: return
        viewModelScope.launch {
            val suggested = transactionRepository.suggestCategoryFor(key) ?: return@launch
            _uiState.update { state ->
                if (state.selectedCategoryId != null) state
                else state.copy(selectedCategoryId = suggested)
            }
        }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit || state.saving) return

        val accountId = state.accountId ?: return
        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            addTransaction(
                NewTransaction(
                    type = state.type,
                    // The one place toman becomes rials.
                    amountRial = Money.tomanToRial(state.amountValue),
                    accountId = accountId,
                    toAccountId = state.transferToAccountId,
                    categoryId = state.selectedCategoryId,
                    occurredAt = state.occurredAtMillis ?: now,
                    note = state.note,
                    attachmentPath = state.attachmentPath,
                ),
                nowMillis = now,
            )

            _uiState.update { it.copy(saving = false, saved = true) }
        }
    }
}
