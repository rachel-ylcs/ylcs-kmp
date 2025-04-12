package love.yinlin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggerBase

val logger: Logger = LoggerFactory.getLogger(LoggerBase::class.java)

class WorkFilter : Filter<ILoggingEvent>() {
	override fun decide(event: ILoggingEvent) =
		if (event.loggerName == LoggerBase::class.java.name
			&& event.level.isGreaterOrEqual(Level.INFO))
			FilterReply.ACCEPT else FilterReply.DENY
}

class RequestFilter : Filter<ILoggingEvent>() {
	override fun decide(event: ILoggingEvent) =
		if (event.loggerName == LoggerBase::class.java.name
			&& event.level == Level.DEBUG)
			FilterReply.ACCEPT else FilterReply.DENY
}