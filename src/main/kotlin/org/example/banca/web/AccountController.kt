package org.example.banca.web

import org.example.banca.account.Account
import org.example.banca.account.AccountFilter
import org.example.banca.core.BankAccountService
import org.example.banca.transaction.Transaction
import org.example.banca.transaction.TransactionSummary
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping(path = ["/api/accounts"], produces = ["application/json"])
class AccountController(
    private val service: BankAccountService
) {
    @PostMapping("/create-new")
    fun createAccount(@RequestBody acc: AccountInput): ResponseEntity<Account> {
        return service.saveNewAccount(acc)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.internalServerError().build()
    }

    @GetMapping("/{iban}/balance")
    fun getAccountBalance(@PathVariable iban: String): ResponseEntity<BigDecimal> {
        return service.getAccountBalance(iban)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/{iban}/transaction-history")
    fun getTransactionHistory(@PathVariable iban: String): ResponseEntity<List<TransactionSummary>> {
        return service.getTransactionHistory(iban).let { ResponseEntity.ok(it) }
    }

    @PostMapping("/find-by-filter")
    fun getAccountsByFilter(@RequestBody filter: AccountFilter): ResponseEntity<List<Account>> {
        return service.getAccountsByFilter(filter).let { ResponseEntity.ok(it) }
    }

    @PostMapping("/{iban}/transfer")
    fun makeBankTransfer(
        @PathVariable iban: String,
        @RequestBody transferInput: TransferInput
    ): ResponseEntity<Transaction> {
        return service.createBankTransfer(iban, transferInput)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
