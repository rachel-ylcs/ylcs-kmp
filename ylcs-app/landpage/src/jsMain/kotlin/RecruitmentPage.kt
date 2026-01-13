import kotlinx.html.IframeSandbox
import kotlinx.html.TagConsumer
import kotlinx.html.iframe
import org.w3c.dom.HTMLElement

object RecruitmentPage : Page {
    override fun onCreate() {

    }

    override fun onDestroy() {

    }

    override fun TagConsumer<HTMLElement>.render() {
        iframe(sandbox = IframeSandbox.allowScripts, classes = "flex-grow w-full h-full border-none") {
            width = "100%"
            height = "100%"
            attributes["frameborder"] = "0"
            src = "recruitment.html"
        }
    }
}