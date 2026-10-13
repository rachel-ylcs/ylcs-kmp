package love.yinlin.page

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.compose.ui.widget.Calendar
import love.yinlin.compose.ui.widget.LogLevel
import love.yinlin.compose.ui.widget.LogOutput
import love.yinlin.compose.ui.widget.LogOutputState
import love.yinlin.extension.DateEx
import kotlin.time.Duration.Companion.seconds

@Stable
object WidgetPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Calendar") {
                Calendar(modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth())
            }

            Component("LogOutput") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    val log = remember { LogOutputState() }
                    val search = rememberInputState()
                    var minimumLevel by rememberState { LogLevel.Trace }
                    var live by rememberTrue()
                    val query = search.text

                    LaunchedEffect(log) {
                        repeat(80) { index ->
                            log.append("Initial output ${index + 1}", source = "startup")
                        }
                        var v = 0
                        while (true) {
                            if (live) {
                                ++v
                                when (v % 5) {
                                    0 -> log.append("Device timeout $v\nRetry scheduled", LogLevel.Error, source = "device")
                                    2 -> log.append("\u001b[33mRetrying\u001b[0m connection $v", LogLevel.Warn, source = "network")
                                    else -> log.append(
                                        "\u001b[32mConnected\u001b[0m to local device · transport ready $v",
                                        timestamp = DateEx.Current,
                                        source = "console",
                                    )
                                }
                            }
                            delay(1.seconds)
                        }
                    }

                    Input(state = search, hint = "外部过滤：日志内容或来源", modifier = Modifier.fillMaxWidth())

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        TextButton("全部", onClick = { minimumLevel = LogLevel.Trace })
                        TextButton("警告及错误", onClick = { minimumLevel = LogLevel.Warn })
                        TextButton("仅错误", onClick = { minimumLevel = LogLevel.Error })
                        TextButton("清空", onClick = { log.clear() })
                        TextButton(if (live) "暂停输出" else "继续输出", onClick = { live = !live })
                        TextButton("追加 100 行", onClick = {
                            repeat(100) { index ->
                                log.append("Batch output ${index + 1}", source = "batch")
                            }
                        })
                    }

                    LogOutput(
                        state = log,
                        modifier = Modifier.fillMaxWidth(),
                        filter = { entry ->
                            entry.level >= minimumLevel && (query.isBlank() || entry.text.contains(query, ignoreCase = true) || entry.source.contains(query, ignoreCase = true))
                        }
                    )
                }
            }
        }
    }
}
