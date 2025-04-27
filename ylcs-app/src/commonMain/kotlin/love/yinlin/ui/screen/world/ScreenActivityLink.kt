package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.AppModel
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedComponent
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.platform.WebPage
import love.yinlin.ui.component.platform.WebPageSettings
import love.yinlin.ui.component.platform.WebPageState
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.CommonScreen

@Stable
class ScreenActivityLink(model: AppModel) : CommonScreen(model) {
    val showstartInput = TextInputState()
    val webPageState = WebPageState(WebPageSettings())
    var showstartResult by mutableStateOf("无结果")

    @Composable
    override fun Content() {
        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "链接提取",
            onBack = { pop() }
        ) {
            OS.ifPlatform(
                Platform.WebWasm, *Platform.Desktop,
                ifTrue = {
                    UnsupportedComponent(modifier = Modifier.fillMaxSize())
                },
                ifFalse = {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
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
}