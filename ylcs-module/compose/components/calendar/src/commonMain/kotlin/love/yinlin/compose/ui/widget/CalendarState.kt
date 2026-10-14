package love.yinlin.compose.ui.widget

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import love.yinlin.extension.DateEx

@Stable
class CalendarState : PagerState(currentPage = 5) {
    override val pageCount: Int = 12

    companion object {
        internal fun indexShadowDate(index: Int): LocalDate {
            val today = DateEx.Today
            val value = index - 5 + today.month.number
            return if (value <= 0) LocalDate(year = today.year - 1, month = value + 12, day = 1)
            else if (value > 12) LocalDate(year = today.year + 1, month = value - 12, day = 1)
            else LocalDate(year = today.year, month = value, day = 1)
        }
    }
}