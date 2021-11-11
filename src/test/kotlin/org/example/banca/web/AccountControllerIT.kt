package org.example.banca.web

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.example.banca.ACCOUNT_HOLDER_ID
import org.example.banca.CHECKING_IBAN
import org.example.banca.SAVINGS_IBAN
import org.example.banca.account.AccountRepository
import org.example.banca.account.AccountType
import org.example.banca.messaging.MessagePublisher
import org.example.banca.messaging.TransactionMessageListener
import org.example.banca.transaction.TransactionRepository
import org.example.banca.checkingAccount
import org.example.banca.savingsAccount
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccountControllerIT : AbstractIntegrationTest() {

    @MockkBean
    private lateinit var accountRepository: AccountRepository

    @MockkBean
    private lateinit var transactionRepository: TransactionRepository

    @MockkBean
    private lateinit var messagePublisher: MessagePublisher

    @MockkBean
    private lateinit var messageListener: TransactionMessageListener

    private val baseUrl = "/api/accounts"

    @Test
    fun `createAccount - happy path - account created`() {
        every { accountRepository.saveAccount(any()) } returns checkingAccount()

        given()
            .contentType(ContentType.JSON)
            .body(AccountInput(holderId = ACCOUNT_HOLDER_ID, type = AccountType.CHECKING))
            .post("$baseUrl/create-new")
            .then()
            .statusCode(200)
            .body("holderId", equalTo(ACCOUNT_HOLDER_ID.toString()))
            .body("iban", equalTo(CHECKING_IBAN))
            .body("balance", equalTo(1750.83f))
            .body("type", equalTo("checking"))
    }

    @Test
    fun `makeBankTransfer - transaction created`() {
        every { accountRepository.findByIban(SAVINGS_IBAN) } returns savingsAccount()
        every { accountRepository.findByIban(CHECKING_IBAN) } returns checkingAccount()
        every { transactionRepository.saveTransaction(any()) } answers { firstArg() }
        every { messagePublisher.publishTransactionMessage(any()) } returns Unit

        val transferInput = TransferInput(
            amount = BigDecimal.valueOf(230.75),
            destinationIban = CHECKING_IBAN,
            reason = "there is no spoon",
            bic = null
        )

        given()
            .contentType(ContentType.JSON)
            .body(transferInput)
            .post("$baseUrl/$SAVINGS_IBAN/transfer")
            .then()
            .statusCode(200)
            .body("originIban", equalTo(SAVINGS_IBAN))
            .body("destinationIban", equalTo(CHECKING_IBAN))
            .body("isComplete", equalTo(false))
            .body("amount", equalTo(230.75f))
            .body("reason", equalTo("there is no spoon"))
    }
}
