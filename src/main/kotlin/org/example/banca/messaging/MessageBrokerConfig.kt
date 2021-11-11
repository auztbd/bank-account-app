package org.example.banca.messaging

import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MessageBrokerConfig(
    @Value("\${messaging.message-ttl}") private val messageTTL: Int,
) {
    @Bean
    fun queue(): Queue = QueueBuilder.durable(QUEUE).ttl(messageTTL).build()

    @Bean
    fun exchange(): TopicExchange = TopicExchange(EXCHANGE)

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY)

    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun template(connectionFactory: ConnectionFactory): AmqpTemplate =
        RabbitTemplate(connectionFactory).apply {
            messageConverter = messageConverter()
        }

    companion object {
        const val QUEUE = "msg-queue"
        const val EXCHANGE = "msg-exchange"
        const val ROUTING_KEY = "msg-routingKey"
    }
}
