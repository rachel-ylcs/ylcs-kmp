package love.yinlin.gallery.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.compose.screen.ClearPolicy
import love.yinlin.compose.screen.CreatePolicy
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.input.CheckBox
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.toJsonString
import kotlinx.serialization.Serializable
import love.yinlin.compose.screen.ScreenModel

@Serializable
internal enum class ArgumentMode { Preview, Edit }

@Immutable
@Serializable
internal data class PayloadOwner(val id: Int, val name: String)

@Immutable
@Serializable
internal data class DemoPayload(val revision: Int, val owner: PayloadOwner, val tags: List<String>, val note: String?, val enabled: Boolean)

@Immutable
internal data class ArgumentField(val type: String, val value: String)

private fun fields(value: Int): List<ArgumentField> = [ArgumentField("Int", value.toString())]
private fun fields(value: Int, text: String?): List<ArgumentField> = fields(value) + ArgumentField("String?", text.toJsonString())
private fun fields(value: Int, text: String?, mode: ArgumentMode): List<ArgumentField> = fields(value, text) + ArgumentField("Enum", mode.name)
private fun fields(value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload): List<ArgumentField> = fields(value, text, mode) + ArgumentField("Object", payload.toJsonString())
private fun fields(value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload, sequence: Long): List<ArgumentField> = fields(value, text, mode, payload) + ArgumentField("Long", sequence.toString())

@Stable
internal class ArgumentLabScreen(routes: GalleryRoutes) : GalleryScreen(routes, GallerySection.Arguments) {
    @Composable
    override fun PageContent() {
        PageHeading("02", "参数传递，保持类型", "六种工厂签名对应 0–5 个导航参数。观察实际构造值，再发送一轮 Resume，比较初始参数与当前状态。")
        GalleryCard("参数实验台", "先 New，再切换到下一轮数据并 Resume。同一个编号意味着复用；初始参数应保留，而当前状态由 withResume 更新。") {
            ScreenManager.Navigation<ArgumentRootScreen>(Modifier.fillMaxWidth().height(720.dp)) {
                val scope = ArgumentRoutes(journal, "参数 $instanceId")
                journal.event(scope.name, "注册", "无参入口与 0–5 参数页面")
                screen(scope::root)
                screen(scope::args0)
                screen(scope::args1)
                screen(scope::args2)
                screen(scope::args3)
                screen(scope::args4)
                screen(scope::args5)
            }
        }
        GalleryCard("本实验包含的数据") {
            Paragraph("Int、可空 String、可序列化 Enum、包含子对象 / List / null / Boolean 的 Object，以及超过 JavaScript Number 精确整数范围的 Long。")
            Paragraph("字符串包含中文、引号、分隔符和换行。Object 的实际值通过框架默认 Json 输出，Long 用 Kotlin Long 原样显示，便于对比 Desktop 与 Wasm。")
            Paragraph("Resume 的参数属于本次请求，不会覆盖 ScreenKey 中的初始参数。保留着的 ViewModel 负责保存更新后的状态；这里不做进程结束恢复。")
        }
    }
}

@Stable
internal class ArgumentRoutes(val journal: GalleryJournal, val name: String) {
    var arity by mutableStateOf(5)
    var revision by mutableStateOf(0)
    var useNull by mutableStateOf(false)
    var lastResult by mutableStateOf("选一个参数数量，然后 New。直接 Resume 也可以测试无目标时的 New 回退。")

    fun root() = ArgumentRootScreen(this)
    fun args0() = Argument0Screen(this)
    fun args1(value: Int) = Argument1Screen(this, value)
    fun args2(value: Int, text: String?) = Argument2Screen(this, value, text)
    fun args3(value: Int, text: String?, mode: ArgumentMode) = Argument3Screen(this, value, text, mode)
    fun args4(value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload) = Argument4Screen(this, value, text, mode, payload)
    fun args5(value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload, sequence: Long) = Argument5Screen(this, value, text, mode, payload, sequence)

    fun send(caller: ScreenModel, create: CreatePolicy) {
        val value = revision * 10 + 7
        val text: String? = if (useNull) null else "银临 | / JSON · 第 $revision 轮\n第二行：\"你好\""
        val mode = if (revision % 2 == 0) ArgumentMode.Preview else ArgumentMode.Edit
        val payload = DemoPayload(revision, PayloadOwner(42 + revision, "演示用户 🧭"), ["中文", "a|b", "第 $revision 轮"], null, revision % 2 == 0)
        val sequence = 9_007_199_254_740_993L + revision
        val policy = create + ClearPolicy.None
        journal.event(name, "请求", "$arity 个参数 · 第 $revision 轮 · $create")
        when (arity) {
            0 -> caller.navigate(::args0, policy)
            1 -> caller.navigate(::args1, value, policy)
            2 -> caller.navigate(::args2, value, text, policy)
            3 -> caller.navigate(::args3, value, text, mode, policy)
            4 -> caller.navigate(::args4, value, text, mode, payload, policy)
            else -> caller.navigate(::args5, value, text, mode, payload, sequence, policy)
        }
        lastResult = "已执行 $create → Args$arity。请查看当前页面的真实编号、resume 次数和独立计数，比较初始参数与当前状态。"
    }

