package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.WebView
import love.yinlin.compose.ui.WebViewLoadingState
import love.yinlin.compose.ui.WebViewState
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.platform.Platform

@Stable
class ScreenActivityLink : Screen() {
    private val showstart = InputState(maxLength = 16)
    private val webPageState = WebViewState()

    override val title: String = "链接提取"

    @Composable
    override fun Content() {
        Platform.use(
            *Platform.Web, *Platform.Desktop,
            ifTrue = {
                UnsupportedPlatformComponent(modifier = Modifier.fillMaxSize())
            },
            ifFalse = {
                Column(
                    modifier = Modifier
                        .padding(LocalImmersivePadding.current)
                        .fillMaxSize()
                        .padding(Theme.padding.value)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                ) {
                    WebView(
                        state = webPageState,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                    Input(
                        state = showstart,
                        hint = "秀动ID",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(
                            text = "加载",
                            enabled = showstart.isSafe,
                            onClick = {
                                webPageState.url = "https://wap.showstart.com/pages/activity/detail/detail?activityId=${showstart.text}"
                            }
                        )
                        TextButton(
                            text = "提取",
                            enabled = webPageState.loadingState is WebViewLoadingState.Finished,
                            onClick = {
                                webPageState.evaluateJavaScript("document.getElementById('openApp').click();")
                            }
                        )
                    }
                    SelectionBox {
                        Text(text = webPageState.url)
                    }
                }
            }
        )
    }
}