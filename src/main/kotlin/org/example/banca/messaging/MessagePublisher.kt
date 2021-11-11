package org.example.banca.messaging

import mu.KLogging
import org.example.banca.messaging.MessageBrokerConfig.Companion.EXCHANGE
import org.example.banca.messaging.MessageBrokerConfig.Companion.ROUTING_KEY
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class MessagePublisher(
    private val rabbitTemplate: RabbitTemplate
) {
    fun publishTransactionMessage(payload: TransactionMessage) {
        logger.info { "Publishing $payload to queue with routing key $ROUTING_KEY" }
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload)
    }

    companion object : KLogging()
}
