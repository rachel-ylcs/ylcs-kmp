package love.yinlin.gallery.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compose.screen.ClearPolicy
import love.yinlin.compose.screen.CreatePolicy
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.screen.ScreenModel
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.extension.catchingError
import love.yinlin.reflect.metaClassName

@Stable
@OptIn(CompatibleRachelApi::class)
internal class FactoryLabScreen(routes: GalleryRoutes) : GalleryScreen(routes, GallerySection.Factories) {
    private var result by mutableStateOf("尚未执行失败请求。下面三个案例会捕获预期异常，并把原始信息留在这里。")

    private fun attempt(label: String, action: () -> Unit) {
        val error = catchingError(action)
        result = if (error == null) "$label：意外成功，请检查演示注册与输入。"
        else "$label：${error::class.simpleName}\n${error.message}"
        journal.event("工厂实验", if (error == null) "意外成功" else "预期失败", result)
    }

    @Composable
    override fun PageContent() {
        PageHeading("05", "注册工厂与导航构造式", "导航里的构造式只参与类型推导。实际实例来自注册工厂；依赖可以由工厂闭包提供，不必塞进可序列化的导航参数。")
        GalleryCard("证明哪个构造式被执行", "成功请求传 Int。导航构造式内放置了计数和 error；如果它被执行，请求就会失败。正确行为是只执行注册工厂。") {
            Actions {
                Metric("注册工厂执行", routes.factoryCalls.toString())
                Metric("导航构造式执行", routes.markerCalls.toString())
                Metric("拒绝构造工厂", routes.rejectedFactoryCalls.toString())
            }
            PrimaryButton("导航到实际工厂创建的页面") {
                journal.event("工厂实验", "请求", "导航构造式不会执行，注册工厂应创建 Probe(42)")
                navigate<FactoryProbeScreen, Int>({ _ ->
                    routes.markerCalls++
                    error("这个导航构造式不应该被调用。")
                }, 42)
            }
            CodeBlock("""
                // routes 持有演示依赖；只把 value 当作导航参数。
                screen(routes::factoryProbe)

                // 该 lambda 只提供返回类型与参数类型，不执行。
                navigate<FactoryProbeScreen, Int>({ _ ->
                    error("不会调用")
                }, 42)
            """)
            Paragraph("主页面也遵循同样规则：它的注册工厂必须接收 0 个导航参数。内部构造依赖可通过闭包注入。工厂表在 Manager 首次创建时建立，不随普通重组反复注册。")
        }
        GalleryCard("通过字符串 key 导航", "注册时显式绑定 gallery.probe，调用方只需提供字符串和参数，就能打开 Probe 页面。") {
            Actions {
                PrimaryButton("按 key 打开 Probe(42)") {
                    journal.event("工厂实验", "字符串导航", "gallery.probe → Probe(42)，应只执行注册工厂。")
                    navigate("gallery.probe", 42)
                }
                SecondaryButton("未绑定 key → 404") {
                    navigate("gallery.missing", 42)
                }
                SecondaryButton("类名作为未绑定 key → 404") {
                    navigate(metaClassName<FactoryProbeScreen>(), 42)
                }
            }
            CodeBlock("""
                screen(routes::factoryProbe, key = "gallery.probe")

                navigate("gallery.probe", 42)
                navigate("gallery.probe", 99,
                    CreatePolicy.Resume + ClearPolicy.None)
            """)
            Paragraph("字符串导航只查显式绑定的 key；注册类名不会自动成为 key。后两个按钮都应显示 404，即使 Probe 类已经注册。key 按原字符串精确匹配，重复绑定会报错。")
            Paragraph("成功绑定后，字符串导航和构造式导航指向同一种页面。打开 Probe 后，可以混用两种方式测试 Move 与 Resume：实例编号保留，Resume 更新状态，注册工厂执行次数不增加。字符串调用方需要与注册模块约定参数数量和类型。")
        }
        GalleryCard("失败请求，保持可操作", "这些失败发生在发布导航栈之前。执行后本页应保持原编号；再发起一次成功导航并返回，检查管理器仍然可用。") {
            Actions {
                SecondaryButton("参数数量错误") {
                    attempt("工厂要求 1 个参数，实际传 0 个") { navigate({ routes.factoryProbe(0) }) }
                }
                SecondaryButton("参数类型错误") {
                    attempt("工厂要求 Int，实际传 String") { navigate({ text: String -> routes.factoryProbe(text.length) }, "不是 Int") }
                }
                SecondaryButton("注册工厂抛错") {
                    attempt("实际注册工厂拒绝构造") { navigate(routes::rejectedProbe) }
                }
            }
            CodeBlock(result)
            Paragraph("Int 解析失败和参数数量错误不应增加成功工厂的执行次数。主动拒绝构造只增加拒绝计数。这里用框架 catchingError 展示异常，并保留协程取消的语义。")
            Paragraph("这组实验不覆盖 initialize / resume 内部抛错；这两个回调执行时导航可能已经完成，不能用本组案例推断回调异常会回滚导航。")
        }
        GalleryCard("默认 404", "普通 navigate 请求未注册页面时会进入默认 404。返回后，本实验的计数和状态应保留。下方同时展示未注册主页面的兜底。") {
            PrimaryButton("navigate 到未注册页面") {
                journal.event("工厂实验", "请求", "未注册类型交给默认 404 工厂；导航构造式不会执行。")
                navigate<UnregisteredProbeScreen>({ error("不会调用") })
            }
            ScreenManager.Navigation<UnregisteredProbeScreen>(Modifier.fillMaxWidth().height(220.dp)) { }
            Paragraph("404 使用原导航条目的 UUID，并由对应 Manager 保存和清理。按钮进入的 404 可以返回本实验；下方独立区域只有根条目，因此返回按钮会保留它。")
        }
        GalleryCard("自定义 404 · 无参注册", "另一个导航使用相同的未注册主页面类型，但设置了自己的无参 404 工厂。两份导航各自管理实例。") {
            ScreenManager.Navigation<UnregisteredProbeScreen>(Modifier.fillMaxWidth().height(350.dp)) {
                screen404(::Gallery404Screen)
            }
            PrimaryButton("覆盖后返回，检查 404 计数") {
                navigate(routes::cover, "工厂实验及两个 404 导航被覆盖。返回后，自定义 404 的计数应保留；默认和自定义导航仍各自显示自己的页面。")
            }
            CodeBlock("""
                ScreenManager.Navigation<Main> {
                    screen(::Main)
                    screen(::Detail)
                    screen404(::My404) // 只接收无参工厂
                }
                // 未设置 screen404 时使用 Screen404。
            """)
            Paragraph("主页面的注册工厂一旦存在，参数错误或工厂抛错仍会报告异常；404 只处理缺失页面，不隐藏已注册页面的错误。")
            Paragraph("这两个 404 区域直接使用 ScreenModel；它们的状态在区域内查看，不计入演示日志中的 ObservedScreen 实例统计。")
        }
    }
}

