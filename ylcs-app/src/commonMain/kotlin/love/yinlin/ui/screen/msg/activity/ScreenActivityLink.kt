package love.yinlin.ui.screen.msg.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedComponent
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.platform.WebPage
import love.yinlin.ui.component.platform.WebPageLoadingState.Finished
import love.yinlin.ui.component.platform.WebPageSettings
import love.yinlin.ui.component.platform.WebPageState
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState

@Stable
class ScreenActivityLink(model: AppModel) : CommonSubScreen(model) {
    val showstartInput = TextInputState()
    val webPageState = WebPageState(WebPageSettings())

    override val title: String = "链接提取"

    @Composable
    override fun SubContent(device: Device) {
        OS.ifPlatform(
            Platform.WebWasm, *Platform.Desktop,
            ifTrue = {
                UnsupportedComponent(modifier = Modifier.fillMaxSize())
            },
            ifFalse = {
                Column(
                    modifier = Modifier
                        .padding(LocalImmersivePadding.current)
                        .fillMaxSize()
                        .padding(ThemeValue.Padding.Value)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace),
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
                        RachelButton(
                            text = "加载",
                            enabled = showstartInput.ok,
                            onClick = {
                                webPageState.url = "https://wap.showstart.com/pages/activity/detail/detail?activityId=${showstartInput.text}"
                            }
                        )
                        RachelButton(
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