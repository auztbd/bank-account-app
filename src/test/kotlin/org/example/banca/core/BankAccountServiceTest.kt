package org.example.banca.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.example.banca.CHECKING_IBAN
import org.example.banca.EXTERNAL_IBAN
import org.example.banca.PRIVATE_LOAN_IBAN
import org.example.banca.SAVINGS_IBAN
import org.example.banca.account.AccountRepository
import org.example.banca.account.AccountType
import org.example.banca.checkingAccount
import org.example.banca.deposit
import org.example.banca.messaging.MessagePublisher
import org.example.banca.bankTransfer
import org.example.banca.error.EntityNotFoundException
import org.example.banca.error.ErrorCode
import org.example.banca.error.InvalidInputException
import org.example.banca.messaging.TransactionMessage
import org.example.banca.privateLoanAccount
import org.example.banca.savingsAccount
import org.example.banca.transaction.TransactionRepository
import org.example.banca.transaction.TransactionType
import org.example.banca.web.AccountInput
import org.example.banca.web.DEPOSIT_ORIGIN_IBAN
import org.example.banca.web.DepositInput
import org.example.banca.web.TransferInput
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class BankAccountServiceTest {
    private val accountRepo = mockk<AccountRepository>()
    private val transactionRepo = mockk<TransactionRepository>()
    private val messagePublisher = mockk<MessagePublisher>()
    private val service = BankAccountService(accountRepo, transactionRepo, messagePublisher)

    @Test
    fun `get account balance`() {
        every { accountRepo.findByIban(CHECKING_IBAN) } returns checkingAccount()
        assertThat(service.getAccountBalance(CHECKING_IBAN)).isEqualByComparingTo("1750.83")
    }

    @Test
    fun `get transaction by id`() {
        val trxId = UUID.fromString("d999da21-6ccc-4aa4-bd8b-839f392e02a3")
        every { transactionRepo.findById(trxId) } returns bankTransfer(id = trxId)

        val result = service.getTransactionById(trxId.toString())
        assertSoftly {
            with(result) {
                assertThat(this!!.id).isEqualTo(trxId)
                assertThat(originIban).isEqualTo(CHECKING_IBAN)
                assertThat(destinationIban).isEqualTo(SAVINGS_IBAN)
                assertThat(amount).isEqualByComparingTo("120.550")
                assertThat(reason).isEqualTo("becoz am rich")
            }
        }
    }

    @Test
    fun `get transaction history for iban`() {
        every { transactionRepo.findByIban(CHECKING_IBAN) } returns
                listOf(deposit(), bankTransfer()).sortedByDescending { it.createdAt }

        val result = service.getTransactionHistory(CHECKING_IBAN)
        assertSoftly {
            assertThat(result.size).isEqualTo(2)
            with(result[0]) {
                assertThat(amount).isEqualByComparingTo("120.55")
                assertThat(type).isEqualTo(TransactionType.OUTGOING)
                assertThat(iban).isEqualTo(SAVINGS_IBAN)
                assertThat(reason).isEqualTo("becoz am rich")
            }
            with(result[1]) {
                assertThat(amount).isEqualByComparingTo("1250.00")
                assertThat(type).isEqualTo(TransactionType.INCOMING)
                assertThat(iban).isEqualTo(DEPOSIT_ORIGIN_IBAN)
                assertThat(reason).isEqualTo("deposit")
            }
        }
    }

    @Test
    fun `createBankTransfer - happy path`() {
        val input = TransferInput(
            amount = BigDecimal.valueOf(151.25),
            destinationIban = CHECKING_IBAN,
            bic = null,
            reason = "from savings to reference"
        )
        every { accountRepo.findByIban(SAVINGS_IBAN) } returns savingsAccount()
        every { transactionRepo.saveTransaction(any()) } answers { firstArg() }
        every { messagePublisher.publishTransactionMessage(any()) } returns Unit

        val transaction = service.createBankTransfer(SAVINGS_IBAN, input)
        assertSoftly {
            assertThat(transaction).isNotNull
            with(transaction!!) {
                assertThat(amount).isEqualByComparingTo(input.amount)
                assertThat(originIban).isEqualTo(SAVINGS_IBAN)
                assertThat(destinationIban).isEqualTo(CHECKING_IBAN)
                assertThat(reason).isEqualTo("from savings to reference")
            }
        }

        val msg = TransactionMessage(transaction!!.id)
        verify(exactly = 1) { messagePublisher.publishTransactionMessage(msg) }
    }

    @Test
    fun `createDeposit - happy path`() {
        val input = DepositInput(
            amount = BigDecimal.valueOf(71.52),
            destinationIban = CHECKING_IBAN
        )
        every { accountRepo.findByIban(CHECKING_IBAN) } returns checkingAccount()
        every { transactionRepo.saveTransaction(any()) } answers { firstArg() }
        every { messagePublisher.publishTransactionMessage(any()) } returns Unit

        val transaction = service.createDeposit(input)
        assertSoftly {
            assertThat(transaction).isNotNull
            with(transaction!!) {
                assertThat(amount).isEqualByComparingTo(input.amount)
                assertThat(originIban).isEqualTo(DEPOSIT_ORIGIN_IBAN)
                assertThat(destinationIban).isEqualTo(CHECKING_IBAN)
                assertThat(reason).isEqualTo("deposit")
            }
        }

        val msg = TransactionMessage(transaction!!.id)
        verify(exactly = 1) { messagePublisher.publishTransactionMessage(msg) }
    }

    @Nested
    inner class InputValidityChecks {

        @Test
        fun `validateAccountInput - reference account must be provided for savings account`() {
            val input = AccountInput(type = AccountType.SAVINGS, refAccountIban = null)
            assertThrows<InvalidInputException> {
                service.saveNewAccount(input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.MISSING_REFERENCE_ACCOUNT)
                assertThat(it.message).isEqualTo("A reference account must be provided for savings account")
            }
        }

        @Test
        fun `validateDepositInput - amount must be positive`() {
            val input = DepositInput(amount = BigDecimal.ZERO, destinationIban = CHECKING_IBAN)
            assertThrows<InvalidInputException> {
                service.validateDepositInput(input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.NON_POSITIVE_AMOUNT)
                assertThat(it.message).isEqualTo("Transfer amount must be positive")
            }
        }

        @Test
        fun `validateDepositInput - sender and receiver IBANs must be different`() {
            val input = DepositInput(amount = BigDecimal.valueOf(100), destinationIban = DEPOSIT_ORIGIN_IBAN)
            assertThrows<InvalidInputException> {
                service.validateDepositInput(input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.TRANSFER_TO_SELF)
                assertThat(it.message).isEqualTo("Transfer to self is not allowed")
            }
        }

        @Test
        fun `validateDepositInput - destination IBAN must be correct`() {
            every { accountRepo.findByIban(CHECKING_IBAN) } returns checkingAccount()
            val input = DepositInput(
                amount = BigDecimal.valueOf(100),
                destinationIban = "S-P-Q-R"
            )
            assertThrows<IllegalArgumentException> {
                service.createDeposit(input)
            }.also {
                assertThat(it.message).isEqualTo("destination iban S-P-Q-R is invalid")
            }
        }

        @Test
        fun `validateDepositInput - internal IBAN, not found in db - exception thrown`() {
            every { accountRepo.findByIban(SAVINGS_IBAN) } returns null
            val input = DepositInput(amount = BigDecimal.valueOf(100), destinationIban = SAVINGS_IBAN)
            assertThrows<EntityNotFoundException> {
                service.validateDepositInput(input)
            }.also {
                assertThat(it.message).isEqualTo("No account found in db for iban ${input.destinationIban}")
            }
        }

        @Test
        fun `validateTransferInput - sender and receiver IBANs must be different`() {
            val input = TransferInput(
                amount = BigDecimal.valueOf(100),
                destinationIban = CHECKING_IBAN,
                bic = null,
                reason = null
            )
            assertThrows<InvalidInputException> {
                service.validateTransferInput(CHECKING_IBAN, input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.TRANSFER_TO_SELF)
                assertThat(it.message).isEqualTo("Transfer to self is not allowed")
            }
        }

        @Test
        fun `validateTransferInput - amount must be positive`() {
            val input = TransferInput(
                amount = BigDecimal.valueOf(-1.25),
                destinationIban = CHECKING_IBAN,
                bic = null,
                reason = null
            )
            assertThrows<InvalidInputException> {
                service.validateTransferInput(SAVINGS_IBAN, input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.NON_POSITIVE_AMOUNT)
                assertThat(it.message).isEqualTo("Transfer amount must be positive")
            }
        }

        @Test
        fun `validateTransferInput - BIC must be valid`() {
            val input = TransferInput(
                amount = BigDecimal.valueOf(111.25),
                destinationIban = EXTERNAL_IBAN,
                bic = "SELBIT2BXXX",
                reason = null
            )
            assertThrows<InvalidInputException> {
                service.validateTransferInput(CHECKING_IBAN, input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.WRONG_BIC)
                assertThat(it.message).isEqualTo("Invalid BIC for IBAN $CHECKING_IBAN")
            }
        }
    }

    @Nested
    inner class BankTransferValidityChecks {

        @Test
        fun `verifyTransferValidity - sender account must not be locked`() {
            every { accountRepo.findByIban(CHECKING_IBAN) } returns checkingAccount().copy(isLocked = true)
            val input = TransferInput(
                amount = BigDecimal.valueOf(100),
                destinationIban = SAVINGS_IBAN,
                bic = null,
                reason = null
            )
            assertThrows<IllegalArgumentException> {
                service.createBankTransfer(CHECKING_IBAN, input)
            }.also {
                assertThat(it.message).isEqualTo("account $CHECKING_IBAN is locked!")
            }
        }

        @Test
        fun `verifyTransferValidity - sender IBAN must be correct`() {
            every { accountRepo.findByIban(any()) } returns checkingAccount().copy(iban = "x-y-z")
            val input = TransferInput(
                amount = BigDecimal.valueOf(100),
                destinationIban = SAVINGS_IBAN,
                bic = null,
                reason = null
            )
            assertThrows<IllegalArgumentException> {
                service.createBankTransfer("x-y-z", input)
            }.also {
                assertThat(it.message).isEqualTo("source iban x-y-z is invalid")
            }
        }

        @Test
        fun `verifyTransferValidity - recipient IBAN must be correct`() {
            every { accountRepo.findByIban(CHECKING_IBAN) } returns checkingAccount()
            val input = TransferInput(
                amount = BigDecimal.valueOf(100),
                destinationIban = "1-2-3-4",
                bic = null,
                reason = null
            )
            assertThrows<IllegalArgumentException> {
                service.createBankTransfer(CHECKING_IBAN, input)
            }.also {
                assertThat(it.message).isEqualTo("destination iban 1-2-3-4 is invalid")
            }
        }

        @Test
        fun `verifyTransferValidity - cannot withdraw from private loan account`() {
            every { accountRepo.findByIban(PRIVATE_LOAN_IBAN) } returns privateLoanAccount()
            val input = TransferInput(
                amount = BigDecimal.valueOf(100),
                destinationIban = CHECKING_IBAN,
                bic = null,
                reason = null
            )
            assertThrows<InvalidInputException> {
                service.createBankTransfer(PRIVATE_LOAN_IBAN, input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.WITHDRAW_FROM_PRIVATE_LOAN_ACCOUNT)
                assertThat(it.message).isEqualTo("withdrawal not possible from private loan account")
            }
        }

        @Test
        fun `verifyTransferValidity - can only withdraw to reference account, from savings account`() {
            every { accountRepo.findByIban(SAVINGS_IBAN) } returns savingsAccount()
            val input = TransferInput(
                amount = BigDecimal.valueOf(100),
                destinationIban = EXTERNAL_IBAN,
                bic = null,
                reason = null
            )
            assertThrows<InvalidInputException> {
                service.createBankTransfer(SAVINGS_IBAN, input)
            }.also {
                assertThat(it.code).isEqualTo(ErrorCode.WITHDRAW_TO_NON_REFERENCE_ACCOUNT)
                assertThat(it.message).isEqualTo("from savings account, you can only transfer to the reference account")
            }
        }
    }
}
