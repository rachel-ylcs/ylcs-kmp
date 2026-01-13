import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.js.onClickFunction
import love.yinlin.extension.DateEx
import org.w3c.dom.HTMLElement

object MainPage : Page {
    override fun onCreate() {
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
                phone.style.transform = "translateY(${wave - lift}px)"
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

    override fun onDestroy() {
        window.onscroll = {}
    }

    private fun TagConsumer<HTMLElement>.renderSection0() {
        section(Styles.section) {
            id = "section-0-container"
            div {
                div("flex flex-col md:flex-row items-center justify-between gap-6 md:gap-12") {
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

    private fun TagConsumer<HTMLElement>.renderSection1() {
        section(Styles.section) {
            id = "section-1-container"
            div("space-y-6") {
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

    private fun TagConsumer<HTMLElement>.renderSection2() {
        section(Styles.section) {
            id = "section-2-container"
            div( "flex flex-col md:flex-row w-full gap-4 md:gap-10 items-center") {
                div("w-full md:w-1/4 space-y-6 flex flex-col justify-center") {
                    id = "text-box-2"
                    h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") { +"无损音质听歌" }
                    p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"曲库包含银临歌曲集中200多首最高音质曲目，搭配动态播放器畅享音乐人生。" }
                }
                div("w-full md:w-3/4 flex items-center justify-center") {
                    div("w-full aspect-[2/1] bg-zinc-200 dark:bg-zinc-800 rounded-[2rem] md:rounded-[3rem] overflow-hidden shadow-2xl transition-all duration-75") {
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

    private fun TagConsumer<HTMLElement>.renderSection3() {
        section(Styles.section) {
            id = "section-3-container"
            div( "flex flex-col md:flex-row w-full gap-4 md:gap-10 items-start") {
                div("w-full md:w-1/4 space-y-6 flex flex-col justify-center") {
                    h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") { +"银临最新资讯" }
                    p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"实时同步微博、超话、抖音等信息流，无需登录也能浏览和下载" }
                }
                div("relative w-full md:w-3/4 h-[350px] md:h-[800px] flex items-center justify-center") {
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

    private fun TagConsumer<HTMLElement>.renderSection4() {
        section(Styles.section) {
            id = "section-4-container"
            div("flex flex-col items-center") {
                div("w-full text-start z-20 space-y-6 mb-10 md:mb-0") {
                    h2("text-4xl md:text-5xl font-black text-gray-900 dark:text-emerald-400") {
                        +"演出活动抢先知"
                    }
                    p("text-lg text-gray-500 dark:text-gray-400 leading-relaxed") { +"银临演出消息灵通，社区丰富氛围轻松" }
                }
                div("relative w-full h-[500px] md:h-[800px] flex justify-center items-start perspective-container") {
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

    private fun TagConsumer<HTMLElement>.renderSection5() {
        section(Styles.section) {
            id = "section-5-container"
            div( "flex flex-col md:flex-row items-start") {
                div("w-full md:w-1/4 space-y-6 flex flex-col justify-center") {
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
                div("w-full md:w-3/4 relative w-full h-[350px] md:h-[800px] flex items-center justify-center overflow-visible mt-10") {
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

    private fun TagConsumer<HTMLElement>.renderSection6() {
        section(Styles.section) {
            id = "section-6-container"
            div("flex flex-col md:flex-row items-center justify-between space-x-0 lg:space-x-12 z-10") {
                div("w-full lg:w-[30%] relative") {
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
                div("relative w-full aspect-[16/9] mb-10 md:mb-0 flex items-center justify-center") {
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
            }
        }
    }

    override fun TagConsumer<HTMLElement>.render() {
        renderSection0()
        renderSection1()
        renderSection2()
        renderSection3()
        renderSection4()
        renderSection5()
        renderSection6()
    }
}