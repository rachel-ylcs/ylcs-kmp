package love.yinlin.extension

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object DateEx {
	object Formatter {
		val weiboDateTime = DateTimeComponents.Format {
			dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
			char(' ')
			monthName(MonthNames.ENGLISH_ABBREVIATED)
			char(' ')
			dayOfMonth()
			char(' ')
			hour()
			char(':')
			minute()
			char(':')
			second()
			char(' ')
			offset(UtcOffset.Formats.FOUR_DIGITS)
			char(' ')
			year()
		}

		val standardDateTime = LocalDateTime.Format {
			year()
			char('-')
			monthNumber(padding = Padding.ZERO)
			char('-')
			dayOfMonth(padding = Padding.ZERO)
			char(' ')
			hour()
			char(':')
			minute()
			char(':')
			second()
		}

		val standardDate = LocalDate.Format {
			year()
			char('-')
			monthNumber(padding = Padding.ZERO)
			char('-')
			dayOfMonth(padding = Padding.ZERO)
		}
	}

	val Today: LocalDate get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

	val TodayString: String = Today.format(Formatter.standardDate)
}

val Long.toLocalDateTime: LocalDateTime? get() = try {
	Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
}
catch (_: Throwable) { null }
val Long.toLocalDate: LocalDate? get() = this.toLocalDateTime?.date
val LocalDateTime.toLong: Long get() = this.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
val LocalDate.toLong: Long get() = this.toLocalDateTime.toLong
val LocalDate.toLocalDateTime: LocalDateTime get() = LocalDateTime(this, LocalTime(8, 0, 0))