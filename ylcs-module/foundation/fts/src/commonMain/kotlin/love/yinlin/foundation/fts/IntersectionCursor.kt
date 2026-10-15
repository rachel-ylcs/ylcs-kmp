package love.yinlin.foundation.fts

internal class IntersectionCursor(private val cursors: List<DocumentCursor>) : DocumentCursor {
    private val lead = cursors.minBy { it.estimate }
    override val estimate: Int get() = lead.estimate
    override var current: Int = -1
        private set

    override fun next(): Int = if (current == DocumentCursor.END_DOCUMENT) current else advanceTo(current + 1)

    override fun advanceTo(target: Int): Int {
        if (current >= target) return current
        var document = lead.advanceTo(target)
        restart@ while (document != DocumentCursor.END_DOCUMENT) {
            for (cursor in cursors) {
                if (cursor === lead) continue
                val next = cursor.advanceTo(document)
                if (next != document) {
                    document = lead.advanceTo(next)
                    continue@restart
                }
            }
            break
        }
        current = document
        return current
    }
}