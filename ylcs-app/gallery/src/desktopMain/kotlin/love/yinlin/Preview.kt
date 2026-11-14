package love.yinlin

object Preview {
    fun inject() {
        val selector = System.getenv("GALLERY_PREVIEW")?.takeIf { it.isNotBlank() } ?: return
        val target = selector.split(':', limit = 2)
        require(target.size == 2) { "Invalid GALLERY_PREVIEW: $selector" }
        val page = GalleryCatalog.pageById[target[0]] ?: error("Unknown Gallery page: ${target[0]}")
        val component = page.components.firstOrNull { it.title == target[1] || it.previewAlias == target[1] } ?: error("Unknown Gallery component: $selector")
        currentPage = page
        galleryPreviewComponent = component
    }
}
