package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable

@Stable
enum class RichArg(val value: String) {
    Type("t"),
    Member("m"),
    Uri("u"),
    Text("tx"),
    Width("w"),
    Height("h"),
    FontSize("s"),
    Color("c"),
    Bold("b"),
    Italic("i"),
    Underline("u"),
    Strikethrough("d");
}