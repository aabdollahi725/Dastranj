package ir.dastranj.app.domain.transaction

import ir.dastranj.app.data.db.entity.TransactionEntity
import ir.dastranj.app.data.db.entity.TransactionType
import ir.dastranj.app.data.repository.TransactionRepository
import ir.dastranj.app.domain.merchant.MerchantKey
import javax.inject.Inject

/** Everything needed to record one transaction. Amounts are already in rials. */
data class NewTransaction(
    val type: TransactionType,
    val amountRial: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val occurredAt: Long,
    val note: String?,
    val attachmentPath: String?,
)

/**
 * Records a transaction and updates the merchant memory in one step.
 *
 * This exists rather than letting the ViewModel call the repository twice because the two writes
 * are one user action: a transaction saved without its merchant mapping means the next entry for
 * the same shop will not be pre-categorised, which is a silent, hard-to-notice regression.
 *
 * Validation lives here too, so the invariants hold no matter which screen calls it — a UI check
 * alone would be bypassed by any future caller.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {

    /**
     * @return the new transaction's id.
     * @throws IllegalArgumentException if the transaction violates an invariant. These are
     *   programming errors — the screen blocks submission long before this — so they fail loudly
     *   rather than being silently corrected.
     */
    suspend operator fun invoke(input: NewTransaction, nowMillis: Long): Long {
        validate(input)

        val id = transactionRepository.insert(
            TransactionEntity(
                type = input.type,
                amountRial = input.amountRial,
                accountId = input.accountId,
                toAccountId = input.toAccountId.takeIf { input.type == TransactionType.TRANSFER },
                categoryId = input.categoryId.takeIf { input.type != TransactionType.TRANSFER },
                occurredAt = input.occurredAt,
                note = input.note?.trim()?.ifBlank { null },
                attachmentPath = input.attachmentPath,
                createdAt = nowMillis,
            ),
        )

        // Only categorised entries teach the map anything; a transfer has no category to learn.
        val categoryId = input.categoryId
        if (input.type != TransactionType.TRANSFER && categoryId != null) {
            MerchantKey.normalize(input.note)?.let { key ->
                transactionRepository.rememberMerchant(key, categoryId, nowMillis)
            }
        }

        return id
    }

    private fun validate(input: NewTransaction) {
        require(input.amountRial > 0) { "amount must be positive" }
        // The sign convention depends on this: amounts are stored positive and direction comes
        // from the type, so a negative amount here would corrupt every aggregate.
        require(input.amountRial % 10 == 0L) { "amount must be a whole number of toman" }

        when (input.type) {
            TransactionType.TRANSFER -> {
                requireNotNull(input.toAccountId) { "a transfer needs a destination account" }
                require(input.toAccountId != input.accountId) {
                    "a transfer must move money between two different accounts"
                }
            }
            else -> requireNotNull(input.categoryId) { "a non-transfer needs a category" }
        }
    }
}
