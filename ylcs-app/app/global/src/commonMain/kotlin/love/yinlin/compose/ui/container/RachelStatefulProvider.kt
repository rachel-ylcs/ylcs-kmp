package love.yinlin.compose.ui.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.app.global.resources.*
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.catchingDefault

class RachelStatefulProvider(
    initStatus: StatefulStatus = StatefulStatus.Empty,
    private val emptyText: String? = ValueTheme.runtime(),
    private val networkErrorText: String? = ValueTheme.runtime(),
    private val loadingText: String? = ValueTheme.runtime(),
    private val emptyHandler: (() -> Unit)? = null,
    private val emptyHandlerText: String = "跳转",
    private val networkErrorHandler: (() -> Unit)? = null,
    private val networkErrorHandlerText: String = "重试",
) : StatefulProvider {
    override var status by mutableStateOf(initStatus)

    val isLoading: Boolean get() = status == StatefulStatus.Loading

    suspend inline fun withLoading(loading: Boolean = true, block: suspend () -> Boolean) {
        if (status != StatefulStatus.Loading) {
            if (loading) status = StatefulStatus.Loading
            status = catchingDefault(StatefulStatus.NetworkError) {
                if (block()) StatefulStatus.Content else StatefulStatus.Empty
            }
        }
    }

    @Composable
    override fun CustomLayout() { }

    @Composable
    override fun EmptyLayout() {
        Column(
            modifier = Modifier.padding(Theme.padding.value6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)
        ) {
            Image(res = Res.drawable.img_state_empty, modifier = Modifier.size(Theme.size.image2))
            Text(text = emptyText ?: Theme.value.statefulBoxDefaultEmptyText, style = Theme.typography.v5)
            if (emptyHandler != null) PrimaryButton(text = emptyHandlerText, onClick = emptyHandler)
        }
    }

    @Composable
    override fun NetworkErrorLayout() {
        Column(
            modifier = Modifier.padding(Theme.padding.value6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)
        ) {
            Image(res = Res.drawable.img_state_network_error, modifier = Modifier.size(Theme.size.image2))
            Text(text = networkErrorText ?: Theme.value.statefulBoxDefaultNetworkErrorText, style = Theme.typography.v5)
            if (networkErrorHandler != null) SecondaryButton(text = networkErrorHandlerText, onClick = networkErrorHandler)
        }
    }

    @Composable
    override fun LoadingLayout() {
        Column(
            modifier = Modifier.padding(Theme.padding.value6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)
        ) {
            Image(res = Res.drawable.img_state_loading, modifier = Modifier.size(Theme.size.image2))
            Text(text = loadingText ?: Theme.value.statefulBoxDefaultLoadingText, style = Theme.typography.v5)
            CircleLoading.Content(modifier = Modifier.size(Theme.size.image9))
        }
    }
}