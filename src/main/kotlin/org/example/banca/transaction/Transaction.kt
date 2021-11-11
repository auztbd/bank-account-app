package org.example.banca.transaction

import org.example.banca.transaction.TransactionType.INCOMING
import org.example.banca.transaction.TransactionType.OUTGOING
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime
import java.math.BigDecimal

data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val amount: BigDecimal,
    val originIban: String,
    val destinationIban: String,
    val reason: String?,
    val createdAt: Instant = Instant.now(),
    val isComplete: Boolean = false
) {
    fun summarize(perspectiveIban: String): TransactionSummary {
        require(perspectiveIban == originIban || perspectiveIban == destinationIban) {
            "invalid perspective iban $perspectiveIban"
        }
        return TransactionSummary(
            id = id,
            amount = amount,
            type = if (perspectiveIban == originIban) OUTGOING else INCOMING,
            iban = if (perspectiveIban == originIban) destinationIban else originIban,
            reason = reason,
            createdAt = createdAt
        )
    }
}

data class TransactionSummary(
    val id: UUID,
    val amount: BigDecimal,
    val type: TransactionType,
    val iban: String,
    val reason: String?,
    val createdAt: Instant
)

enum class TransactionType {
    INCOMING,
    OUTGOING
}

object TransactionTable : Table("transaction") {
    val id = uuid("id")
    val amount = decimal("amount", 15, 2)
    val originIban = varchar("origin_iban", 64)
    val destinationIban = varchar("destination_iban", 64)
    val reason = varchar("reason", 128).nullable()
    val createdAt = datetime("created_at")
    val isComplete = bool("is_complete")

    override val primaryKey: PrimaryKey = PrimaryKey(id, name = "transaction_pkey")
}
