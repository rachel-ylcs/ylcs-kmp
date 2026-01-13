import kotlinx.html.TagConsumer
import org.w3c.dom.HTMLElement

interface Page {
    fun onCreate()
    fun onDestroy()
    fun TagConsumer<HTMLElement>.render()
}