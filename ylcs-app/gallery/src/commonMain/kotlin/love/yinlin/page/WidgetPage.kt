package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.compose.ui.widget.*
import love.yinlin.extension.DateEx
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Stable
object WidgetPage : Page() {
    private val HexData = Random.nextBytes(1024 * 1024)

    private class StatusCell(index: Int = 0) : DataTableSelectionCell(["Ready", "Review", "Offline"], index) {
        @Composable
        override fun Display(modifier: Modifier) {
            val value = text
            val color = when (value) {
                "Ready" -> Theme.color.secondary
                "Review" -> Theme.color.warning
                else -> Theme.color.onSurfaceVariant
            }
            Row(
                modifier = modifier.padding(horizontal = Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(Theme.shape.circle).background(color))
                SimpleEllipsisText(text = value, style = Theme.typography.v8, color = color)
            }
        }
    }

    @Stable
    private class Sample(
        device: String = "New device",
        transport: Int = 0,
        rate: Double = 48000.0,
        status: Int = 0,
        enabled: Boolean = true,
        notes: String = "",
    ) : DataTableItem() {
        override val items: List<DataTableCell> = [
            DataTableTextCell(device),
            DataTableSelectionCell(["USB", "WebSocket", "TCP"], transport),
            DataTableNumberCell(rate),
            StatusCell(status),
            DataTableBoolCell(enabled),
            DataTableTextCell(notes),
        ]

        companion object {
            private val devices = ["Rachel gateway", "Studio interface", "Sensor array", "Binary inspector", "Edge controller", "Development board", "Telemetry relay"]
            private val transports = ["USB", "WebSocket", "TCP"]
            private val rates = [48000.0, 1000.0, 2000.0, 24000.0, 9600.0]
            val Default = List(15) { offset ->
                Sample(
                    device = devices[offset % devices.size],
                    transport = offset % transports.size,
                    rate = rates[offset % rates.size],
                    status = when (offset % 7) {
                        4 -> 1
                        6 -> 2
                        else -> 0
                    },
                    enabled = offset % 7 != 6,
                )
            }
            val Columns = [
                DataTableColumn("Device name", DataTableTextCell::class, width = 220.dp),
                DataTableColumn("Transport", DataTableSelectionCell::class, width = 170.dp),
                DataTableColumn("Sample rate / Hz", DataTableNumberCell::class, width = 190.dp),
                DataTableColumn("Status", DataTableSelectionCell::class, width = 160.dp),
                DataTableColumn("Enabled", DataTableBoolCell::class, width = 150.dp),
            ]
        }
    }

    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Calendar") {
                Calendar()
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

            Component("HexView") {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                    val state = remember { HexViewState(HexData) }
                    val selection = state.selectedRange
                    val selectionText = if (selection.isEmpty()) "未选择" else {
                        val start = selection.first.toString(16).uppercase().padStart(8, '0')
                        val end = selection.last.toString(16).uppercase().padStart(8, '0')
                        val value = (state[state.selectedIndex].toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
                        "$start – $end · ${selection.last - selection.first + 1} 字节 · 当前 ${state.selectedIndex} / $value"
                    }

                    Text("${state.size} B · $selectionText", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)

                    HexView(state = state, modifier = Modifier.fillMaxWidth())
                }
            }

            Component("DataTable") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    DataTable(
                        state = remember { DataTableState(Sample.Columns, Sample.Default) },
                        modifier = Modifier.fillMaxWidth(),
                        readonly = false
                    )
                }
            }
        }
    }
}
