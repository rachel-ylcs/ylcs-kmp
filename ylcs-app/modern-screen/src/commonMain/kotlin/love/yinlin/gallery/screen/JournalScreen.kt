package love.yinlin.gallery.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.TextButton

@Stable
internal class JournalScreen(routes: GalleryRoutes) : GalleryScreen(routes, GallerySection.Journal) {
    private var instanceFilter by mutableStateOf(0)
    private var eventFilter by mutableStateOf(0)
    private val instanceLabels: List<String> = ["尚未清理", "已清理", "全部"]
    private val eventLabels: List<String> = ["全部", "生命周期", "导航与请求", "资源与协程"]
    private val lifecycleKinds: List<String> = ["构造", "initialize", "resume", "uninitialize", "进入组合", "离开组合"]
    private val navigationKinds: List<String> = ["navigate", "pop", "请求", "批量导航", "示例就绪", "根页面返回", "预期失败", "意外成功"]

    private fun eventMatches(event: GalleryEvent): Boolean = when (eventFilter) {
        1 -> event.kind in lifecycleKinds
        2 -> event.kind in navigationKinds
        3 -> event.kind.startsWith("资源") || event.kind.startsWith("协程") || event.kind.contains("作用域")
        else -> true
    }

    @Composable
    override fun PageContent() {
        PageHeading("06", "记录事实，再判断行为", "编号、回调与状态快照都来自真实 Screen 实例。这里只保存值，不保存 Screen 引用，也不把记录当作内部导航栈。")
        GalleryCard("本次会话") {
            Actions {
                Metric("累计构造", journal.createdCount.toString())
                Metric("累计清理", journal.clearedCount.toString())
                Metric("尚未清理", (journal.createdCount - journal.clearedCount).toString())
                Metric("已保留事件", "${journal.events.size} / 160")
            }
            Paragraph("尚未清理包括栈内页面和仍被退出动画引用的页面。一个页面离开组合，不等于 ViewModel 已被清理。")
        }
        GalleryCard("实例观察", "默认查看尚未清理的实例。最多展示最近 24 份记录；全部存活实例与最近 40 份已清理实例会保留在会话中。") {
            Filter(size = instanceLabels.size, selectedProvider = { instanceFilter == it }, titleProvider = { instanceLabels[it] },
                onClick = { index, _ -> instanceFilter = index })
            val records = journal.instances.filter {
                when (instanceFilter) { 0 -> !it.cleared; 1 -> it.cleared; else -> true }
            }.takeLast(24).asReversed()
            if (records.isEmpty()) Paragraph("这个筛选条件下还没有实例。")
            records.forEach { record ->
                GalleryCard("${record.name} ${record.id}", record.scope) {
                    Actions {
                        Badge(when {
                            record.cleared -> "已清理"
                            record.compositions > 0 -> "UI 在组合中"
                            else -> "VM 保留 / 无 UI"
                        }, if (record.cleared) Theme.color.onSurfaceVariant else Theme.color.primary)
                        Badge("initialize ${if (record.initialized) 1 else 0} 次")
                        Badge("resume ${record.resumes} 次")
                        Badge("组合数 ${record.compositions}")
                    }
                    Paragraph("初始：${record.initial}")
                    Paragraph("状态：${record.state}")
                }
            }
        }
        GalleryCard("事件时间线", "按最近发生的顺序显示。保留最近 160 条，本页最多显示筛选后的 60 条；时间从本次 Gallery 会话创建开始计算。") {
            Filter(size = eventLabels.size, selectedProvider = { eventFilter == it }, titleProvider = { eventLabels[it] },
                onClick = { index, _ -> eventFilter = index })
            TextButton("清空事件记录") {
                journal.events.clear()
                journal.event("Gallery", "记录清空", "仅清空演示事件，页面与实例计数保持原样。")
            }
            EventRows(journal.events.filter(::eventMatches).takeLast(60).asReversed())
        }
    }
}
