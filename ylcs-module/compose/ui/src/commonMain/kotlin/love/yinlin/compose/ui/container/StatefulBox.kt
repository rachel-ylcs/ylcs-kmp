package love.yinlin.compose.ui.container

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text

@Stable
enum class StatefulStatus {
    Content, // 内容
    Custom, // 自定义
    Empty, // 空
    NetworkError, // 网络异常
    Loading, // 加载中
}

@Stable
interface StatefulProvider {
    var status: StatefulStatus

    @Composable
    fun CustomLayout()
    @Composable
    fun EmptyLayout()
    @Composable
    fun NetworkErrorLayout()
    @Composable
    fun LoadingLayout()
}

@Stable
open class DefaultStatefulProvider(
    initStatus: StatefulStatus = StatefulStatus.Empty,
    private val emptyText: String? = ValueTheme.runtime(),
    private val networkErrorText: String? = ValueTheme.runtime(),
    private val loadingText: String? = ValueTheme.runtime(),
) : StatefulProvider {
    override var status by mutableStateOf(initStatus)

    @Composable
    override fun CustomLayout() {}

    @Composable
    override fun EmptyLayout() {
        Column(
            modifier = Modifier.padding(Theme.padding.value10),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
        ) {
            Icon(icon = Icons.Error, modifier = Modifier.size(Theme.size.image9))
            Text(text = emptyText ?: Theme.value.statefulBoxDefaultEmptyText)
        }
    }

    @Composable
    override fun NetworkErrorLayout() {
        Column(
            modifier = Modifier.padding(Theme.padding.value10),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
        ) {
            Icon(icon = Icons.WifiOff, modifier = Modifier.size(Theme.size.image9))
            Text(text = networkErrorText ?: Theme.value.statefulBoxDefaultNetworkErrorText)
        }
    }

    @Composable
    override fun LoadingLayout() {
        Column(
            modifier = Modifier.padding(Theme.padding.value10),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
        ) {
            CircleLoading.Content(modifier = Modifier.size(Theme.size.image9))
            Text(text = loadingText ?: Theme.value.statefulBoxDefaultLoadingText)
        }
    }
}

@Composable
fun StatefulBox(
    provider: StatefulProvider,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimationContent(
        state = provider.status,
        modifier = modifier,
        alignment = Alignment.Center,
        enter = { fadeIn(tween(it)) },
        exit = { fadeOut(tween(it)) }
    ) { status ->
        Box(contentAlignment = Alignment.Center) {
            when (status) {
                StatefulStatus.Content -> content()
                StatefulStatus.Empty -> provider.EmptyLayout()
                StatefulStatus.NetworkError -> provider.NetworkErrorLayout()
                StatefulStatus.Loading -> provider.LoadingLayout()
                StatefulStatus.Custom -> provider.CustomLayout()
            }
        }
    }
}