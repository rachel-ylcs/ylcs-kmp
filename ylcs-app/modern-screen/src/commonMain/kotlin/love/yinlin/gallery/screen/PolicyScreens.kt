package love.yinlin.gallery.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.compose.screen.ClearPolicy
import love.yinlin.compose.screen.CreatePolicy
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.screen.ScreenModel
import love.yinlin.compose.ui.input.CheckBox
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState

@Stable
internal class PolicyLabScreen(routes: GalleryRoutes) : GalleryScreen(routes, GallerySection.Policies) {
    @Composable
    override fun PageContent() {
        PageHeading("01", "选择策略，辨认同一份页面", "A 与 B 是两种页面类型。相同类型可以有多份实例；参数并不参与 Move、Resume 和 Replace 的匹配。")
        GalleryCard("策略实验台", "建议先创建示例栈 H → A1 → B1 → A2 → B2，再选择 A、请求参数 99，比较实际显示的实例编号和逐次 pop 的顺序。") {
            ScreenManager.Navigation<PolicyRootScreen>(Modifier.fillMaxWidth().height(660.dp)) {
                val scope = PolicyRoutes(journal, "策略 $instanceId")
                journal.event(scope.name, "注册", "独立导航 · H / A(Int) / B(Int)")
                screen(scope::root)
                screen(scope::alpha)
                screen(scope::beta)
            }
        }
        GalleryCard("八种组合的契约") {
            Paragraph("New：总是新增；即使选择 Clear，也保留原栈。")
            Paragraph("Replace：替换最靠近栈顶的同类型实例，再将新实例放到栈顶。")
            Paragraph("Move：复用最靠近栈顶的同类型实例；不更新参数，也不调用 resume。")
            Paragraph("Resume：复用同一实例并调用 resume；本演示只更新当前参数，保留计数器和初始参数。")
            Paragraph("Clear：找到同类型目标时，清理它上方的页面；目标下方保持原样。没有目标时，Replace / Move / Resume 都按 New 处理，并忽略 Clear。")
            CodeBlock("""
                navigate(routes::alpha, 99, CreatePolicy.New + ClearPolicy.None)
                navigate(routes::alpha, 99, CreatePolicy.Move + ClearPolicy.Clear)
                navigate(routes::alpha, 99, CreatePolicy.Resume + ClearPolicy.None)

                override fun resume() = withResume { value: Int ->
                    currentValue = value
                }
            """)
        }
    }
}

@Stable
internal class PolicyRoutes(val journal: GalleryJournal, val name: String) {
    var create by mutableStateOf(CreatePolicy.New)
    var clear by mutableStateOf(false)
    var target by mutableStateOf("A")
    val request = InputState("99", maxLength = 8)
    var lastResult by mutableStateOf("实验根页 H 已就绪。pop() 会保留最后一个页面。")
    var seedDescription by mutableStateOf("尚未创建示例栈。可以在这里直接验证「无匹配目标」的回退行为。")
    private var seedOnResume = false
    private var seeding = false
    private var lastCreatedId = ""

    fun root() = PolicyRootScreen(this)
    fun alpha(value: Int) = PolicyAlphaScreen(this, value, if (seeding && value == 2) 3 else 0).also { lastCreatedId = it.instanceId }
    fun beta(value: Int) = PolicyBetaScreen(this, value).also { lastCreatedId = it.instanceId }

    fun send(caller: ScreenModel) {
        val value = request.text.toIntOrNull() ?: return
        val policy = create + if (clear) ClearPolicy.Clear else ClearPolicy.None
        journal.event(name, "请求", "$create + ${policy.clearPolicy} → $target($value)")
        if (target == "A") caller.navigate(::alpha, value, policy) else caller.navigate(::beta, value, policy)
        lastResult = "已执行 $create + ${policy.clearPolicy} → $target($value)。请查看当前页面的真实编号、初始参数、当前参数、计数和 resume 次数。"
    }

    fun reset(caller: ScreenModel, seed: Boolean) {
        seedOnResume = seed
        lastResult = "复用原有根页 H。A / B 出栈，等待相应的 UI 引用释放。"
        seedDescription = "当前已重置到 H；下一次 A / B 请求没有同类型目标。"
        // 根页在 Resume 回调中自行创建示例栈，不需要取得 navigate 的返回值。
        caller.navigate(::root, CreatePolicy.Resume + ClearPolicy.Clear)
    }

    fun seedIfRequested(caller: ScreenModel, rootId: String) {
        if (!seedOnResume) return
        seedOnResume = false
        seeding = true
        try {
            caller.navigate(::alpha, 1)
            val a1 = lastCreatedId
            caller.navigate(::beta, 1)
            val b1 = lastCreatedId
            caller.navigate(::alpha, 2)
            val a2 = lastCreatedId
            caller.navigate(::beta, 2)
            val b2 = lastCreatedId
            seedDescription = "本轮创建：H $rootId → A1 $a1 → B1 $b1 → A2 $a2 → B2 $b2。编号由注册工厂记录，只保存字符串。"
            lastResult = "示例栈创建完成。当前显示 B2 $b2；目标 A 应匹配 A2 $a2。"
            journal.event(name, "示例就绪", seedDescription)
        }
        finally { seeding = false }
    }

