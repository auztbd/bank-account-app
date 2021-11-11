package org.example.banca.account

import org.example.banca.web.AccountInput
import org.example.banca.error.DatabaseErrorCode
import org.example.banca.error.DatabaseException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Repository
@Transactional
class AccountRepository {

    fun find(filter: AccountFilter): List<Account> {
        val query = AccountTable.selectAll().apply {
            andWhere { Op.build { AccountTable.holderId eq filter.holderId } }
            if (filter.accountTypes.isNotEmpty()) {
                andWhere { Op.build { AccountTable.type inList filter.accountTypes } }
            }
        }
        return query.toList().map { it.toEntity() }
    }

    fun findByIban(iban: String): Account? {
        return AccountTable.select {
            AccountTable.iban eq iban
        }.firstOrNull()?.toEntity()
    }

    fun saveAccount(input: AccountInput): Account? {
        val account = input.toAccount()
        return try {
            AccountTable.insert {
                it[id] = account.id
                it[holderId] = account.holderId
                it[iban] = account.iban
                it[refAccountIban] = account.refAccountIban
                it[type] = account.type
                it[balance] = account.balance
                it[isLocked] = account.isLocked
                it[createdAt] = DateTime(account.createdAt.toEpochMilli())
            }.resultedValues?.firstOrNull()?.toEntity()
        } catch (e: ExposedSQLException) {
            throw DatabaseException(
                code = DatabaseErrorCode.ALREADY_EXISTS,
                message = "user account cannot be saved for id=${account.id}"
            )
        }
    }

    fun updateBalance(iban: String, newBalance: BigDecimal) = try {
        AccountTable.update({ AccountTable.iban eq iban }) {
            it[balance] = newBalance
        }
    } catch (e: ExposedSQLException) {
        throw DatabaseException(
            code = DatabaseErrorCode.UPDATE_FAILED,
            message = "account with iban=$iban cannot be updated"
        )
    }

    private fun ResultRow.toEntity(): Account {
        return Account(
            id = this[AccountTable.id],
            holderId = this[AccountTable.holderId],
            iban = this[AccountTable.iban],
            refAccountIban = this[AccountTable.refAccountIban],
            type = this[AccountTable.type],
            balance = this[AccountTable.balance],
            isLocked = this[AccountTable.isLocked],
            createdAt = Instant.ofEpochMilli(this[AccountTable.createdAt].millis),
        )
    }
}
