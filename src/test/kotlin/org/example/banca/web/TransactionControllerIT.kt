package org.example.banca.web

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.example.banca.CHECKING_IBAN
import org.example.banca.DEPOSIT_IBAN
import org.example.banca.account.AccountRepository
import org.example.banca.messaging.MessagePublisher
import org.example.banca.messaging.TransactionMessageListener
import org.example.banca.bankTransfer
import org.example.banca.transaction.TransactionRepository
import org.example.banca.checkingAccount
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class TransactionControllerIT : AbstractIntegrationTest() {

    @MockkBean
    private lateinit var accountRepository: AccountRepository

    @MockkBean
    private lateinit var transactionRepository: TransactionRepository

    @MockkBean
    private lateinit var messagePublisher: MessagePublisher

    @MockkBean
    private lateinit var messageListener: TransactionMessageListener

    private val baseUrl = "/api"

    @Test
    fun `makeDeposit - happy path - transaction created`() {
        every { accountRepository.findByIban(CHECKING_IBAN) } returns checkingAccount()
        every { transactionRepository.saveTransaction(any()) } answers { firstArg() }
        every { messagePublisher.publishTransactionMessage(any()) } returns Unit

        given()
            .contentType(ContentType.JSON)
            .body(DepositInput(amount = BigDecimal.valueOf(120.55), destinationIban = CHECKING_IBAN))
            .post("$baseUrl/deposit")
            .then()
            .statusCode(200)
            .body("originIban", equalTo(DEPOSIT_IBAN))
            .body("destinationIban", equalTo(CHECKING_IBAN))
            .body("isComplete", equalTo(false))
            .body("reason", equalTo("deposit"))
            .body("amount", equalTo(120.55f))
    }

    @Test
    fun `makeDeposit - account not found for iban - http 404 returned`() {
        every { accountRepository.findByIban(any()) } returns null
        every { transactionRepository.saveTransaction(any()) } answers { firstArg() }
        every { messagePublisher.publishTransactionMessage(any()) } returns Unit

        given()
            .contentType(ContentType.JSON)
            .body(DepositInput(amount = BigDecimal.valueOf(120.55), destinationIban = CHECKING_IBAN))
            .post("$baseUrl/deposit")
            .then()
            .statusCode(404)
            .body("message", equalTo("No account found in db for iban $CHECKING_IBAN"))
    }

    @Test
    fun `getTransaction - happy path - transaction returned`() {
        val trxIdString = "c17c809c-dcee-4663-904e-c7ade941c34d"
        val trxId = UUID.fromString(trxIdString)
        every { transactionRepository.findById(trxId) } returns bankTransfer(id = trxId)

        given()
            .contentType(ContentType.JSON)
            .get("$baseUrl/transactions/$trxIdString")
            .then()
            .statusCode(200)
            .body("id", equalTo(trxIdString))
    }
}
