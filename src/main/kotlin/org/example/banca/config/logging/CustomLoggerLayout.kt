package org.example.banca.config.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.contrib.json.classic.JsonLayout
import java.util.LinkedHashMap

class CustomLoggerLayout : JsonLayout() {

    private val timestampAttrName = "application-time"

    override fun toJsonMap(event: ILoggingEvent): Map<*, *> {
        val map = LinkedHashMap<String, Any>()
        addTimestamp(timestampAttrName, this.includeTimestamp, event.timeStamp, map)
        add(LEVEL_ATTR_NAME, this.includeLevel, event.level.toString(), map)
        add(THREAD_ATTR_NAME, this.includeThreadName, event.threadName, map)
        add(LOGGER_ATTR_NAME, this.includeLoggerName, event.loggerName, map)
        add(FORMATTED_MESSAGE_ATTR_NAME, this.includeFormattedMessage, event.formattedMessage.doubleEscape(), map)
        add(MESSAGE_ATTR_NAME, this.includeMessage, event.message, map)
        add(CONTEXT_ATTR_NAME, this.includeContextName, event.loggerContextVO.name, map)
        if (event.throwableProxy != null)
            addThrowableInfo(EXCEPTION_ATTR_NAME, this.includeException, event, map)
        return map
    }

    override fun addThrowableInfo(
        fieldName: String,
        field: Boolean,
        value: ILoggingEvent?,
        map: MutableMap<String, Any>
    ) {
        if (field) {
            value?.throwableProxy?.let {
                throwableProxyConverter.convert(value)?.let { message ->
                    if (message.isNotBlank()) {
                        map[fieldName] = message.doubleEscape()
                    }
                }
            }
        }
    }

    private fun String.doubleEscape() = this.replace("\"", "\\\"")
}
