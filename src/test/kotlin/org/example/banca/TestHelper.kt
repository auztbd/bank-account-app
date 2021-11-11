package org.example.banca

import org.example.banca.account.Account
import org.example.banca.account.AccountType
import org.example.banca.transaction.Transaction
import org.example.banca.web.DEPOSIT_ORIGIN_IBAN
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

const val CHECKING_IBAN = "DE17120300001425297056"
const val SAVINGS_IBAN = "DE51120300002856756030"
const val PRIVATE_LOAN_IBAN = "DE11120300008319286407"
const val EXTERNAL_IBAN = "BE94967019820607"
const val DEPOSIT_IBAN = "DE38120300005392125312"

val ACCOUNT_HOLDER_ID: UUID = UUID.fromString("0a7b5c40-b9e2-424e-8296-458b238cb83f")

fun checkingAccount() = Account(
    id = UUID.fromString("62ac6f99-ad34-4389-9998-e41f9a987ede"),
    holderId = ACCOUNT_HOLDER_ID,
    iban = CHECKING_IBAN,
    refAccountIban = null,
    balance = BigDecimal.valueOf(1750.83),
    type = AccountType.CHECKING
)

fun savingsAccount() = Account(
    id = UUID.fromString("390dea03-5119-4644-b8ea-8e142fd4aea9"),
    holderId = ACCOUNT_HOLDER_ID,
    iban = SAVINGS_IBAN,
    refAccountIban = CHECKING_IBAN,
    balance = BigDecimal.valueOf(6084.12),
    type = AccountType.SAVINGS
)


fun privateLoanAccount() = Account(
    id = UUID.fromString("79b56b28-bb53-48cf-8b4b-aba531e2a9b3"),
    holderId = ACCOUNT_HOLDER_ID,
    iban = PRIVATE_LOAN_IBAN,
    refAccountIban = null,
    balance = BigDecimal.valueOf(1572.34),
    type = AccountType.PRIVATE_LOAN
)

fun bankTransfer(
    id: UUID? = null,
    originIban: String? = null,
    destinationIban: String? = null
) = Transaction(
    id = id ?: UUID.randomUUID(),
    amount = BigDecimal.valueOf(120.55),
    originIban = originIban ?: CHECKING_IBAN,
    destinationIban = destinationIban ?: SAVINGS_IBAN,
    reason = "becoz am rich"
)

fun deposit() = Transaction(
    id = UUID.fromString("d999da21-6ccc-4aa4-bd8b-839f392e02a3"),
    amount = BigDecimal.valueOf(1250),
    originIban = DEPOSIT_ORIGIN_IBAN,
    destinationIban = CHECKING_IBAN,
    reason = "deposit",
    createdAt = Instant.now().minusSeconds(100_000)
)
