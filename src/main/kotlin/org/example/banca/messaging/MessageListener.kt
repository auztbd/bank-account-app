package org.example.banca.messaging

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KLogging
import org.example.banca.core.BankAccountService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TransactionMessageListener(
    private val service: BankAccountService
) {
    @RabbitListener(queues = [MessageBrokerConfig.QUEUE])
    fun listener(message: TransactionMessage) {
        logger.info { "Received TransactionMessage with id ${message.transactionId}" }
        service.processTransaction(message.transactionId)
    }

    companion object : KLogging()
}

data class TransactionMessage(
    @JsonProperty("transactionId")
    val transactionId: UUID
)
