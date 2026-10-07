package love.yinlin.gallery.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.compose.screen.ClearPolicy
import love.yinlin.compose.screen.CreatePolicy
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState

@Stable
internal class DemoResource(private val journal: GalleryJournal, private val scope: String, private val owner: String) : AutoCloseable {
    var released by mutableStateOf(false)
        private set

    init { journal.event(scope, "资源创建", "$owner · 演示资源已绑定到页面") }

    override fun close() {
        if (released) return
        released = true
        journal.event(scope, "资源释放", "$owner · release() 执行一次")
    }
}

@Stable
internal class StateScreen(routes: GalleryRoutes) : GalleryScreen(routes, GallerySection.State) {
    private var counter by mutableStateOf(0)
    private var ticks by mutableStateOf(0)
    private var running by mutableStateOf(false)
    private val draft = InputState("这份草稿属于 Screen ViewModel。", maxLength = 500)
    private var ticker: Job? = null
    private var resource: DemoResource? = null

    override fun onInitialize() {
        resource = DemoResource(journal, "状态实验", instanceId)
        startTicker()
    }

    override fun onResume() = withResume {
        draft.text = "Resume 更新草稿，原有计数和计时仍然保留。"
        publish()
    }

    override fun onUninitialize() {
        running = false
        resource?.close()
        publish()
        journal.event("状态实验", "作用域检查", "$instanceId · viewModelScope.isActive = ${viewModelScope.isActive}")
    }

    private fun startTicker() {
        if (running) return
        running = true
        journal.event("状态实验", "协程启动", "$instanceId · viewModelScope，每秒一次")
        ticker = viewModelScope.launch {
            try {
                while (isActive) {
                    delay(1_000)
                    ticks++
                    publish()
                }
            }
            finally { journal.event("状态实验", "协程结束", "$instanceId · 计时任务 finally，ticks=$ticks") }
        }
        publish()
    }

    private fun publish() {
        journal.state(instanceId, "VM计数 $counter · ticks $ticks · 草稿 ${draft.text.length} 字 · 资源${if (resource?.released == true) "已释放" else "占用中"}")
    }

    @Composable
    override fun PageContent() {
        var rememberedCounter by remember { mutableStateOf(0) }
        var savedCounter by rememberSaveable { mutableStateOf(0) }
        val draftLength = draft.text.length
        val summary = "VM计数 $counter · ticks $ticks · 草稿 $draftLength 字 · 资源${if (resource?.released == true) "已释放" else "占用中"}"
        SideEffect { journal.state(instanceId, summary) }
        PageHeading("03", "页面被覆盖，数据还在", "给三种状态各加几次，然后打开覆盖页。等待退出动画结束再返回，比较 UI 状态与 ViewModel 状态。")
        GalleryCard("三种状态，各自归属", "普通 remember 在内容真正离开组合后会重新创建；rememberSaveable 由条目状态持有者保存；ViewModel 状态留在本实例里。") {
            Actions {
                Metric("ViewModel 计数", counter.toString())
                Metric("remember 计数", rememberedCounter.toString())
                Metric("rememberSaveable 计数", savedCounter.toString())
            }
            Actions {
                PrimaryButton("VM +1") { counter++; publish() }
                SecondaryButton("remember +1") { rememberedCounter++ }
                SecondaryButton("saveable +1") { savedCounter++ }
            }
            Input(draft, Modifier.fillMaxWidth(), hint = "编辑一份页面草稿", minLines = 2, maxLines = 4)
            Paragraph("草稿由 VM 的 InputState 保存。这里的保存指仍在当前进程、当前导航条目作用域内保留。")
        }
        GalleryCard("后台页面的协程与资源", "页面留在栈内时，viewModelScope 不会因为 UI 被覆盖而取消。计时应继续；只有页面清理或主动暂停才结束计时任务。") {
            Actions {
                Metric("累计 ticks", ticks.toString())
                Metric("计时任务", if (running) "运行中" else "已暂停")
                Metric("演示资源", if (resource?.released == true) "已释放" else "占用中")
                Metric("resume 回调", resumeCount.toString())
            }
            Actions {
                SecondaryButton(if (running) "暂停任务" else "继续任务") {
                    if (running) { running = false; ticker?.cancel(); publish() }
                    else startTicker()
                }
                PrimaryButton("用一个页面覆盖我") {
                    publish()
                    navigate(routes::cover, "状态页面 $instanceId 被覆盖。等待约一秒后，VM 的 ticks 应继续增加；返回只恢复显示，不会自动调用 resume。")
                }
                TextButton("显式 Resume 本实例") { navigate(routes::state, CreatePolicy.Resume + ClearPolicy.None) }
                TextButton("Replace 为全新实例") { navigate(routes::state, CreatePolicy.Replace + ClearPolicy.None) }
                TextButton("回到概览并清理我") { navigate(routes::overview, CreatePolicy.Move + ClearPolicy.Clear) }
            }
        }
        GalleryCard("推荐观察顺序") {
            Paragraph("1. 修改计数与草稿，打开覆盖页；观察状态实例仍为存活、组合数变为 0，ticks 继续增加。")
            Paragraph("2. 返回，检查编号相同、VM 和 saveable 计数保留。普通 remember 的表现取决于内容是否已真正离开组合。")
            Paragraph("3. 显式 Resume，应增加回调次数并更新草稿。普通返回应保持回调次数不变。")
            Paragraph("4. Replace 或回概览清理，等待退出动画完成；观察旧编号的协程结束、资源释放和 uninitialize。")
            CodeBlock("""
                override fun initialize() {
                    resource = createResource()
                    viewModelScope.launch { /* 页面任务 */ }
                }

                override fun uninitialize() {
                    resource.release()
                }
            """)
            Paragraph("演示资源是一个 commonMain 对象，用于验证释放时机；播放器、WebView 等仍应按实际平台对象的使用要求释放。")
        }
    }
}

@Stable
internal class CoverScreen(routes: GalleryRoutes, message: String) : GalleryScreen(routes, GallerySection.State, "覆盖页", message) {
    private var message by mutableStateOf(message)
    override fun onResume() = withResume { next: String -> message = next }

    @Composable
    override fun PageContent() {
        PageHeading("03 / 04", "前一个页面暂时看不见", message)
        GalleryCard("被保留的实例快照", "下方记录来自各页面自己的状态更新。组合数为 0 仍可能有存活的 VM 和运行中的协程。") {
            val records = journal.instances.filter { !it.cleared && it.id != instanceId }
            records.takeLast(12).forEach { record ->
                Badge("${record.scope} · ${record.name} ${record.id} · 组合 ${record.compositions}")
                Paragraph(record.state)
            }
            Actions {
                PrimaryButton("返回被覆盖的页面") { pop() }
                SecondaryButton("返回概览并清理沿途页面") { navigate(routes::overview, CreatePolicy.Move + ClearPolicy.Clear) }
            }
            Paragraph("本页没有发送 Resume。返回后 resume 次数应保持原值。清理回调可能等待当前退出动画完成。")
        }
    }
}
