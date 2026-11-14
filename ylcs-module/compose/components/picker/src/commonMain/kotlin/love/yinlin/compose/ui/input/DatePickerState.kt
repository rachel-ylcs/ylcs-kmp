package love.yinlin.compose.ui.input

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import love.yinlin.compose.extension.mutableEnumStateOf
import love.yinlin.extension.DateEx

@Stable
class DatePickerState(initialDate: LocalDate? = null) {
    internal companion object {
        const val CENTER_PAGE = Int.MAX_VALUE / 2
    }

    private val monthOrigin = (initialDate ?: DateEx.Today).yearMonth

    internal val monthPager = PagerState(currentPage = CENTER_PAGE) { Int.MAX_VALUE }

    var date: LocalDate? by mutableStateOf(initialDate)
        internal set

    internal var view by mutableEnumStateOf(DatePickerView.Days)

    internal fun monthAt(page: Int): YearMonth = monthOrigin.plus(page - CENTER_PAGE, DateTimeUnit.MONTH)

    internal fun showMonth(year: Int, monthNumber: Int) {
        val page = CENTER_PAGE.toLong() + (year.toLong() - monthOrigin.year) * 12 + monthNumber - monthOrigin.month.number
        if (page in 0L ..< monthPager.pageCount.toLong() && page.toInt() != monthPager.currentPage) {
            monthPager.requestScrollToPage(page.toInt())
        }
    }

    internal fun select(yearMonth: YearMonth) {
        showMonth(yearMonth.year, yearMonth.month.number)
        view = DatePickerView.Days
    }

    internal fun select(date: LocalDate) {
        showMonth(date.year, date.month.number)
        this.date = date
    }
}
