import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

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