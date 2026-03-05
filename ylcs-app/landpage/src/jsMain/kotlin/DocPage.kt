import kotlinx.html.TagConsumer
import kotlinx.html.js.*
import org.w3c.dom.HTMLElement

object DocPage : Page {
    override fun onCreate() {

    }

    override fun onDestroy() {

    }

    override fun TagConsumer<HTMLElement>.render() {
        main(classes = "px-5 md:px-20 py-8 md:py-16 bg-transparent transition-colors") {
            h1(classes = "text-4xl sm:text-5xl font-extrabold mb-12 border-b border-gray-200 pb-6 tracking-tight") { +"Q&A" }
            div(classes = "space-y-12") {
                for (item in QA) {
                    div {
                        h2(classes = "text-xl sm:text-2xl font-bold mb-3") { +"Q: ${item.question}" }
                        p(classes = "text-lg leading-relaxed") {
                            val text = "A: ${item.answer}"
                            text.split('\n').forEach {
                                +it
                                br()
                            }
                        }
                    }
                }
            }
        }
    }
}