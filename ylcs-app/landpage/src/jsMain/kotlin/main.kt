import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

var currentPage: Page? = null

fun navigate(page: Page) {
    document.getElementById("root")?.let { root ->
        currentPage?.onDestroy()
        root.innerHTML = ""
        currentPage = page
        page.onCreate()
        root.append {
            with(page) { render() }
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

fun TagConsumer<HTMLElement>.renderHeaderMenu() {
    a(href = DOWNLOAD_URL, target = ATarget.blank, classes = Styles.link) { +"客户端下载" }
    a(href = WEBAPP_URL, classes = Styles.link) { +"网页版" }
    button(classes = Styles.link) {
        +"使用教程"
        onClickFunction = { navigate(DocPage) }
    }
    a(href = GITHUB_URL, target = ATarget.blank, classes = Styles.link) { +"Github开源" }
    button(classes = Styles.link) {
        +"茶舍招新"
        onClickFunction = { navigate(RecruitmentPage) }
    }
}

fun TagConsumer<HTMLElement>.renderHeader() {
    nav("sticky top-0 z-[100] backdrop-blur-xl bg-transparent border-b border-gray-300 dark:border-gray-800 transition-colors duration-300") {
        div("mx-5 md:mx-20") {
            div("flex justify-between items-center h-16") {
                div("flex items-center") {
                    button(classes = "md:hidden mr-5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors") {
                        id = "mobile-menu-btn"
                        i("fas fa-bars text-lg")
                    }
                    button(classes = "flex items-center md:mr-10 space-x-4") {
                        onClickFunction = { navigate(MainPage) }
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

fun TagConsumer<HTMLElement>.renderFooter() {
    footer("bg-transparent font-sans text-gray-500 dark:text-gray-400") {
        div("mx-5 md:mx-20 my-6") {
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

fun main() {
    window.onload = {
        setupTailwindcss()
        document.documentElement?.className = "w-full min-h-full m-0 p-0 dark"
        document.body?.apply {
            className = "w-full min-h-full m-0 p-0 bg-gradient-to-br from-green-50 to-white dark:from-[#0f1715] dark:to-[#0a0a0a] transition-colors duration-500"
            append {
                div("min-h-screen flex flex-col text-gray-900 dark:text-gray-100 text-sm font-sans transition-colors duration-300") {
                    renderHeader()
                    div("flex flex-col flex-grow") { id = "root" }
                    renderFooter()
                }
                div("absolute top-0 right-0 w-1/2 h-full opacity-10 dark:opacity-5 pointer-events-none") {
                    style = "background: radial-gradient(circle at 70% 30%, rgba(34, 197, 94, 0.3) 0%, rgba(0,0,0,0) 70%);"
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
            if (!mobileMenu.classList.contains("hidden")) mobileMenu.classList.add("hidden")
        }

        navigate(MainPage)
    }
}