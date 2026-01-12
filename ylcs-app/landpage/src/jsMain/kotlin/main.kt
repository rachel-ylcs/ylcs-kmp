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
        val progress = ((windowHeight - rect.top) / (windowHeight + rect.height)).coerceIn(0.0, 1.0)
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
                        img(src = "favicon.ico", classes = "w-auto h-[2em]")
                        span("font-bold text-xl") { +"银临茶舍" }
                    }
                    div("hidden md:flex space-x-10 font-medium") {
                        renderHeaderMenu()
                    }
                }

                div("flex items-center space-x-5") {
                    button(classes = "p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-400 transition-colors") {
                        onClickFunction = {
                            document.documentElement?.classList?.let {
                                if (it.contains("dark")) it.remove("dark") else it.add("dark")
                            }
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
                    p("${Styles.pLarge} my-6") { +"集资讯、听歌、美图、社交于一体的小银子聚集地。" }
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
                        img(src = "preview.webp", loading = ImgLoading.lazy, classes = "aspect-[16/9] object-cover")
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderSection1() {
    section("py-20 bg-transparent border-b border-gray-300 dark:border-gray-800 transition-colors") {
        div("${Styles.container} space-y-6") {
            h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") { +"支持平台" }
            div("flex justify-between items-end") {
                p("text-gray-500 text-lg") { +"银临茶舍APP是基于Rachel框架开发的多端跨平台APP，目前基本支持了所有主流平台。" }
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
        id = "section-2-container"
        div( "${Styles.container} relative grid grid-cols-1 md:grid-cols-12 gap-12 items-center") {
            div("md:col-span-3 space-y-6 flex flex-col justify-center order-2 md:order-1") {
                id = "text-box-2"
                h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") { +"无损音质听歌" }
                p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"曲库包含银临歌曲集中200多首最高音质曲目，搭配动态播放器畅享音乐人生。" }
            }
            div("md:col-span-9 relative flex items-center justify-center order-1 md:order-2") {
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
    section("relative py-24 border-b border-gray-300 dark:border-gray-800 transition-all duration-1000 opacity-0 transform translate-y-10 z-10") {
        id = "section-3-container"
        div( "${Styles.container} grid grid-cols-1 md:grid-cols-12 items-start") {
            div("md:col-span-3 space-y-6") {
                h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") { +"银临最新资讯" }
                p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"实时同步微博、超话、抖音等信息流，无需登录也能浏览和下载" }
            }
            div("md:col-span-9 relative h-[500px] md:h-[750px] flex items-center justify-center overflow-visible") {
                id = "gallery-stack"
                div("absolute w-[70%] aspect-[12/7] rounded-3xl overflow-hidden shadow-xl border-2 border-white/20 dark:border-zinc-800 transition-transform duration-100 ease-out") {
                    id = "img-layer-1"
                    style = "transform: rotate(-10deg) translateX(-45%) translateY(-10%); z-index: 10;"
                    img(src = "section3_1.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                }
                div("absolute w-[22%] aspect-[9/20] rounded-2xl overflow-hidden shadow-2xl border-2 border-white/20 dark:border-zinc-800 transition-transform duration-100 ease-out") {
                    id = "img-layer-2"
                    style = "transform: rotate(15deg) translateX(-110%) translateY(25%); z-index: 20;"
                    img(src = "section3_2.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                }
                div("absolute w-[45%] aspect-[12/7] rounded-3xl overflow-hidden shadow-2xl border-2 border-white/20 dark:border-zinc-800 transition-transform duration-100 ease-out") {
                    id = "img-layer-3"
                    style = "transform: rotate(8deg) translateX(75%) translateY(-30%); z-index: 15;"
                    img(src = "section3_3.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                }
                div("absolute w-[25%] aspect-[9/20] rounded-2xl overflow-hidden shadow-2xl border-2 border-white/20 dark:border-zinc-800 transition-transform duration-100 ease-out") {
                    id = "img-layer-4"
                    style = "transform: rotate(-12deg) translateX(120%) translateY(15%); z-index: 25;"
                    img(src = "section3_4.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                }
                div("absolute w-[32%] aspect-[9/20] rounded-3xl overflow-hidden shadow-[0_40px_100px_rgba(0,0,0,0.6)] border-4 border-white dark:border-zinc-700 transition-transform duration-100 ease-out") {
                    id = "img-layer-5"
                    style = "transform: rotate(0deg) translateX(20%) translateY(0%); z-index: 30;"
                    img(src = "section3_5.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderSection4() {
    section("relative py-24 border-b border-gray-300 dark:border-gray-800 transition-colors z-10 overflow-hidden") {
        id = "section-4-container"
        div("absolute inset-0 pointer-events-none") {
            div("absolute top-0 left-1/2 -translate-x-1/2 w-full h-[600px] bg-gradient-to-b from-blue-500/5 to-transparent blur-3xl") {}
        }
        div("${Styles.container} flex flex-col items-center") {
            div("text-center relative z-20 space-y-6") {
                h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") {
                    +"演出活动抢先知"
                }
                p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"银临演出消息灵通，社区丰富氛围轻松" }
            }
            div("relative w-full max-w-7xl h-[700px] md:h-[800px] flex justify-center items-start perspective-container -mt-20 -md:mt-30") {
                style = "perspective: 2500px;"
                div("grid grid-cols-3 gap-x-16 gap-y-8 md:gap-x-32 md:gap-y-16 transform-style-3d") {
                    id = "section-4-grid"
                    style = "transform: rotateX(20deg) rotateZ(-8deg) skewY(2deg) translateX(0%);"
                    (1..6).forEach { i ->
                        div("relative w-[180px] h-[380px] md:w-[260px] md:h-[540px] bg-white dark:bg-zinc-900 rounded-[2.5rem] border-[6px] border-gray-200 dark:border-zinc-700 shadow-[0_30px_60px_rgba(0,0,0,0.3)] overflow-hidden transition-all duration-100") {
                            id = "s4-phone-$i"
                            img(src = "section4_$i.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                            div("absolute inset-0 bg-gradient-to-tr from-transparent via-white/10 to-transparent pointer-events-none") {}
                        }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderSection5() {
    section("relative py-32 border-b border-gray-300 dark:border-gray-800 transition-colors z-10 overflow-hidden") {
        id = "section-5-container"
        div( "${Styles.container} grid grid-cols-1 md:grid-cols-12 items-start") {
            div("md:col-span-3 space-y-6 flex flex-col justify-center") {
                h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") { +"银临美图相册" }
                p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"全网演出官摄图集收录，每一张都是绝美壁纸。" }
                div("flex gap-4 pt-4") {
                    div("flex flex-col space-y-2") {
                        span("text-3xl font-black text-emerald-500") { +"1000+" }
                        span("text-lg text-gray-400 uppercase tracking-tighter") { +"4K超清美图" }
                    }
                    div("w-[1px] h-10 bg-gray-200 dark:bg-gray-800")
                    div("flex flex-col space-y-2") {
                        span("text-3xl font-black text-blue-500") { +"Realtime" }
                        span("text-lg text-gray-400 uppercase tracking-tighter") { +"实时更新" }
                    }
                }
            }
            div("md:col-span-9 relative w-full h-[500px] md:h-[850px] flex items-center justify-center overflow-visible mt-10") {
                div("absolute top-0 right-[-5%] w-[85%] md:w-[80%] aspect-[3/2] bg-gray-50 dark:bg-zinc-800 rounded-[1rem] lg:rounded-[3rem] shadow-2xl border border-gray-200 dark:border-zinc-700 overflow-hidden z-10 transition-transform duration-75 ease-out") {
                    id = "s5-card-2"
                    img(src = "section5_2.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover opacity-80 hover:opacity-100 transition-opacity")
                }
                div("absolute bottom-[-5%] left-[-5%] w-[85%] md:w-[80%] aspect-[3/2] bg-white dark:bg-zinc-900 rounded-[1rem] lg:rounded-[3rem] shadow-[0_60px_120px_rgba(0,0,0,0.5)] border border-gray-200 dark:border-zinc-700 overflow-hidden z-20 transition-transform duration-75 ease-out") {
                    id = "s5-card-1"
                    img(src = "section5_1.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.renderSection6() {
    section("relative py-32 border-b border-gray-300 dark:border-gray-800 transition-colors z-10 overflow-hidden") {
        id = "section-6-container"
        div("${Styles.container} flex flex-col lg:flex-row items-center justify-between space-x-0 lg:space-x-12 relative z-10") {
            div("relative w-full lg:w-[60%] h-[400px] lg:h-[600px] flex items-center justify-center") {
                id = "s6-visual-group"
                div("absolute inset-0 bg-slate-100 dark:bg-slate-900/80 border border-gray-300 dark:border-slate-800 rounded-3xl shadow-2xl overflow-hidden transform-gpu") {
                    id = "s6-main-card"
                    div("w-full h-full bg-gradient-to-br from-indigo-500/10 to-purple-500/10 flex items-center justify-center") {
                        img(src = "section6_1.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                    }
                }
                div("absolute -bottom-6 -right-2 lg:-right-8 w-32 h-64 lg:w-48 lg:h-96 bg-black border-4 border-slate-800 rounded-[2.5rem] shadow-2xl overflow-hidden z-20 transform-gpu") {
                    id = "s6-phone-1"
                    div("w-full h-full bg-slate-800 flex items-center justify-center") {
                        img(src = "section6_2.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                    }
                }
                div("absolute -bottom-12 right-12 lg:right-28 w-32 h-64 lg:w-48 lg:h-96 bg-black border-4 border-slate-800 rounded-[2.5rem] shadow-2xl overflow-hidden z-10 transform-gpu") {
                    id = "s6-phone-2"
                    div("w-full h-full bg-slate-800 flex items-center justify-center") {
                        img(src = "section6_3.webp", loading = ImgLoading.lazy, classes = "w-full h-full object-cover")
                    }
                }
            }
            div("w-full lg:w-[30%] relative mt-16 lg:mt-0") {
                id = "s6-text-content"
                div("absolute -top-12 -left-12 w-48 h-48 opacity-10 dark:opacity-20 pointer-events-none") {
                    id = "s6-decor-bg"
                    i("fa-solid fa-gamepad text-[12rem] text-emerald-500")
                }
                div("relative z-20 text-left space-y-6") {
                    h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") { +"主题游戏世界" }
                    p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"各类银临歌曲相关的互动或对抗游戏，享受冲榜的乐趣。在音乐的节奏中体验竞技的快感。" }
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
                    renderSection4()
                    renderSection5()
                    renderSection6()
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

        val section2Manager = ScrollAnimationManager("section-2-container") { progress ->
            val monitor = document.getElementById("monitor-frame") as? HTMLElement ?: return@ScrollAnimationManager
            val phone = document.getElementById("layer-phone-2") as? HTMLElement ?: return@ScrollAnimationManager
            val smoothProgress = (progress * 1.5).coerceIn(0.0, 1.0)
            val currentScale = 1.1 - (smoothProgress * 0.2)
            monitor.style.transform = "scale($currentScale)"
            val phoneX = 150 - (smoothProgress * 300)
            phone.style.transform = "translateX(${phoneX.coerceAtLeast(0.0)}vw)"
            phone.style.opacity = "${(smoothProgress * 6.0).coerceAtMost(1.0)}"
        }

        val section3Manager = ScrollAnimationManager("section-3-container") { progress ->
            val container = document.getElementById("section-3-container") as HTMLElement
            val l1 = document.getElementById("img-layer-1") as HTMLElement
            val l2 = document.getElementById("img-layer-2") as HTMLElement
            val l3 = document.getElementById("img-layer-3") as HTMLElement
            val l4 = document.getElementById("img-layer-4") as HTMLElement
            val l5 = document.getElementById("img-layer-5") as HTMLElement

            val opacity = (progress * 2).coerceIn(0.0, 1.0)
            container.style.opacity = opacity.toString()
            container.style.transform = "translateY(${(1.0 - opacity) * 50}px)"

            val offset = progress * 100
            l1.style.transform = "rotate(${-10 + progress * 2}deg) translateX(${-45 + progress * 5}%) translateY(${offset * 0.3}px)"
            l2.style.transform = "rotate(${15 - progress * 5}deg) translateX(${-110 + progress * 15}%) translateY(${-offset * 0.2}px)"
            l3.style.transform = "rotate(${8 + progress * 2}deg) translateX(${75 - progress * 10}%) translateY(${-offset * 0.5}px)"
            l4.style.transform = "rotate(${-12 + progress * 4}deg) translateX(${120 - progress * 15}%) translateY(${offset * 0.4}px)"
            l5.style.transform = "rotate(${0 - progress * 2}deg) translateX(${20 - progress * 5}%) translateY(${-offset * 0.1}px)"
        }

        val section4Manager = ScrollAnimationManager("section-4-container") { progress ->
            val grid = document.getElementById("section-4-grid") as? HTMLElement ?: return@ScrollAnimationManager
            val containerY = (0.5 - progress) * 100
            grid.style.transform = "rotateX(20deg) rotateZ(-8deg) skewY(2deg) translateX(0%) translateY(${containerY}px)"
            (1..6).forEach { i ->
                val phone = document.getElementById("s4-phone-$i") as? HTMLElement ?: return@forEach
                val wave = kotlin.math.sin(progress * 4.0 + i * 0.8) * 20
                val lift = (1.0 - progress) * 100
                phone.style.transform = "translateY(${wave + lift}px)"
            }
        }

        val section5Manager = ScrollAnimationManager("section-5-container") { progress ->
            val card1 = document.getElementById("s5-card-1") as? HTMLElement ?: return@ScrollAnimationManager
            val card2 = document.getElementById("s5-card-2") as? HTMLElement ?: return@ScrollAnimationManager
            val offset = (progress - 0.5) * 120

            card1.style.transform = "translateY(${-offset}px) translateX(${offset * 0.5}px)"
            card2.style.transform = "translateY(${offset}px) translateX(${-offset * 0.5}px)"
        }

        val section6Manager = ScrollAnimationManager("section-6-container") { progress ->
            val mainCard = document.getElementById("s6-main-card") as? HTMLElement ?: return@ScrollAnimationManager
            val phone1 = document.getElementById("s6-phone-1") as? HTMLElement ?: return@ScrollAnimationManager
            val phone2 = document.getElementById("s6-phone-2") as? HTMLElement ?: return@ScrollAnimationManager
            val textContent = document.getElementById("s6-text-content") as? HTMLElement ?: return@ScrollAnimationManager
            val decorBg = document.getElementById("s6-decor-bg") as? HTMLElement ?: return@ScrollAnimationManager

            val mainOpacity = (progress * 2.0).coerceIn(0.0, 1.0)
            val mainScale = 0.9 + (progress * 0.1).coerceAtMost(0.1)
            val mainTranslate = (1.0 - progress) * 60
            mainCard.style.transform = "scale($mainScale) translateY(${mainTranslate}px)"

            val p1Progress = (progress * 1.5).coerceIn(0.0, 1.0)
            val p1Translate = (1.0 - p1Progress) * 50
            phone1.style.transform = "translateY(${p1Translate}px) rotate(5deg)"

            val p2Progress = (progress * 1.3 - 0.1).coerceIn(0.0, 1.0)
            val p2Translate = (1.0 - p2Progress) * 150
            phone2.style.transform = "translateY(${p2Translate}px) rotate(-3deg)"

            val textAlpha = (progress * 1.8 - 0.4).coerceIn(0.0, 1.0)
            textContent.style.transform = "translateX(${(1.0 - textAlpha) * 40}px)"

            val decorRotate = progress * 180.0
            decorBg.style.transform = "rotate(${decorRotate}deg)"
        }

        window.onscroll = {
            section2Manager.handleScroll()
            section3Manager.handleScroll()
            section4Manager.handleScroll()
            section5Manager.handleScroll()
            section6Manager.handleScroll()
        }
    }
}