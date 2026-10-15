package love.yinlin.foundation.fts

internal object EmptyCursor : DocumentCursor {
    override val estimate: Int = 0
    override val current: Int = DocumentCursor.END_DOCUMENT
    override fun next(): Int = DocumentCursor.END_DOCUMENT
    override fun advanceTo(target: Int): Int = DocumentCursor.END_DOCUMENT
}