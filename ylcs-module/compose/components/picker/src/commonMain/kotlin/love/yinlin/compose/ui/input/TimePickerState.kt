package love.yinlin.compose.ui.input

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.structuralEqualityPolicy
import kotlinx.datetime.LocalTime
import love.yinlin.extension.DateEx

@Stable
class TimePickerState(initialTime: LocalTime = DateEx.Current.time) {
    internal companion object {
        private const val CENTER_PAGE = 1_000_000_080
        internal const val HOURS = 24
        internal const val MINUTES = 60
        internal const val SECONDS = 60

        fun wheelValue(page: Int, range: Int): Int = (page - CENTER_PAGE).mod(range)
    }

    internal val hours = PagerState(currentPage = CENTER_PAGE + initialTime.hour) { Int.MAX_VALUE }
    internal val minutes = PagerState(currentPage = CENTER_PAGE + initialTime.minute) { Int.MAX_VALUE }
    internal val seconds = PagerState(currentPage = CENTER_PAGE + initialTime.second) { Int.MAX_VALUE }

    val time: LocalTime by derivedStateOf(structuralEqualityPolicy()) {
        LocalTime(
            hour = wheelValue(hours.currentPage, HOURS),
            minute = wheelValue(minutes.currentPage, MINUTES),
            second = wheelValue(seconds.currentPage, SECONDS)
        )
    }
}
