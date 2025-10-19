package love.yinlin.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

class RequestFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent) =
        if (event.loggerName == LoggerBase::class.java.name
            && event.level == Level.DEBUG)
            FilterReply.ACCEPT else FilterReply.DENY
}