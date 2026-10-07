package love.yinlin.gallery.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.screen.ScreenModel
import love.yinlin.compose.screen.ScreenRegistry
import kotlin.time.TimeSource

@Immutable
internal data class GalleryEvent(val number: Int, val elapsed: String, val scope: String, val kind: String, val message: String)

@Immutable
internal data class InstanceRecord(
    val id: String,
    val scope: String,
    val name: String,
    val initial: String,
    val initialized: Boolean = false,
    val cleared: Boolean = false,
    val compositions: Int = 0,
    val resumes: Int = 0,
    val state: String = "等待 initialize",
)

/** 只记录文本和数值快照，不持有 Screen、Manager、资源或协程。 */
@Stable
internal class GalleryJournal {
    private val start = TimeSource.Monotonic.markNow()
    private var eventSequence = 0
    private var instanceSequence = 0
    val events = mutableStateListOf<GalleryEvent>()
    val instances = mutableStateListOf<InstanceRecord>()
    var createdCount by mutableStateOf(0)
        private set
    var clearedCount by mutableStateOf(0)
        private set

    fun event(scope: String, kind: String, message: String) {
        val milliseconds = start.elapsedNow().inWholeMilliseconds
        val elapsed = "${milliseconds / 1000}.${(milliseconds % 1000).toString().padStart(3, '0')}s"
        events.add(GalleryEvent(++eventSequence, elapsed, scope, kind, message))
        if (events.size > 160) events.removeAt(0)
    }

    fun create(scope: String, name: String, initial: String): String {
        val id = "#${(++instanceSequence).toString().padStart(3, '0')}"
        createdCount++
        instances.add(InstanceRecord(id, scope, name, initial))
        event(scope, "构造", "$name $id · $initial")
        return id
    }

    private fun update(id: String, transform: (InstanceRecord) -> InstanceRecord) {
        val index = instances.indexOfFirst { it.id == id }
        if (index != -1) {
            val previous = instances[index]
            val next = transform(previous)
            if (next != previous) instances[index] = next
        }
    }

    fun initialize(id: String) {
        update(id) { it.copy(initialized = true, state = "已初始化") }
        instances.find { it.id == id }?.let { event(it.scope, "initialize", "${it.name} $id · 只调用一次") }
    }

    fun resume(id: String) {
        update(id) { it.copy(resumes = it.resumes + 1) }
        instances.find { it.id == id }?.let { event(it.scope, "resume", "${it.name} $id · 第 ${it.resumes} 次") }
    }

    fun composition(id: String, enter: Boolean) {
        update(id) { it.copy(compositions = (it.compositions + if (enter) 1 else -1).coerceAtLeast(0)) }
        instances.find { it.id == id }?.let {
            event(it.scope, if (enter) "进入组合" else "离开组合", "${it.name} $id")
        }
    }

    fun state(id: String, summary: String) = update(id) { it.copy(state = summary) }

    fun clear(id: String) {
        update(id) { it.copy(cleared = true) }
        clearedCount++
        instances.find { it.id == id }?.let { event(it.scope, "uninitialize", "${it.name} $id · ViewModel 已清理") }
        // 保留全部存活实例以及有限的已清理历史，避免网页演示无限积累记录。
        while (instances.count { it.cleared } > 40) instances.removeAt(instances.indexOfFirst { it.cleared })
    }
}

@Stable
internal abstract class ObservedScreen(
    protected val journal: GalleryJournal,
    scopeName: String,
    val displayName: String,
    initial: String = "无导航参数",
) : ScreenModel() {
    val instanceId = journal.create(scopeName, displayName, initial)
    var resumeCount by mutableStateOf(0)
        private set

    final override fun initialize() {
        journal.initialize(instanceId)
        onInitialize()
    }

    final override fun resume() {
        onResume()
        resumeCount++
        journal.resume(instanceId)
    }

    final override fun uninitialize() {
        try { onUninitialize() }
        finally { journal.clear(instanceId) }
    }

    protected open fun onInitialize() { }
    protected open fun onResume() = withResume { }
    protected open fun onUninitialize() { }

    @Composable
    final override fun ModelContent() {
        // 离开组合仅记录 UI 事件；业务资源仍由 uninitialize 释放。
        DisposableEffect(instanceId) {
            journal.composition(instanceId, true)
            onDispose { journal.composition(instanceId, false) }
        }
        ScreenContent()
    }

    @Composable
    protected abstract fun ScreenContent()
}

internal enum class GallerySection(val title: String, val caption: String) {
    Overview("概览", "从这里开始"),
    Policies("导航策略", "New / Replace / Move / Resume"),
    Arguments("类型安全参数", "0–5 个参数与 Resume"),
    State("状态与生命周期", "ViewModel · 组合 · 协程"),
    Nested("嵌套与隔离", "两个独立导航区域"),
    Factories("工厂与边界", "注册、字符串 key 与失败请求"),
    Journal("观察记录", "真实回调与实例快照"),
}

/** 依赖由工厂闭包注入；不会把 journal 等非序列化对象当作导航参数。 */
@Stable
internal class GalleryRoutes {
    val journal = GalleryJournal()
    var darkMode by mutableStateOf(false)
    var factoryCalls by mutableStateOf(0)
    var markerCalls by mutableStateOf(0)
    var rejectedFactoryCalls by mutableStateOf(0)

    fun overview() = OverviewScreen(this)
    fun policies() = PolicyLabScreen(this)
    fun arguments() = ArgumentLabScreen(this)
    fun state() = StateScreen(this)
    fun nested() = NestedLabScreen(this)
    fun factories() = FactoryLabScreen(this)
    fun records() = JournalScreen(this)
    fun cover(message: String) = CoverScreen(this, message)
    fun factoryProbe(value: Int): FactoryProbeScreen {
        factoryCalls++
        return FactoryProbeScreen(this, value)
    }
    fun rejectedProbe(): RejectedProbeScreen {
        rejectedFactoryCalls++
        error("演示工厂主动拒绝构造；没有返回 Screen 实例。")
    }
}

internal fun ScreenRegistry.Builder.registerGallery(routes: GalleryRoutes) {
    routes.journal.event("Gallery", "注册", "主导航注册工厂 · 主页面工厂接收 0 个导航参数")
    screen(routes::overview)
    screen(routes::policies)
    screen(routes::arguments)
    screen(routes::state)
    screen(routes::nested)
    screen(routes::factories)
    screen(routes::records)
    screen(routes::cover, key = "gallery.cover")
    screen(routes::factoryProbe, key = "gallery.probe")
    screen(routes::rejectedProbe)
}
