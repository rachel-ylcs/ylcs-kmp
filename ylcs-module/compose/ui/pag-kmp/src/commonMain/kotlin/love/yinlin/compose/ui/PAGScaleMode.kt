package love.yinlin.compose.ui

enum class PAGScaleMode {
    None, Stretch, LetterBox, Zoom;

    companion object {
        fun fromInt(value: Int): PAGScaleMode = when (value) {
            Stretch.ordinal -> Stretch
            LetterBox.ordinal -> LetterBox
            Zoom.ordinal -> Zoom
            else -> None
        }
    }
}