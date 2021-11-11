package org.example.banca.account

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.banca.transaction.TransactionTable
import java.math.BigDecimal
import java.time.Instant
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.UUID

data class Account(
    val id: UUID = UUID.randomUUID(),
    val holderId: UUID,
    val iban: String,
    val refAccountIban: String?,
    val type: AccountType,
    val balance: BigDecimal = BigDecimal.ZERO,
    val isLocked: Boolean = false,
    val createdAt: Instant = Instant.now(),
)

enum class AccountType {
    @JsonProperty("checking")
    CHECKING,

    @JsonProperty("savings")
    SAVINGS,

    @JsonProperty("private_loan")
    PRIVATE_LOAN
}

object AccountTable : Table("customer_account") {
    val id = uuid("id")
    val holderId = uuid("holder_id")
    val iban = varchar("iban", 64)
    val refAccountIban = varchar("ref_iban", 64).nullable()
    val type = enumeration("type", AccountType::class)
    val balance = decimal("balance", 15, 2)
    val isLocked = bool("is_locked")
    val createdAt = datetime("created_at")

    override val primaryKey: PrimaryKey = PrimaryKey(TransactionTable.id, name = "customer_account_pkey")
}

data class AccountFilter(
    val holderId: UUID,
    val accountTypes: List<AccountType> = listOf()
)
