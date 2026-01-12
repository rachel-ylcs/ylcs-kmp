import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import love.yinlin.extension.DateEx
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

// #### 数据
const val DOWNLOAD_URL = "https://pan.baidu.com/s/1Ujv4UeRMXUSbXas-Qy6UKw?pwd=1211"
const val WEBAPP_URL = "https://web.yinlin.love"
const val GITHUB_URL = "https://github.com/rachel-ylcs/ylcs-kmp"
const val DEVELEOPMENT_URL = "https://kotlinlang.org"
const val BEIAN_URL = "https://beian.miit.gov.cn"

enum class ContactInfo(
    val title: String,
    val icon: String,
    val url: String
) {
    Weibo(title = "微博", icon = "fa-brands fa-weibo", url = "https://weibo.com/u/7969517430"),
    Bilibili(title = "B站", icon = "fa-brands fa-bilibili", url = "https://space.bilibili.com/3546773870086322"),
    QQGroup(title = "QQ水群", icon = "fa-brands fa-qq", url = "https://qm.qq.com/q/8sfRdA1XPy");
}

enum class AppPlatformVersion(val title: String, val textColor: String, val bgColor: String) {
    STABLE("Stable", "text-green-500", "bg-green-500/10"),
    ALPHA("Alpha", "text-pink-500", "bg-pink-500/10"),
    BETA("Beta", "text-orange-500", "bg-orange-500/10");
}

enum class AppPlatform(
    val title: String,
    val content: String,
    val iconColor: String,
    val icon: String,
    val label: AppPlatformVersion
) {
    Android("Android", "安卓平台支持度最高，覆盖全部已实现的功能，并且具有10M左右的安装包体积和极致的性能体验。", "from-purple-500 to-blue-500", "fa-brands fa-android", AppPlatformVersion.STABLE),
    IOS("iOS", "苹果端还在早期支持中，基本功能已实现，但安装只能通过自签侧载的方式, 并且可能存在一些隐性问题。", "from-yellow-400 to-green-500", "fa-brands fa-apple", AppPlatformVersion.ALPHA),
    Windows("Windows", "Windows端支持度与安卓相同，除了扫码等手机特有操作外基本完全覆盖，并且具备各类窗口操作的特性。", "from-blue-400 to-cyan-400", "fa-brands fa-windows", AppPlatformVersion.STABLE),
    Linux("Linux", "Linux端支持度除了音视频播放等功能外基本与Windows电脑端一致。", "from-purple-600 to-indigo-600", "fa-brands fa-linux", AppPlatformVersion.BETA),
    MacOS("macOS", "macOS端支持度除了音视频播放等功能外基本与Windows电脑端一致。", "from-rose-500 to-purple-800", "fa-solid fa-apple-whole", AppPlatformVersion.ALPHA),
    PWA("PWA", "PWA网页端除了听歌等文件存储相关的功能外基本已支持，后续将会实现完整的网页PWA应用，首次加载需要等待较长时间。", "from-cyan-500 to-blue-700", "fa-brands fa-edge", AppPlatformVersion.BETA),
    Web("Web", "JS网页端支持程度与PWA相同，加载速度较快，但性能可能略有下降。", "from-amber-500 to-orange-700", "fa-brands fa-internet-explorer", AppPlatformVersion.ALPHA),
    Server("Server", "服务器正在持续运行中。", "from-zinc-600 to-black", "fa-solid fa-server", AppPlatformVersion.STABLE),
}

// #### 样式

@Suppress("ConstPropertyName")
object Styles {
    const val primaryGreen = "text-[#16a34a]"
    const val primaryGradient = "bg-clip-text text-transparent bg-gradient-to-r from-[#22c55e] via-[#06b6d4] to-[rgb(253,165,213)]"
    const val container = "mx-0 lg:mx-20 px-4 lg:px-8"
    const val h1 = "text-3xl md:text-5xl lg:text-6xl font-extrabold tracking-tight mb-6 leading-tight"
    const val h2 = "text-2xl md:text-4xl lg:text-5xl font-bold tracking-tight mb-4 leading-tight"
    const val pLarge = "text-lg md:text-xl text-gray-600 dark:text-gray-400 leading-relaxed max-w-2xl"
    const val link = "hover:text-[#22c55e] transition-colors"
    const val btnPrimary = "bg-[#22c55e] text-black font-bold py-3 px-5 lg:px-10 rounded-full hover:bg-green-400 active:scale-95 active:bg-green-600 transition-all transform hover:scale-105"
    const val btnOutline = "border border-gray-300 dark:border-gray-600 hover:border-[#22c55e] hover:text-[#22c55e] active:scale-95 active:bg-[#22c55e]/10 font-medium py-3 px-5 lg:px-10 rounded-full transition-all backdrop-blur-sm"
    const val globalBg = "bg-gradient-to-br from-green-50 to-white dark:from-[#0f1715] dark:to-[#0a0a0a] transition-colors duration-500"
}

