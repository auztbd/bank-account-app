package org.example.banca.error

class EntityNotFoundException(
    message: String,
    cause: Exception? = null
) : RuntimeException(message, cause)

class DatabaseException(
    val code: DatabaseErrorCode,
    override val message: String
) : Throwable()

enum class DatabaseErrorCode {
    ALREADY_EXISTS,
    UPDATE_FAILED
}

class InvalidInputException(
    val code: ErrorCode,
    override val message: String
) : Throwable()

enum class ErrorCode {
    TRANSFER_TO_SELF,
    NON_POSITIVE_AMOUNT,
    WRONG_BIC,
    MISSING_REFERENCE_ACCOUNT,
    WITHDRAW_FROM_PRIVATE_LOAN_ACCOUNT,
    WITHDRAW_TO_NON_REFERENCE_ACCOUNT
}
