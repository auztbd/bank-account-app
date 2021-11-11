package org.example.banca.transaction

import org.example.banca.error.DatabaseErrorCode
import org.example.banca.error.DatabaseException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
@Transactional
class TransactionRepository {

    fun findById(id: UUID): Transaction? {
        return TransactionTable.select {
            TransactionTable.id eq id
        }.firstOrNull()?.toEntity()
    }

    fun findByIban(iban: String, sortOrder: SortOrder = SortOrder.DESC): List<Transaction> {
        return TransactionTable.select {
            (TransactionTable.isComplete eq true) and (
                    (TransactionTable.originIban eq iban) or (TransactionTable.destinationIban eq iban))
        }.orderBy(TransactionTable.createdAt to sortOrder).map { it.toEntity() }
    }

    fun saveTransaction(transaction: Transaction): Transaction? {
        return try {
            TransactionTable.insert {
                it[id] = transaction.id
                it[amount] = transaction.amount
                it[originIban] = transaction.originIban
                it[destinationIban] = transaction.destinationIban
                it[reason] = transaction.reason
                it[createdAt] = DateTime(transaction.createdAt.toEpochMilli())
                it[isComplete] = transaction.isComplete
            }.resultedValues?.firstOrNull()?.toEntity()
        } catch (e: ExposedSQLException) {
            throw DatabaseException(
                code = DatabaseErrorCode.ALREADY_EXISTS,
                message = "transaction cannot be saved for id=${transaction.id}"
            )
        }
    }

    fun markTransactionAsComplete(id: UUID) = try {
        TransactionTable.update({ TransactionTable.id eq id }) {
            it[isComplete] = true
        }
    } catch (e: ExposedSQLException) {
        throw DatabaseException(
            code = DatabaseErrorCode.UPDATE_FAILED,
            message = "transaction cannot be saved for id=$id"
        )
    }
}

private fun ResultRow.toEntity(): Transaction {
    return Transaction(
        id = this[TransactionTable.id],
        amount = this[TransactionTable.amount],
        originIban = this[TransactionTable.originIban],
        destinationIban = this[TransactionTable.destinationIban],
        reason = this[TransactionTable.reason],
        createdAt = Instant.ofEpochMilli(this[TransactionTable.createdAt].millis),
        isComplete = this[TransactionTable.isComplete]
    )
}