// #### 缓动动画
class ScrollAnimationManager(
    private val sectionId: String,
    private val onUpdate: (Double) -> Unit
) {
    fun handleScroll() {
        val element = document.getElementById(sectionId) as? HTMLElement ?: return
        val rect = element.getBoundingClientRect()
        val windowHeight = window.innerHeight.toDouble()
        val startTrigger = windowHeight * 0.8
        val progress = ((startTrigger - rect.top) / (windowHeight + element.clientHeight / 2)).coerceIn(0.0, 1.0)
        onUpdate(progress)
    }
}

// #### UI

fun TagConsumer<HTMLElement>.renderHeaderMenu() {
    a(href = DOWNLOAD_URL, target = ATarget.blank, classes = Styles.link) { +"客户端下载" }
    a(href = WEBAPP_URL, classes = Styles.link) { +"网页版" }
    a(href = "#", classes = Styles.link) { +"使用教程" }
    a(href = GITHUB_URL, target = ATarget.blank, classes = Styles.link) { +"Github开源" }
    a(href = "#", classes = Styles.link) { +"茶舍招新" }
}

fun TagConsumer<HTMLElement>.renderHeader() {
    nav("sticky top-0 z-[100] backdrop-blur-xl bg-transparent border-b border-gray-300 dark:border-gray-800 transition-colors duration-300") {
        div(Styles.container) {
            div("flex justify-between items-center h-16") {
                div("flex items-center space-x-10") {
                    div("flex items-center space-x-2 sm:space-x-5") {
                        button(classes = "md:hidden p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors") {
                            id = "mobile-menu-btn"
                            i("fas fa-bars text-lg")
                        }
                    }
                    a(href = "#", classes = "flex items-center space-x-4") {
                        img(src = "favicon.ico", alt = "logo", classes = "w-auto h-[2em]")
                        span("font-bold text-xl") { +"银临茶舍" }
                    }
                    div("hidden md:flex space-x-10 font-medium") {
                        renderHeaderMenu()
                    }
                }

                div("flex items-center space-x-5") {
                    button(classes = "p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-400 transition-colors") {
                        onClickFunction = {
                            val html = document.documentElement
                            if (html?.classList?.contains("dark") == true) html.classList.remove("dark") else html?.classList?.add("dark")
                        }
                        i("fas fa-moon dark:hidden")
                        i("fas fa-sun hidden dark:block text-yellow-400")
                    }
                }
            }
        }

        div("hidden md:hidden absolute top-16 left-0 w-full bg-white/90 dark:bg-black/90 backdrop-blur-xl border-b border-gray-200 dark:border-gray-800 py-4 px-6 flex flex-col space-y-4 shadow-xl") {
            id = "mobile-menu"
            renderHeaderMenu()
        }
    }
}

