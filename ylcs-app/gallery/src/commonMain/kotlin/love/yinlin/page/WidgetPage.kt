package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.compose.ui.widget.Calendar
import love.yinlin.compose.ui.widget.DataTable
import love.yinlin.compose.ui.widget.DataTableCellType
import love.yinlin.compose.ui.widget.DataTableColumn
import love.yinlin.compose.ui.widget.DataTableSortDirection
import love.yinlin.compose.ui.widget.rememberDataTableState
import love.yinlin.compose.ui.widget.HexView
import love.yinlin.compose.ui.widget.HexViewState
import love.yinlin.compose.ui.widget.LogLevel
import love.yinlin.compose.ui.widget.LogOutput
import love.yinlin.compose.ui.widget.LogOutputState
import love.yinlin.extension.DateEx
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Stable
object WidgetPage : Page() {
    private val HexData = Random.nextBytes(1024 * 1024)

    // 扩展内置 selection 的显示，校验、排序和下拉编辑仍由内置类型处理。
    private val StatusType = object : DataTableCellType by DataTableCellType.Selection(listOf("Ready", "Review", "Offline")) {
        @Composable
        override fun Display(value: String, modifier: Modifier) {
            val color = when (value) {
                "Ready" -> Theme.color.secondary
                "Review" -> Theme.color.warning
                else -> Theme.color.onSurfaceVariant
            }
            Row(
                modifier = modifier.padding(horizontal = Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h / 2)
            ) {
                Box(Modifier.size(6.dp).clip(Theme.shape.circle).background(color))
                SimpleEllipsisText(value, style = Theme.typography.v8, color = color)
            }
        }
    }

    private fun tableRows(start: Int, count: Int): List<Map<String, String>> {
        val devices = listOf("Rachel gateway", "Studio interface", "Sensor array", "Binary inspector", "Edge controller", "Development board", "Telemetry relay")
        val transports = listOf("USB", "WebSocket", "TCP")
        val rates = listOf("48000", "1000", "2000", "24000", "9600")
        return List(count) { offset ->
            val index = start + offset
            mapOf(
                "device" to devices[index % devices.size],
                "transport" to transports[index % transports.size],
                "rate" to rates[index % rates.size],
                "status" to when (index % 7) { 4 -> "Review"; 6 -> "Offline"; else -> "Ready" },
                "enabled" to (index % 7 != 6).toString(),
                "updated" to "2026-10-13T${(9 + index % 8).toString().padStart(2, '0')}:30",
            )
        }
    }

    @Composable
    private fun rememberTableState() = rememberDataTableState(
        columns = remember { listOf(
            DataTableColumn("device", "Device name", width = 220.dp, defaultValue = "New device"),
            DataTableColumn("transport", "Transport", DataTableCellType.Selection(listOf("USB", "WebSocket", "TCP")), width = 170.dp),
            DataTableColumn("rate", "Sample rate / Hz", DataTableCellType.Number, width = 190.dp, defaultValue = "48000"),
            DataTableColumn("status", "Status", StatusType, width = 160.dp),
            DataTableColumn("enabled", "Enabled", DataTableCellType.Bool, width = 150.dp, defaultValue = "true"),
            DataTableColumn("updated", "Updated at", DataTableCellType.DateTime, width = 220.dp, defaultValue = "2026-10-13T09:30"),
        ) },
        rows = remember { tableRows(0, 12) },
    )

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
                val table = rememberTableState()
                var readonly by rememberState { false }
                var savedData by rememberState { "点击“读取状态”查看当前已保存的前三行数据。" }

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
                            Text("只读", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                            Switch(readonly, { readonly = it })
                        }
                        TextButton("追加 1000 行", enabled = !readonly, onClick = { table.addRows(tableRows(table.rowCount, 1000)) })
                        TextButton("读取状态", onClick = {
                            savedData = table.snapshot().take(3).joinToString("\n") { row ->
                                table.columns.joinToString(" · ") { column -> "${column.title}: ${row.getValue(column.key)}" }
                            }.ifEmpty { "表格为空" }
                        })
                    }

                    Text("${table.rowCount} 行 · 当前列顺序：${table.columns.joinToString(" / ") { it.title }}", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                    val sorting = when (table.sortDirection) {
                        DataTableSortDirection.None -> "未排序"
                        DataTableSortDirection.Ascending -> "升序"
                        DataTableSortDirection.Descending -> "降序"
                    }
                    Text("当前排序：$sorting · 原始前三行：${table.originalRows.take(3).joinToString(" / ") { it["device"] }}", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                    Text("点击数据单元格内嵌编辑，合法输入即时写入状态；布尔值单击切换，选择列使用下拉框。直接拖动表头调整列顺序；未排序时拖动序号调整行顺序，长按序号删除该行，末行增加。", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                    Text("排序按钮依次切换未排序、升序、降序，再恢复原始顺序。排序期间的编辑和增删也会更新原始数据；排序期间禁止调整行顺序，仍可调整列顺序，恢复未排序后可拖动序号。", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                    Text("数字支持负数、小数和科学计数法；日期时间使用 2026-10-13T09:30 格式，无效内容保留最近的合法值。", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                    Text("数据已即时保存，无需手动提交。点击序号或表头取消选中并结束编辑，Enter 保存，Esc 取消并恢复编辑前的值，两者均结束编辑并取消选中；失焦保存并结束编辑。编辑排序列时结束编辑后再调整行位置。只读模式仅支持排序和滚动查看。", style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)

                    DataTable(
                        state = table,
                        modifier = Modifier.fillMaxWidth(),
                        readonly = readonly,
                    )

                    Text(savedData, style = Theme.typography.v9, color = Theme.color.onSurfaceVariant)
                }
            }
        }
    }
}