    fun prediction(): String {
        val prefix = if (target == "A") "H → A1 → B1" else "H → A1 → B1 → A2"
        val intervening = if (target == "A" && !clear) " → B2" else ""
        return when (create) {
            CreatePolicy.New -> "H → A1 → B1 → A2 → B2 → $target(新)"
            CreatePolicy.Replace -> "$prefix$intervening → $target(新)"
            CreatePolicy.Move -> "$prefix$intervening → ${target}2"
            CreatePolicy.Resume -> "$prefix$intervening → ${target}2(当前=${request.text})"
        }
    }
}

@Stable
internal abstract class PolicyPage(
    protected val scope: PolicyRoutes,
    name: String,
    val initialValue: Int?,
    initialCounter: Int = 0,
) : ObservedScreen(scope.journal, scope.name, name, initialValue?.let { "value=$it" } ?: "实验根页，无导航参数") {
    var currentValue by mutableStateOf(initialValue)
        private set
    var counter by mutableStateOf(initialCounter)
        private set

    override fun onResume() {
        if (initialValue == null) withResume { }
        else withResume { value: Int -> currentValue = value }
        publish()
    }

    override fun onInitialize() = publish()

    private fun publish() = journal.state(instanceId, "初始 $initialValue · 当前 $currentValue · 计数 $counter")

    fun increment() { counter++; publish() }

    @Composable
    override fun ScreenContent() {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Actions {
                Badge("当前真实页面：$displayName $instanceId")
                Badge(scope.name)
            }
            Actions {
                Metric("初始参数", initialValue?.toString() ?: "无参")
                Metric("当前参数", currentValue?.toString() ?: "无参")
                Metric("独立计数器", counter.toString())
                Metric("resume 回调", resumeCount.toString())
            }
            Actions {
                SecondaryButton("此实例计数 +1") { increment() }
                TextButton("pop() 返回一层") {
                    val result = pop()
                    scope.lastResult = "$displayName $instanceId 发起 pop()，返回 $result。${if (result) "通过新显示的编号确认返回顺序。" else "已经是实验根页。"}"
                    journal.event(scope.name, "pop", scope.lastResult)
                }
            }
            Filter(size = CreatePolicy.entries.size, selectedProvider = { scope.create == CreatePolicy.entries[it] },
                titleProvider = { CreatePolicy.entries[it].name }, onClick = { index, _ -> scope.create = CreatePolicy.entries[index] })
            Actions {
                Filter(size = 2, selectedProvider = { scope.target == if (it == 0) "A" else "B" },
                    titleProvider = { if (it == 0) "目标 A" else "目标 B" }, onClick = { index, _ -> scope.target = if (index == 0) "A" else "B" })
                CheckBox(scope.clear, { scope.clear = it }, "Clear 上方页面")
                Input(scope.request, Modifier.width(160.dp), hint = "整数参数")
            }
            Actions {
                PrimaryButton("执行 ${scope.create} → ${scope.target}", enabled = scope.request.text.toIntOrNull() != null) { scope.send(this@PolicyPage) }
                SecondaryButton("重建示例栈") { scope.reset(this@PolicyPage, seed = true) }
                TextButton("清空到根页") { scope.reset(this@PolicyPage, seed = false) }
                TextButton("快速 New × 5") {
                    repeat(5) { index ->
                        navigate(scope::alpha, 100 + index)
                    }
                    scope.lastResult = "快速创建 A(100..104)。请查看当前页面的编号，并连续 pop 检查没有显示过的页面。"
                    journal.event(scope.name, "批量导航", scope.lastResult)
                }
            }
            Paragraph(scope.lastResult)
            GalleryCard("示例推演 · 预期顺序", "下面针对固定示例栈推演，不读取或模拟 Manager 的实际栈。自由操作后，请通过 pop 和实例编号确认真实顺序。") {
                Paragraph(scope.seedDescription)
                CodeBlock("起点：H → A1 → B1 → A2 → B2\n预期：${scope.prediction()}")
                Paragraph("示例栈把 A2 的计数预设为 3。同类匹配忽略参数：请求 A(99) 应命中 A2；Move / Resume 应保留编号和计数，Replace 应创建计数为 0 的新实例。")
            }
        }
    }
}

@Stable
internal class PolicyRootScreen(scope: PolicyRoutes) : PolicyPage(scope, "H", null) {
    override fun onResume() {
        super.onResume()
        scope.seedIfRequested(this, instanceId)
    }
}

@Stable
internal class PolicyAlphaScreen(scope: PolicyRoutes, value: Int, initialCounter: Int = 0) : PolicyPage(scope, "A", value, initialCounter)

@Stable
internal class PolicyBetaScreen(scope: PolicyRoutes, value: Int) : PolicyPage(scope, "B", value)
