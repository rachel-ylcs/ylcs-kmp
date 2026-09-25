package love.yinlin.tpl.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import love.yinlin.extension.DateEx

@Stable
internal object WeiboDate {
    private val weiboDateTime = DateEx.Formatter(DateTimeComponents.Format {
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

    fun convert(time: String): LocalDateTime = weiboDateTime.parse(time)!!.toLocalDateTime()
}