    fun code(): String {
        val callArgs = when (arity) {
            0 -> ""
            1 -> ", 7"
            2 -> ", 7, null"
            3 -> ", 7, null, ArgumentMode.Preview"
            4 -> ", 7, null, ArgumentMode.Preview, payload"
            else -> ", 7, null, ArgumentMode.Preview, payload, 9007199254740993L"
        }
        val parameters: List<String> = ["value: Int", "text: String?", "mode: ArgumentMode", "payload: DemoPayload", "sequence: Long"]
        val resumeArgs = parameters.take(arity).joinToString()
        val lambda = if (arity == 0) "" else "$resumeArgs ->"
        return "screen(routes::args$arity)\nnavigate(routes::args$arity$callArgs, CreatePolicy.Resume + ClearPolicy.None)\n\noverride fun resume() = withResume { $lambda\n    // 更新本实例的当前状态\n}"
    }
}

@Stable
internal abstract class ArgumentPage(
    protected val scope: ArgumentRoutes,
    name: String,
    private val initialFields: List<ArgumentField>,
) : ObservedScreen(scope.journal, scope.name, name, initialFields.joinToString { "${it.type}=${it.value}" }.ifEmpty { "无导航参数" }) {
    private var currentFields by mutableStateOf(initialFields)
    var counter by mutableStateOf(0)
        private set

    protected fun updateFields(fields: List<ArgumentField>) {
        currentFields = fields
        publish()
    }

    override fun onInitialize() = publish()

    private fun publish() = journal.state(instanceId, "${currentFields.joinToString { "${it.type}=${it.value}" }} · 计数 $counter")

    @Composable
    override fun ScreenContent() {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Actions {
                Badge("当前真实页面：$displayName $instanceId")
                Badge("resume $resumeCount 次 · 计数 $counter")
            }
            Filter(size = 6, selectedProvider = { scope.arity == it }, titleProvider = { "$it 个参数" },
                onClick = { index, _ -> scope.arity = index })
            Actions {
                Badge("请求数据：第 ${scope.revision} 轮")
                SecondaryButton("下一轮数据") { scope.revision = (scope.revision + 1) % 10_000 }
                CheckBox(scope.useNull, { scope.useNull = it }, "String? 传 null")
            }
            Actions {
                PrimaryButton("New") { scope.send(this@ArgumentPage, CreatePolicy.New) }
                SecondaryButton("Resume") { scope.send(this@ArgumentPage, CreatePolicy.Resume) }
                TextButton("Move · 忽略本次值") { scope.send(this@ArgumentPage, CreatePolicy.Move) }
                TextButton("Replace") { scope.send(this@ArgumentPage, CreatePolicy.Replace) }
                TextButton("计数 +1") { counter++; publish() }
                TextButton("pop()") {
                    val result = pop()
                    scope.lastResult = "$displayName $instanceId 发起 pop() = $result。返回后检查另一份实例的参数和计数。"
                    journal.event(scope.name, "pop", scope.lastResult)
                }
                TextButton("重置参数实验") {
                    navigate(scope::root, CreatePolicy.Move + ClearPolicy.Clear)
                    scope.lastResult = "回到参数入口，所有参数页面已出栈。请求设置仍由本实验的工厂作用域保留。"
                }
            }
            Paragraph(scope.lastResult)
            GalleryCard("初始参数 · 本次构造时收到", "Resume 后这一组仍保持原值；它来自构造函数，不读取内部 key。") { FieldRows(initialFields) }
            GalleryCard("当前状态 · 实际 ViewModel", "withResume 解包新请求并更新这里。Move 不触发更新；New 和 Replace 会产生新的编号。") { FieldRows(currentFields) }
            CodeBlock(scope.code())
        }
    }
}

@Composable
private fun FieldRows(fields: List<ArgumentField>) {
    if (fields.isEmpty()) Paragraph("0 个导航参数。仍然可以区分 New 的新实例与 Resume 的回调次数。")
    fields.forEach { field ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(field.type, Modifier.width(65.dp))
            Column(Modifier.weight(1f)) { CodeBlock(field.value) }
        }
    }
}

@Stable
internal class ArgumentRootScreen(scope: ArgumentRoutes) : ArgumentPage(scope, "参数入口", [])

@Stable
internal class Argument0Screen(scope: ArgumentRoutes) : ArgumentPage(scope, "Args0", []) {
    override fun onResume() = withResume { updateFields([]) }
}

@Stable
internal class Argument1Screen(scope: ArgumentRoutes, value: Int) : ArgumentPage(scope, "Args1", fields(value)) {
    override fun onResume() = withResume { value: Int -> updateFields(fields(value)) }
}

@Stable
internal class Argument2Screen(scope: ArgumentRoutes, value: Int, text: String?) : ArgumentPage(scope, "Args2", fields(value, text)) {
    override fun onResume() = withResume { value: Int, text: String? -> updateFields(fields(value, text)) }
}

@Stable
internal class Argument3Screen(scope: ArgumentRoutes, value: Int, text: String?, mode: ArgumentMode) : ArgumentPage(scope, "Args3", fields(value, text, mode)) {
    override fun onResume() = withResume { value: Int, text: String?, mode: ArgumentMode -> updateFields(fields(value, text, mode)) }
}

@Stable
internal class Argument4Screen(scope: ArgumentRoutes, value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload) : ArgumentPage(scope, "Args4", fields(value, text, mode, payload)) {
    override fun onResume() = withResume { value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload -> updateFields(fields(value, text, mode, payload)) }
}

@Stable
internal class Argument5Screen(scope: ArgumentRoutes, value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload, sequence: Long) : ArgumentPage(scope, "Args5", fields(value, text, mode, payload, sequence)) {
    override fun onResume() = withResume { value: Int, text: String?, mode: ArgumentMode, payload: DemoPayload, sequence: Long ->
        updateFields(fields(value, text, mode, payload, sequence))
    }
}
