package love.yinlin

object Preview {
    fun inject() {
        val selector = System.getenv("GALLERY_PREVIEW")?.takeIf { it.isNotBlank() } ?: return
        val target = selector.split(':', limit = 2)
        require(target.size == 2) { "Invalid GALLERY_PREVIEW: $selector" }
        (val page, val components) = PageItem.entries.firstOrNull { it.id == target[0] } ?: error("Unknown Gallery page: ${target[0]}")
        require(components.any { it.title == target[1] }) { "Unknown Gallery component: $selector" }
        currentPage = page
        galleryPreviewComponentTitle = target[1]
    }
}