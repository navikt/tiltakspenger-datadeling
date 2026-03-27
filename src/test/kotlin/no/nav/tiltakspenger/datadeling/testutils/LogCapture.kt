package no.nav.tiltakspenger.datadeling.testutils

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

class LogCapture private constructor(
    private val logger: Logger,
    private val appender: ListAppender<ILoggingEvent>,
) : AutoCloseable {
    val messages: List<String>
        get() = appender.list.map { event ->
            buildString {
                append(event.formattedMessage)
                event.throwableProxy?.message?.let {
                    append(" ")
                    append(it)
                }
            }
        }

    fun combined(): String = messages.joinToString(separator = "\n")

    override fun close() {
        logger.detachAppender(appender)
        appender.stop()
    }

    companion object {
        fun attach(loggerName: String): LogCapture {
            val logger = LoggerFactory.getLogger(loggerName) as Logger
            val appender = ListAppender<ILoggingEvent>().apply { start() }
            logger.addAppender(appender)
            return LogCapture(logger, appender)
        }

        fun attach(clazz: Class<*>): LogCapture = attach(clazz.name)
    }
}
