@file:OptIn(ExperimentalTime::class)
package love.yinlin.extension

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.jvm.JvmInline
import kotlin.time.ExperimentalTime

object DateEx {
	@JvmInline
	value class Formatter<T> private constructor(private val factory: DateTimeFormat<T>) {
		fun parse(input: CharSequence): T? = factory.parseOrNull(input)
		fun format(value: T): String? = catchingNull { factory.format(value) }

		companion object {
			val weiboDateTime = Formatter(DateTimeComponents.Format {
				dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
				char(' ')
				monthName(MonthNames.ENGLISH_ABBREVIATED)
				char(' ')
				day()
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
			})

			val standardDateTime = Formatter(LocalDateTime.Format {
				year()
				char('-')
				monthNumber(padding = Padding.ZERO)
				char('-')
				day(padding = Padding.ZERO)
				char(' ')
				hour()
				char(':')
				minute()
				char(':')
				second()
			})

			val standardDate = Formatter(LocalDate.Format {
				year()
				char('-')
				monthNumber(padding = Padding.ZERO)
				char('-')
				day(padding = Padding.ZERO)
			})
		}
	}

    val Zero: Instant = Instant.fromEpochMilliseconds(0L)

	val Current: LocalDateTime get() = Clock.System.now().toLocalDateTime!!

    val CurrentInstant: Instant get() = Clock.System.now()

	val CurrentString: String get() = Formatter.standardDateTime.format(Current)!!

	val CurrentLong: Long get() = Clock.System.now().toEpochMilliseconds()

	val Today: LocalDate get() = Current.date

	val TodayString: String get() = Formatter.standardDate.format(Today)!!
}

val Instant.toLocalDateTime: LocalDateTime? get() = catchingNull { this.toLocalDateTime(TimeZone.currentSystemDefault()) }
val Long.toLocalDateTime: LocalDateTime? get() = Instant.fromEpochMilliseconds(this).toLocalDateTime
val Long.toLocalDate: LocalDate? get() = this.toLocalDateTime?.date
val Long.toLocalTime: LocalTime? get() = catchingNull { LocalTime.fromMillisecondOfDay(this.toInt()) }
val LocalDateTime.toLong: Long get() = this.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
val LocalDate.toLong: Long get() = this.toLocalDateTime.toLong
val LocalDate.toLocalDateTime: LocalDateTime get() = LocalDateTime(this, LocalTime(8, 0, 0))