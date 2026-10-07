package love.yinlin.gallery.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.screen.ClearPolicy
import love.yinlin.compose.screen.CreatePolicy
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.TextButton

@Stable
internal class OverviewScreen(routes: GalleryRoutes) : GalleryScreen(routes, GallerySection.Overview) {
    private var popResult by mutableStateOf("最后一个页面会被保留，pop() 应返回 false。")

    @Composable
    override fun PageContent() {
        PageHeading("00", "让每一次导航都看得见", "一套可以亲手操作的 Screen 演示。改变策略、编辑页面状态，再通过实例编号和真实回调确认发生了什么。")
        GalleryCard("先体验一条完整的路径", "进入策略实验 → 创建示例栈 → 尝试 Move 与 Resume → 逐次返回 → 查看清理记录。") {
            Actions {
                PrimaryButton("开始策略实验") { navigate(routes::policies) }
                SecondaryButton("查看状态与生命周期") { navigate(routes::state) }
            }
            Actions {
                Metric("导航策略", "4 × 2")
                Metric("类型安全参数", "0–5 个")
                Metric("已构造 / 已清理", "${journal.createdCount} / ${journal.clearedCount}")
            }
        }
        GalleryCard("选择一个实验", "每个实验都有操作入口、预期行为、实际状态和可复制的代码说明。策略与参数实验使用自己的嵌套导航，随时可以重置。") {
            Actions {
                SecondaryButton("01 · 导航策略") { navigate(routes::policies) }
                SecondaryButton("02 · 类型安全参数") { navigate(routes::arguments) }
                SecondaryButton("03 · 状态与生命周期") { navigate(routes::state) }
                SecondaryButton("04 · 嵌套与隔离") { navigate(routes::nested) }
                SecondaryButton("05 · 工厂与边界") { navigate(routes::factories) }
                SecondaryButton("06 · 观察记录") { navigate(routes::records) }
            }
        }
        GalleryCard("最小接入方式", "主页面注册为无参工厂。导航请求提供页面类型与可序列化参数，实际页面由注册工厂构造。") {
            CodeBlock("""
                @Composable
                fun App() {
                    ScreenManager.Navigation<Main> {
                        screen(::Main)
                        screen(::Detail)
                    }
                }

                class Main : ScreenModel() {
                    @Composable
                    override fun ModelContent() {
                        PrimaryButton("打开详情") {
                            navigate(::Detail, 42)
                        }
                    }
                }

                class Detail(private val itemId: Int) : ScreenModel() {
                    @Composable
                    override fun ModelContent() {
                        TextButton("返回详情 " + itemId + " 的上一页") { pop() }
                    }
                }
            """)
        }
        GalleryCard("观察时先区分三个事件") {
            Paragraph("构造 / initialize：出现新的 Screen ViewModel；每份实例只初始化一次。")
            Paragraph("进入 / 离开组合：UI 开始或停止呈现，覆盖页面可能让它离开组合，但它仍然留在导航栈里。")
            Paragraph("uninitialize：ViewModel 真正清理；协程作用域取消，页面自己的业务资源在这里释放。退出动画可能延后这一时刻。")
            Paragraph("这里的 #001 等编号由演示生成，用于辨认对象身份。页面只记录回调与状态快照，没有读取 Manager 的内部栈，也没有保存 Screen 的全局引用。")
        }
        GalleryCard("验证范围", "此 Gallery 覆盖当前公开的显示、导航、参数、状态、协程和嵌套行为。Android 配置重建与进程结束不在此演示中模拟。") {
            Paragraph("可以先在 Desktop 连续操作，再把同一份 commonMain 界面交给 Web 访问者体验。颜色切换只触发重组，不代表 Activity 重建。")
            TextButton("尝试根页面 pop()") {
                popResult = "pop() = ${pop()} · 最后一个页面保留"
                journal.event("Gallery", "根页面返回", popResult)
            }
            Paragraph(popResult)
            TextButton("回到本页并清理上方页面") {
                navigate(routes::overview, CreatePolicy.Move + ClearPolicy.Clear)
            }
        }
    }
}
