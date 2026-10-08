package love.yinlin.gallery.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.compose.screen.ClearPolicy
import love.yinlin.compose.screen.CreatePolicy
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import kotlin.time.Duration.Companion.milliseconds

@Stable
internal class NestedLabScreen(routes: GalleryRoutes) : GalleryScreen(routes, GallerySection.Nested) {
    @Composable
    override fun PageContent() {
        PageHeading("04", "同一父页面，两份独立导航", "左右都使用同一组 Screen 类型，各自持有 Manager 和实例。只操作左边，右边的页面、计数与草稿应保持原样。")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val panelWidth = if (maxWidth >= 760.dp) (maxWidth - 16.dp) / 2 else maxWidth
            // 始终在同一 FlowRow 中组合；换行不会把 Navigation 移到另一棵组合子树。
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                key("left") { NestedPanel("左侧", Modifier.width(panelWidth)) }
                key("right") { NestedPanel("右侧", Modifier.width(panelWidth)) }
            }
        }
        GalleryCard("观察父子作用域", "覆盖父页面应保留两份子导航；清理父页面后，两个子 Manager 及它们的 Screen 都应清理。") {
            Actions {
                PrimaryButton("覆盖整个父页面") {
                    navigate(routes::cover, "嵌套实验父页 $instanceId 被覆盖。两边的根页面 VM 应继续计时；返回后，两份导航的位置、编号和草稿应保留。")
                }
                SecondaryButton("Replace 父页，创建全新子导航") { navigate(routes::nested, CreatePolicy.Replace + ClearPolicy.None) }
                TextButton("回到概览并清理父子页面") { navigate(routes::overview, CreatePolicy.Move + ClearPolicy.Clear) }
            }
            Paragraph("先在左边 New 到第二层，右边留在根页并修改草稿；覆盖后再返回。观察组合离开事件、计时，以及重新显示时的实例编号。")
            Paragraph("Replace 后，应出现全新的左右根页编号；旧父页和子页的清理事件可在「观察记录」查看。改变窗口宽度只切换布局，下面用 key 维持每侧的组合身份。")
            CodeBlock("""
                // 在父 Screen 的 BasicContent 中：
                ScreenManager.Navigation<ChildMain> {
                    screen(::ChildMain)
                    screen(::ChildDetail)
                }

                // ChildDetail 的 pop() 仅返回自己的子导航。
                // 父 Screen 的 pop() 返回外层导航。
            """)
        }
    }

    @Composable
    private fun NestedPanel(label: String, modifier: Modifier = Modifier) {
        GalleryCard("${label}导航", "计数和草稿由这一侧的页面 VM 维护。", modifier) {
            ScreenManager.Navigation<NestedRootScreen>(Modifier.fillMaxWidth().height(360.dp)) {
                val scope = NestedRoutes(journal, "$label / 父 $instanceId")
                journal.event(scope.name, "注册", "独立子导航 · 同样的 Root / Detail 类型")
                screen(scope::root)
                screen(scope::detail)
            }
        }
    }
}

@Stable
internal class NestedRoutes(val journal: GalleryJournal, val name: String) {
    var lastResult by mutableStateOf("本侧导航已就绪。")
    fun root() = NestedRootScreen(this)
    fun detail(level: Int) = NestedDetailScreen(this, level)
}

@Stable
internal abstract class NestedPage(protected val scope: NestedRoutes, name: String, initialLevel: Int) :
    ObservedScreen(scope.journal, scope.name, name, if (initialLevel == 0) "无导航参数" else "level=$initialLevel") {
    protected var level by mutableStateOf(initialLevel)
    private var counter by mutableStateOf(0)
    protected var ticks by mutableStateOf(0)
    protected val draft = InputState("${scope.name} 的草稿", maxLength = 120)

    protected fun publish() = journal.state(instanceId, "层级 $level · 计数 $counter · ticks $ticks · 草稿 ${draft.text}")

    override fun onInitialize() = publish()

    @Composable
    override fun ScreenContent() {
        val currentDraft = draft.text
        val summary = "层级 $level · 计数 $counter · ticks $ticks · 草稿 $currentDraft"
        SideEffect { journal.state(instanceId, summary) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Badge("${scope.name} · $displayName $instanceId")
            Actions {
                Metric("页面层级", level.toString())
                Metric("独立计数", counter.toString())
                if (level == 0) Metric("根页 ticks", ticks.toString())
            }
            Input(modifier = Modifier.fillMaxWidth(), state = draft, hint = "这一侧的草稿")
            Actions {
                PrimaryButton("New 下一层") {
                    publish()
                    navigate(scope::detail, level + 1)
                    scope.lastResult = "本侧新建第 ${level + 1} 层 Detail。通过当前页面的编号确认新实例，另一侧保持原状态。"
                }
                SecondaryButton("计数 +1") { counter++; publish() }
                TextButton("本侧 pop()") {
                    publish()
                    val result = pop()
                    scope.lastResult = "本侧 pop() = $result${if (result) "" else "，根页保留"}。"
                    journal.event(scope.name, "pop", scope.lastResult)
                }
                TextButton("仅重置本侧") {
                    navigate(scope::root, CreatePolicy.Move + ClearPolicy.Clear)
                    scope.lastResult = "复用本侧根页，仅清理本侧 Detail。根页计数和草稿仍在。"
                }
            }
            Paragraph(scope.lastResult)
        }
    }
}

@Stable
internal class NestedRootScreen(scope: NestedRoutes) : NestedPage(scope, "Root", 0) {
    private var resource: DemoResource? = null

    override fun onInitialize() {
        super.onInitialize()
        resource = DemoResource(journal, scope.name, instanceId)
        viewModelScope.launch {
            try {
                while (isActive) { delay(1000.milliseconds); ticks++; publish() }
            }
            finally { journal.event(scope.name, "协程结束", "$instanceId · 子根页计时结束") }
        }
    }

    override fun onUninitialize() {
        resource?.close()
        journal.event(scope.name, "子作用域检查", "$instanceId · viewModelScope.isActive = ${viewModelScope.isActive}")
    }
}

@Stable
internal class NestedDetailScreen(scope: NestedRoutes, level: Int) : NestedPage(scope, "Detail", level) {
    override fun onResume() = withResume { newLevel: Int -> level = newLevel; publish() }
}
