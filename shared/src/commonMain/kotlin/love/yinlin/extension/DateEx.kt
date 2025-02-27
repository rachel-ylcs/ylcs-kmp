package love.yinlin.extension

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.toLocalDateTime

object DateEx {
	val currentDateInt: Int
	val currentDateString: String

	init {
		val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
		currentDateString = date.format(LocalDate.Format {
			year()
			monthNumber(padding = Padding.ZERO)
			dayOfMonth(padding = Padding.ZERO)
		})
		currentDateInt = currentDateString.toInt()
	}
}