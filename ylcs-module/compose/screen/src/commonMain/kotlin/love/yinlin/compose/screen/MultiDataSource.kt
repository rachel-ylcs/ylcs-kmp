package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

/**
 * 多源数据源
 *
 * 由多个数据源绑定的组合数据源，统一释放
 */
@Stable
class MultiDataSource(vararg val sources: DataSource) : DataSource {
    override fun onDataSourceClean() {
        for (source in sources) source.onDataSourceClean()
    }
}