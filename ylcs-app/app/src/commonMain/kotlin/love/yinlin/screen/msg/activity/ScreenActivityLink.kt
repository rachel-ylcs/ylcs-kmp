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
import love.yinlin.compose.*
import love.yinlin.compose.screen.CommonScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.platform.Platform
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.layout.UnsupportedComponent
import love.yinlin.ui.component.platform.WebPage
import love.yinlin.ui.component.platform.WebPageLoadingState.Finished
import love.yinlin.ui.component.platform.WebPageSettings
import love.yinlin.ui.component.platform.WebPageState

@Stable
class ScreenActivityLink(manager: ScreenManager) : CommonScreen(manager) {
    val showstartInput = TextInputState()
    val webPageState = WebPageState(WebPageSettings())

    override val title: String = "链接提取"

    @Composable
    override fun Content(device: Device) {
        Platform.use(
            Platform.WebWasm, *Platform.Desktop,
            ifTrue = {
                UnsupportedComponent(modifier = Modifier.fillMaxSize())
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
                    WebPage(
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
                            enabled = webPageState.loadingState is Finished,
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