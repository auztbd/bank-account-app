package org.example.banca.core

import mu.KLogging
import org.example.banca.account.Account
import org.example.banca.account.AccountFilter
import org.example.banca.account.AccountRepository
import org.example.banca.account.AccountType
import org.example.banca.error.EntityNotFoundException
import org.example.banca.error.ErrorCode
import org.example.banca.error.InvalidInputException
import org.example.banca.web.AccountInput
import org.example.banca.web.DepositInput
import org.example.banca.web.TransferInput
import org.example.banca.messaging.MessagePublisher
import org.example.banca.messaging.TransactionMessage
import org.example.banca.transaction.Transaction
import org.example.banca.transaction.TransactionRepository
import org.example.banca.transaction.TransactionSummary
import org.example.banca.util.isInternalIban
import org.example.banca.util.isValidBic
import org.example.banca.util.isValidIban
import org.example.banca.web.DEPOSIT_ORIGIN_IBAN
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import java.math.BigDecimal
import java.util.UUID

@Service
class BankAccountService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val messagePublisher: MessagePublisher
) {
    fun getAccountBalance(iban: String): BigDecimal? {
        return accountRepository.findByIban(iban)?.balance
    }

    fun getAccountsByFilter(filter: AccountFilter): List<Account> {
        return accountRepository.find(filter)
    }

    fun saveNewAccount(accountInput: AccountInput): Account? {
        validateAccountInput(accountInput)
        return accountRepository.saveAccount(accountInput)
    }

    fun getTransactionHistory(iban: String): List<TransactionSummary> {
        return transactionRepository.findByIban(iban).map { it.summarize(iban) }
    }

    fun getTransactionById(id: String): Transaction? {
        return transactionRepository.findById(UUID.fromString(id))
    }

    fun createBankTransfer(iban: String, transferInput: TransferInput): Transaction? {
        validateTransferInput(iban, transferInput)
        val account = accountRepository.findByIban(iban)
            ?: throw EntityNotFoundException("No customer account found in db for iban $iban")

        verifyTransferValidity(account, transferInput)

        val transaction = Transaction(
            amount = transferInput.amount,
            originIban = account.iban,
            destinationIban = transferInput.destinationIban,
            reason = transferInput.reason
        )

        return transactionRepository.saveTransaction(transaction)?.let {
            messagePublisher.publishTransactionMessage(TransactionMessage(transactionId = transaction.id))
            it
        }
    }

    fun createDeposit(depositInput: DepositInput): Transaction? {
        validateDepositInput(depositInput)

        val transaction = Transaction(
            amount = depositInput.amount,
            originIban = depositInput.originIban,
            destinationIban = depositInput.destinationIban,
            reason = "deposit"
        )

        return transactionRepository.saveTransaction(transaction)?.let {
            messagePublisher.publishTransactionMessage(TransactionMessage(transactionId = transaction.id))
            it
        }
    }

    @Transactional(rollbackFor = [EntityNotFoundException::class])
    fun processTransaction(transactionId: UUID) {
        logger.info { "Trying to process transaction $transactionId" }
        val transaction = transactionRepository.findById(transactionId)
        if (transaction == null) {
            logger.warn { "Transaction with id $transactionId not found in db" }
            return
        }

        try {
            updateAccountBalances(transaction)
        } catch (e: EntityNotFoundException) {
            logger.warn { "Error occurred during processing. Changes will be rolled back. Exception: $e " }
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            return
        }

        transactionRepository.markTransactionAsComplete(id = transactionId)
        logger.info { "Transaction $transactionId successfully processed" }
    }

    fun updateAccountBalances(transaction: Transaction) {
        synchronized(accountRepository) {
            val originIban = transaction.originIban
            if (originIban != DEPOSIT_ORIGIN_IBAN && isInternalIban(originIban)) {
                val senderAccount = accountRepository.findByIban(originIban)
                if (senderAccount == null) {
                    logger.warn { "Account for iban $originIban not found in db. Transaction with ${transaction.id} will not be processed" }
                    throw EntityNotFoundException("Account for iban $originIban not found in db")
                }
                val newBalanceSender = senderAccount.balance - transaction.amount
                accountRepository.updateBalance(originIban, newBalanceSender)
            }

            val destinationIban = transaction.destinationIban
            if (destinationIban != DEPOSIT_ORIGIN_IBAN && isInternalIban(destinationIban)) {
                val receiverAccount = accountRepository.findByIban(destinationIban)
                if (receiverAccount == null) {
                    logger.warn { "Account for iban $destinationIban not found in db. Transaction with ${transaction.id} will not be processed" }
                    throw EntityNotFoundException("Account for iban $destinationIban not found in db")
                }
                val newBalance = receiverAccount.balance + transaction.amount
                accountRepository.updateBalance(destinationIban, newBalance)
            }
        }
    }

    internal fun validateAccountInput(input: AccountInput) {
        if (input.type == AccountType.SAVINGS && input.refAccountIban == null) {
            throw InvalidInputException(
                code = ErrorCode.MISSING_REFERENCE_ACCOUNT,
                message = "A reference account must be provided for savings account"
            )
        }
    }

    internal fun validateTransferInput(iban: String, input: TransferInput) {
        if (iban == input.destinationIban) {
            throw InvalidInputException(
                code = ErrorCode.TRANSFER_TO_SELF,
                message = "Transfer to self is not allowed"
            )
        }

        if (input.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidInputException(
                code = ErrorCode.NON_POSITIVE_AMOUNT,
                message = "Transfer amount must be positive"
            )
        }

        input.bic?.let {
            if (!isValidBic(it, iban)) {
                throw InvalidInputException(
                    code = ErrorCode.WRONG_BIC,
                    message = "Invalid BIC for IBAN $iban"
                )
            }
        }
    }

    internal fun verifyTransferValidity(account: Account, input: TransferInput) {
        require(!account.isLocked) { "account ${account.iban} is locked!" }
        require(isValidIban(input.destinationIban)) { "destination iban ${input.destinationIban} is invalid" }
        require(isValidIban(account.iban)) { "source iban ${account.iban} is invalid" }

        if (account.type == AccountType.SAVINGS && input.destinationIban != account.refAccountIban) {
            throw InvalidInputException(
                code = ErrorCode.WITHDRAW_TO_NON_REFERENCE_ACCOUNT,
                message = "from savings account, you can only transfer to the reference account"
            )
        }
        if (account.type == AccountType.PRIVATE_LOAN) {
            throw InvalidInputException(
                code = ErrorCode.WITHDRAW_FROM_PRIVATE_LOAN_ACCOUNT,
                message = "withdrawal not possible from private loan account"
            )
        }
    }

    internal fun validateDepositInput(input: DepositInput) {
        if (input.originIban == input.destinationIban) {
            throw InvalidInputException(
                code = ErrorCode.TRANSFER_TO_SELF,
                message = "Transfer to self is not allowed"
            )
        }

        if (input.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidInputException(
                code = ErrorCode.NON_POSITIVE_AMOUNT,
                message = "Transfer amount must be positive"
            )
        }

        require(isValidIban(input.destinationIban)) { "destination iban ${input.destinationIban} is invalid" }

        if (isInternalIban(input.destinationIban)) {
            accountRepository.findByIban(input.destinationIban)
                ?: throw EntityNotFoundException("No account found in db for iban ${input.destinationIban}")
        }
    }

    companion object : KLogging()
}
