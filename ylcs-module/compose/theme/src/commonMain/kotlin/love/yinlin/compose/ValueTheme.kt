package love.yinlin.compose

import androidx.compose.runtime.Stable

@Stable
data class ValueTheme(
    val lightThemeText: String,
    val darkThemeText: String,
    val systemThemeText: String,

    val statefulBoxDefaultEmptyText: String,
    val statefulBoxDefaultNetworkErrorText: String,
    val statefulBoxDefaultLoadingText: String,

    val dialogYesText: String,
    val dialogNoText: String,
    val dialogOkText: String,
    val dialogCancelText: String,
    val dialogInfoTitle: String,
    val dialogConfirmTitle: String,
    val dialogInputTitle: String,
    val dialogLoadingText: String,

    val noContent404Text: String,
    val backText: String,

    val windowMinimizeText: String,
    val windowMaximizeText: String,
    val windowMaximizeBackText: String,
    val windowCloseText: String,
    val windowAlwaysTopEnableText: String,
    val windowAlwaysTopDisableText: String,
) {
    companion object {
        /**
         * 空值的形式表述, 在Composable作用域内默认值会被替换成 LocalValueTheme 中的值
         */
        inline fun <reified T> runtime(): T? = null

        val Default = ValueTheme(
            lightThemeText = "浅色",
            darkThemeText = "深色",
            systemThemeText = "系统",

            statefulBoxDefaultEmptyText = "荒草覆没的古井枯塘...",
            statefulBoxDefaultNetworkErrorText = "如果来生太远寄不到诺言...",
            statefulBoxDefaultLoadingText = "从惊蛰一路走到霜降...",

            dialogYesText = "是",
            dialogNoText = "否",
            dialogOkText = "确定",
            dialogCancelText = "取消",
            dialogInfoTitle = "提示",
            dialogConfirmTitle = "注意",
            dialogInputTitle = "输入",
            dialogLoadingText = "请耐心等待",

            noContent404Text = "404",
            backText = "返回",

            windowMinimizeText = "最小化",
            windowMaximizeText = "最大化",
            windowMaximizeBackText = "还原",
            windowCloseText = "关闭",
            windowAlwaysTopEnableText = "窗口置顶",
            windowAlwaysTopDisableText = "取消窗口置顶",
        )
    }
}