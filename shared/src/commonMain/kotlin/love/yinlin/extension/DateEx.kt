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
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.jvm.JvmInline

object DateEx {
	@JvmInline
	value class Formatter<T> private constructor(private val factory: DateTimeFormat<T>) {
		fun parse(input: CharSequence): T? = factory.parseOrNull(input)
		fun format(value: T): String? = try { factory.format(value) } catch (_: Throwable) { null }

		companion object {
			val weiboDateTime = Formatter(DateTimeComponents.Format {
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
			})

			val standardDateTime = Formatter(LocalDateTime.Format {
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
			})

			val standardDate = Formatter(LocalDate.Format {
				year()
				char('-')
				monthNumber(padding = Padding.ZERO)
				char('-')
				dayOfMonth(padding = Padding.ZERO)
			})
		}
	}

	val Current: LocalDateTime get() = Clock.System.now().toLocalDateTime!!

	val CurrentString: String = Formatter.standardDateTime.format(Current)!!

	val Today: LocalDate get() = Current.date

	val TodayString: String = Formatter.standardDate.format(Today)!!
}

val Instant.toLocalDateTime: LocalDateTime? get() = try { this.toLocalDateTime(TimeZone.currentSystemDefault()) } catch (_: Throwable) { null }
val Long.toLocalDateTime: LocalDateTime? get() = Instant.fromEpochMilliseconds(this).toLocalDateTime
val Long.toLocalDate: LocalDate? get() = this.toLocalDateTime?.date
val LocalDateTime.toLong: Long get() = this.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
val LocalDate.toLong: Long get() = this.toLocalDateTime.toLong
val LocalDate.toLocalDateTime: LocalDateTime get() = LocalDateTime(this, LocalTime(8, 0, 0))