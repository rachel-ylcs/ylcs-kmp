package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.screen.DataSource
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.cs.APIConfig
import love.yinlin.data.rachel.discovery.DiscoveryItem
import love.yinlin.data.rachel.topic.Topic

@Stable
object DataSourceDiscovery : DataSource {
    internal var currentPage by mutableIntStateOf(0)

    val currentSection: Int get() = DiscoveryItem.entries[currentPage].id

    val page = object : PaginationArgs<Topic, Int, Int, Double>(
        default = Int.MAX_VALUE,
        default1 = Double.MAX_VALUE,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: Topic): Int = item.tid
        override fun offset(item: Topic): Int = item.tid
        override fun arg1(item: Topic): Double = item.score
    }

    override fun onDataSourceClean() {
        page.items.clear()
        page.canLoading = false
        currentPage = 0
    }
}