@Stable
internal class FactoryProbeScreen(routes: GalleryRoutes, private val value: Int) :
    GalleryScreen(routes, GallerySection.Factories, "Factory Probe", "value=$value") {
    private var currentValue by mutableStateOf(value)
    override fun onInitialize() = journal.state(instanceId, "注册工厂接收到 value=$value")
    override fun onResume() = withResume { next: Int ->
        currentValue = next
        journal.state(instanceId, "resume 收到 value=$next")
    }

    @Composable
    override fun PageContent() {
        PageHeading("05 / PROBE", "实例来自注册工厂", "此页面真实收到的构造参数为 $value。编号 $instanceId 来自这份新 ViewModel。")
        GalleryCard("查看计数，确认结果") {
            Actions {
                Metric("构造参数", value.toString())
                Metric("当前状态", currentValue.toString())
                Metric("注册工厂执行", routes.factoryCalls.toString())
                Metric("导航构造式执行", routes.markerCalls.toString())
            }
            Paragraph("导航构造式执行次数应始终为 0。New 每次让注册工厂创建新实例；Move / Resume 命中现有实例时不再构造。")
            Actions {
                PrimaryButton("返回工厂实验") { pop() }
                SecondaryButton("再次 New 一个 Probe") { navigate(routes::factoryProbe, currentValue + 1) }
                SecondaryButton("Move 本实例") { navigate(routes::factoryProbe, 999, CreatePolicy.Move + ClearPolicy.None) }
                SecondaryButton("Resume 本实例") { navigate(routes::factoryProbe, 999, CreatePolicy.Resume + ClearPolicy.None) }
            }
            Actions {
                SecondaryButton("按 key New 一个 Probe") { navigate("gallery.probe", currentValue + 1) }
                SecondaryButton("按 key Move 本实例") { navigate("gallery.probe", 888, CreatePolicy.Move + ClearPolicy.None) }
                SecondaryButton("按 key Resume 本实例") { navigate("gallery.probe", 888, CreatePolicy.Resume + ClearPolicy.None) }
                SecondaryButton("按 key 覆盖本页") {
                    navigate("gallery.cover", "通过字符串 key 打开覆盖页。返回后，Probe 的实例编号、当前状态和工厂计数应保留。")
                }
            }
        }
    }
}

// 只用于导航的类型推导；未注册类型由 404 兜底，拒绝构造的类型由工厂抛错。
@Stable
internal class UnregisteredProbeScreen : ScreenModel() {
    @Composable
    override fun ModelContent() { }
}

@Stable
internal class RejectedProbeScreen : ScreenModel() {
    @Composable
    override fun ModelContent() { }
}

@Stable
internal class Gallery404Screen : ScreenModel() {
    private var counter by mutableStateOf(0)
    private var initialized by mutableStateOf(false)
    private var resumes by mutableStateOf(0)
    private var result by mutableStateOf("404 也是一份 Screen ViewModel。")

    override fun initialize() { initialized = true }

    override fun resume() = withResume { resumes++ }

    @Composable
    override fun ModelContent() {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Badge("自定义 404 · 无参构造")
            Actions {
                Metric("initialize", if (initialized) "已执行" else "未执行")
                Metric("独立 VM 计数", counter.toString())
                Metric("resume 次数", resumes.toString())
            }
            Actions {
                PrimaryButton("计数 +1") { counter++ }
                SecondaryButton("New 另一份 404") { navigate(::UnregisteredProbeScreen) }
                SecondaryButton("Resume 本实例") { navigate(::UnregisteredProbeScreen, CreatePolicy.Resume + ClearPolicy.None) }
                SecondaryButton("pop()") {
                    val popped = pop()
                    result = "pop() = $popped · ${if (popped) "已返回上一页。" else "最后一个页面保留。"}"
                }
            }
            Paragraph(result)
        }
    }
}