fun TagConsumer<HTMLElement>.renderHero() {
    section("relative pt-12 md:pt-24 pb-20 md:pb-32 border-b border-gray-300 dark:border-gray-800 overflow-hidden") {
        div("absolute top-0 right-0 w-1/2 h-full opacity-10 dark:opacity-5 pointer-events-none") {
            style = "background: radial-gradient(circle at 70% 30%, rgba(34, 197, 94, 0.3) 0%, rgba(0,0,0,0) 70%);"
        }

        div(Styles.container) {
            div("flex flex-col md:flex-row items-center justify-between gap-12") {
                div("w-full md:w-1/2 text-left") {
                    h1(Styles.h1) { +"银临茶舍" }
                    h2(Styles.h2) {
                        span(Styles.primaryGradient) { +"APP跨端版本 上线!" }
                    }
                    p("${Styles.pLarge} my-6") {
                        +"集资讯、听歌、美图、社交于一体的小银子聚集地。"
                    }
                    div("flex flex-wrap gap-4") {
                        button(classes = Styles.btnPrimary) {
                            +"客户端下载"
                            onClickFunction = { window.open(DOWNLOAD_URL) }
                        }
                        button(classes = Styles.btnOutline) {
                            +"网页版"
                            onClickFunction = { window.location.href = WEBAPP_URL }
                        }
                    }
                    p("pt-6 text-sm font-medium space-x-4") {
                        span(Styles.primaryGreen) { +"最新版本 v3.4.1" }
                        span("text-gray-500") { +DateEx.TodayString }
                    }
                }

                div("w-full md:w-1/2 relative") {
                    div("relative z-10 rounded-2xl overflow-hidden shadow-2xl transform md:rotate-2 hover:rotate-0 transition-transform duration-500 border border-gray-200 dark:border-gray-700") {
                        img(src = "preview.png", alt = "Preview", classes = "aspect-[16/9] object-cover")
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderSection1() {
    section("py-20 bg-transparent border-b border-gray-300 dark:border-gray-800 transition-colors") {
        div(Styles.container) {
            div("flex justify-between items-end mb-12") {
                div {
                    h2(Styles.h2) { +"支持平台" }
                    p("text-gray-500 text-lg") { +"银临茶舍APP是基于Rachel框架开发的多端跨平台APP，目前基本支持了所有主流平台。" }
                }
                a(href = GITHUB_URL, target = ATarget.blank, classes = "text-[#16a34a] font-semibold hover:underline hidden sm:block") { +"查看Github开源代码 >>" }
            }
            div("grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-6") {
                AppPlatform.entries.forEach { appPlatform ->
                    val label = appPlatform.label
                    div("relative p-4 lg:p-8 rounded-2xl bg-white/30 dark:bg-white/5 backdrop-blur-2xl border border-gray-200 dark:border-gray-800 hover:border-[#16a34a]/50 active:scale-95 transition-all duration-300 group cursor-pointer") {
                        div("absolute top-4 right-4 px-2.5 py-1 rounded-md text-[10px] font-black tracking-widest uppercase ${label.bgColor} ${label.textColor} border border-white/5") {
                            +label.title
                        }
                        div("w-10 h-10 lg:w-16 lg:h-16 mb-6 bg-gradient-to-br ${appPlatform.iconColor} rounded-xl flex items-center justify-center text-white text-2xl shadow-xl shadow-inherit/30 group-hover:scale-110 transition-transform") {
                            i(appPlatform.icon)
                        }
                        h3("text-xl font-bold mb-2 group-hover:${Styles.primaryGreen} transition-colors") { +appPlatform.title }
                        p("text-gray-500 leading-relaxed text-sm") { +appPlatform.content }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderSection2() {
    section("relative py-20 border-b border-gray-300 dark:border-gray-800 transition-colors z-10") {
        id = "scroll-trigger-section-2"
        div(Styles.container + " relative grid grid-cols-1 md:grid-cols-12 gap-12 items-center") {
            div("md:col-span-4 flex flex-col justify-center order-2 md:order-1") {
                id = "text-box-2"
                h2("text-4xl md:text-6xl font-black text-gray-900 dark:text-emerald-400 mb-6 leading-none") {
                    +"无损音质听歌"
                }
                p("text-xl md:text-2xl text-gray-500 dark:text-gray-400 font-bold tracking-wide leading-relaxed") {
                    +"曲库包含银临歌曲集中200多首无损曲目，搭配动态播放器畅享音乐人生。"
                }
            }
            div("md:col-span-8 relative flex items-center justify-center order-1 md:order-2") {
                div("relative w-full aspect-video bg-zinc-200 dark:bg-zinc-800 rounded-[2rem] md:rounded-[3rem] overflow-hidden shadow-2xl transition-all duration-75") {
                    id = "monitor-frame"
                    style = "transform: scale(1.1);"
                    img(src = "section2_1.webp", loading = ImgLoading.lazy, classes = "absolute inset-0 w-full h-full object-cover")
                    div("absolute inset-0 flex items-center justify-center") {
                        div("absolute z-30 h-[90%] w-auto aspect-[9/20] bg-zinc-900 rounded-[1.5rem] md:rounded-[2.5rem] border-[6px] md:border-[10px] border-zinc-950 shadow-[0_50px_100px_rgba(0,0,0,0.6)] overflow-hidden transition-all duration-75") {
                            id = "layer-phone-2"
                            style = "transform: translateX(150vw);"
                            img(src = "section2_2.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                        }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderSection3() {
    section("relative py-20 border-b border-gray-300 dark:border-gray-800 transition-colors z-10") {
        div(Styles.container) {
            div("text-center") {
                h2(Styles.h2) { +"更多功能即将到来" }
                p(Styles.pLarge + " mx-auto") { +"我们致力于为每一个用户提供最好的跨平台开发体验。" }
            }
            div("grid grid-cols-1 md:grid-cols-3 gap-10 mt-24") {
                (1..3).forEach { i ->
                    div("h-80 bg-gray-50 dark:bg-zinc-800/50 rounded-[2rem] border border-gray-100 dark:border-zinc-700 p-10") {
                        div("w-16 h-16 bg-emerald-500/20 text-emerald-500 rounded-2xl flex items-center justify-center mb-8 text-2xl") {
                            i("fas fa-rocket")
                        }
                        h4("text-2xl font-bold mb-4") { +"模块 $i" }
                        p("text-gray-400") { +"占位描述内容，详细功能请关注后续更新。" }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderFooter() {
    footer("bg-transparent pt-12 pb-8 mt-auto font-sans text-gray-500 dark:text-gray-400") {
        div(Styles.container) {
            div("flex items-center text-base space-x-10 mb-8") {
                ContactInfo.entries.forEach { contact ->
                    a(href = contact.url, target = ATarget.blank, classes = "flex text-[#999999] hover:text-black dark:hover:text-white transition-colors items-center gap-x-1") {
                        i(contact.icon)
                        span("uppercase tracking-wider") { +contact.title }
                    }
                }
            }
            div("flex flex-col md:flex-row justify-between items-start md:items-center text-[#999999] space-y-4 md:space-y-0") {
                div("flex flex-wrap gap-x-6 gap-y-2") {
                    span { +"Copyright © 2024-2026 银临茶舍" }
                    a(href = BEIAN_URL, target = ATarget.blank, classes = "hover:text-black dark:hover:text-white transition-colors") { +"皖ICP备2024063749号" }
                }
                div("flex items-center gap-x-1") {
                    +"Developed with"
                    a(href = DEVELEOPMENT_URL, target = ATarget.blank, classes = "font-bold hover:text-black dark:hover:text-white tracking-tighter transition-colors") { +"Kotlin" }
                }
            }
        }
    }
}

private fun setupTailwindcss() = js("""
{
    tailwind.config = {
        darkMode: 'class',
        theme: {
            extend: {
                fontFamily: {
                    sans: ['Inter', 'system-ui', 'sans-serif'],
                },
            }
        }
    };
}
""")

fun main() {
    window.onload = {
        setupTailwindcss()
        document.documentElement?.className = "w-full min-h-full m-0 p-0 dark"
        document.body?.apply {
            className = "w-full min-h-full m-0 p-0 ${Styles.globalBg}"
            append {
                div("min-h-screen flex flex-col text-gray-900 dark:text-gray-100 text-sm font-sans transition-colors duration-300") {
                    renderHeader()
                    renderHero()
                    renderSection1()
                    renderSection2()
                    renderSection3()
                    renderFooter()
                }
            }
        }

        val mobileMenuBtn = document.getElementById("mobile-menu-btn") as HTMLButtonElement
        val mobileMenu = document.getElementById("mobile-menu") as HTMLDivElement

        mobileMenuBtn.onclick = { event ->
            event.stopPropagation()
            mobileMenu.classList.toggle("hidden")
        }

        mobileMenu.onclick = { event ->
            event.stopPropagation()
        }

        window.onclick = {
            if (!mobileMenu.classList.contains("hidden")) {
                mobileMenu.classList.add("hidden")
            }
        }

        val section2Manager = ScrollAnimationManager("scroll-trigger-section-2") { progress ->
            val monitor = document.getElementById("monitor-frame") as HTMLElement
            val phone = document.getElementById("layer-phone-2") as HTMLElement
            val currentScale = 1.1 - (progress * 0.2)
            monitor.style.transform = "scale($currentScale)"
            val phoneX = 150 - (progress * 300)
            phone.style.transform = "translateX(${phoneX.coerceAtLeast(0.0)}vw)"
            phone.style.opacity = "${(progress * 6.0).coerceAtMost(1.0)}"
        }

        window.onscroll = {
            section2Manager.handleScroll()
        }
    }
}