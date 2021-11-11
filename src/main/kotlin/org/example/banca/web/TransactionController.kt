package org.example.banca.web

import org.example.banca.core.BankAccountService
import org.example.banca.transaction.Transaction
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api"], produces = ["application/json"])
class TransactionController(
    private val service: BankAccountService
) {
    @GetMapping("/transactions/{id}")
    fun getTransaction(
        @PathVariable id: String,
    ): ResponseEntity<Transaction> {
        return service.getTransactionById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping("/deposit")
    fun makeDeposit(
        @RequestBody depositInput: DepositInput
    ): ResponseEntity<Transaction> {
        return service.createDeposit(depositInput)
            .let { ResponseEntity.ok(it) }
    }
}
