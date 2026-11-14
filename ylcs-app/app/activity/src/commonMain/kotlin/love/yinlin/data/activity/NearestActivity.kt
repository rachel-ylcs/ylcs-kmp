package love.yinlin.data.activity

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import love.yinlin.data.rachel.activity.Activity

@Stable
data class NearestActivity(val future: Item? = null, val past: Item? = null) {
    @Stable
    data class Item(
        val activity: Activity,
        val date: LocalDate,
        val day: Int,
        val title: String
    )
}