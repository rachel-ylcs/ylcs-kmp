package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable

@Stable
enum class RichType(val value: String) {
    Root("r"),
    Text("t"),
    Emoji("em"),
    Br("br"),
    Image("img"),
    Link("lk"),
    Topic("tp"),
    At("at"),
    Style("s");
}