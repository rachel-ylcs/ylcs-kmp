package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

/**
 * 数据源
 *
 * 为共用页面提供单一稳定的数据源实现状态共享，
 * 生命周期与绑定的页面同步
 */
@Stable
interface DataSource {
    fun onDataSourceClean()
}