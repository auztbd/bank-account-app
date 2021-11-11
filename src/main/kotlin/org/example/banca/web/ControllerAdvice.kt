package org.example.banca.web

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.banca.error.DatabaseException
import org.example.banca.error.EntityNotFoundException
import org.example.banca.error.InvalidInputException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorMessage(val message: String)

@RestControllerAdvice(basePackageClasses = [AccountController::class, TransactionController::class])
class ControllerAdvice(
    private val om: ObjectMapper
) {
    @ExceptionHandler(DatabaseException::class)
    fun handleDatabaseException(e: DatabaseException): ResponseEntity<String> {
        return ResponseEntity.internalServerError().body(om.writeValueAsString(ErrorMessage(e.message)))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(om.writeValueAsString(e.message?.let { ErrorMessage(it) }))
    }

    @ExceptionHandler(InvalidInputException::class)
    fun handleInvalidInputException(e: InvalidInputException): ResponseEntity<String> {
        return ResponseEntity.badRequest().body(om.writeValueAsString(ErrorMessage(e.message)))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity.internalServerError().body(om.writeValueAsString(e.message?.let { ErrorMessage(it) }))
    }
}
