package love.yinlin.compose.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.ServerRes
import love.yinlin.cs.url
import love.yinlin.extension.DateEx

@Stable
private object UserLabelMeta {
    private val labelNameFromLevel = arrayOf("BUG",
        "风露婆娑", "剑心琴魄", "梦外篝火", "日暮入旧",
        "烈火胜情爱", "青山撞入怀", "雨久苔如海", "曾吻过秋槐",
        "明雪澄岚", "春风韵尾", "银河万顷", "山川蝴蝶",
        "薄暮忽晚", "沧流彼岸", "清荷玉盏", "风月顽冥",
        "颜如舜华", "逃奔风月", "自在盈缺", "青鸟遁烟",
        "天生妙罗帷", "梦醒般惊蜕", "韶华的结尾", "满袖皆月色"
    )

    fun label(level: Int): String = labelNameFromLevel.getOrNull(level) ?: labelNameFromLevel[0]

    @Suppress("SpellCheckingInspection")
    fun image(level: Int, darkMode: Boolean): String = when (level) {
        in 1..4 -> "fucaoweiying"
        in 5..8 -> "pifuduhai"
        in 9..12 -> "fenghuaxueyue"
        in 13..16 -> "liuli"
        in 17..20 -> if (darkMode) "lidishigongfen2" else "lidishigongfen1"
        in 21..99 -> "shanseyouwuzhong"
        else -> "fucaoweiying"
    }

    fun image(name: String): String = "special"
}

@Composable
fun UserLabel(
    label: String,
    level: Int,
    onClick: (() -> Unit)? = null
) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
        Box(
            modifier = Modifier.size(92.4.dp, 35.2.dp).condition(onClick != null) {
                silentClick(onClick = onClick).pointerIcon(PointerIcon.Hand)
            },
            contentAlignment = Alignment.Center
        ) {
            val labelName = if (label.isEmpty()) UserLabelMeta.image(level, Theme.darkMode) else UserLabelMeta.image(label)

            WebImage(
                uri = ServerRes.Assets.Label.pic(labelName).url,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
            SimpleClipText(
                text = label.ifEmpty { UserLabelMeta.label(level) },
                modifier = Modifier.fillMaxSize().padding(PaddingValues(start = 12.3.dp, end = 12.3.dp, top = 15.2.dp, bottom = 5.3.dp)).zIndex(2f),
                color = if (Theme.darkMode) Colors.White else Colors.Dark,
                style = Theme.typography.v8.bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UserBar(
    avatar: String,
    name: String,
    time: String,
    label: String,
    level: Int,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
    ) {
        WebImage(
            uri = avatar,
            key = DateEx.TodayLong,
            contentScale = ContentScale.Crop,
            circle = true,
            onClick = onAvatarClick,
            modifier = Modifier.fillMaxHeight().aspectRatio(1f)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            SimpleEllipsisText(
                text = name,
                style = Theme.typography.v7.bold,
                modifier = Modifier.fillMaxWidth()
            )
            SimpleEllipsisText(
                text = time,
                color = Theme.color.onSurfaceVariant,
                style = Theme.typography.v8,
                modifier = Modifier.fillMaxWidth()
            )
        }
        UserLabel(label = label, level = level)
    }
}