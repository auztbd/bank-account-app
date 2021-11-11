package org.example.banca.web

import org.example.banca.account.Account
import org.example.banca.account.AccountType
import org.example.banca.util.IbanGenerator
import java.math.BigDecimal
import java.util.UUID

data class AccountInput(
    val holderId: UUID = UUID.randomUUID(),
    val refAccountIban: String? = null,
    val type: AccountType,
    val isLocked: Boolean = false
) {
    fun toAccount(): Account = Account(
        holderId = holderId,
        iban = IbanGenerator.generate(),
        refAccountIban = refAccountIban,
        type = type,
        isLocked = isLocked
    )
}

class DepositInput(
    val amount: BigDecimal,
    val originIban: String = DEPOSIT_ORIGIN_IBAN,
    val destinationIban: String
)

data class TransferInput(
    val amount: BigDecimal,
    val destinationIban: String,
    val bic: String?,
    val reason: String?
)

const val DEPOSIT_ORIGIN_IBAN = "DE38120300005392125312"
