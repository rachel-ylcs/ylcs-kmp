package love.yinlin.compose.screen

import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

// 策略和生命周期测试的每次导航间隔为 200ms，无需真实等待。
@OptIn(ExperimentalTime::class)
internal class TestNavigationTimeSource : TimeSource.WithComparableMarks {
    private val source = TestTimeSource()

    override fun markNow(): ComparableTimeMark {
        source += 200.milliseconds
        return source.markNow()
    }
}
