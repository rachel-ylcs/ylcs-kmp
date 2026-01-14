package love.yinlin.screen.msg.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.WebView
import love.yinlin.compose.ui.WebViewConfig
import love.yinlin.compose.ui.WebViewLoadingState
import love.yinlin.compose.ui.WebViewState
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.platform.Platform
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.layout.UnsupportedPlatformComponent

@Stable
class ScreenActivityLink(manager: ScreenManager) : Screen(manager) {
    val showstartInput = TextInputState()
    val webPageState = WebViewState(WebViewConfig())

    override val title: String = "链接提取"

    @Composable
    override fun Content(device: Device) {
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
                        .padding(CustomTheme.padding.value)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace),
                ) {
                    WebView(
                        state = webPageState,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                    TextInput(
                        state = showstartInput,
                        hint = "秀动ID",
                        maxLength = 16,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        ClickText(
                            text = "加载",
                            enabled = showstartInput.ok,
                            onClick = {
                                webPageState.url = "https://wap.showstart.com/pages/activity/detail/detail?activityId=${showstartInput.text}"
                            }
                        )
                        ClickText(
                            text = "提取",
                            enabled = webPageState.loadingState is WebViewLoadingState.Finished,
                            onClick = {
                                webPageState.evaluateJavaScript("document.getElementById('openApp').click();")
                            }
                        )
                    }
                    SelectionContainer {
                        Text(text = webPageState.url)
                    }
                }
            }
        )
    }
}