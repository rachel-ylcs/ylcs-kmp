package love.yinlin.compose.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.app.global.resources.*
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.DateEx
import org.jetbrains.compose.resources.DrawableResource

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

    fun image(level: Int, darkMode: Boolean): DrawableResource = when (level) {
        in 1..4 -> Res.drawable.img_label_fucaoweiying
        in 5..8 -> Res.drawable.img_label_pifuduhai
        in 9..12 -> Res.drawable.img_label_fenghuaxueyue
        in 13..16 -> Res.drawable.img_label_liuli
        in 17..20 -> if (darkMode) Res.drawable.img_label_lidishigongfen2 else Res.drawable.img_label_lidishigongfen1
        in 21..99 -> Res.drawable.img_label_shanseyouwuzhong
        else -> Res.drawable.img_label_fucaoweiying
    }

    fun image(name: String): DrawableResource = Res.drawable.img_label_special
}

@Composable
fun UserLabel(
    label: String,
    level: Int,
    onClick: (() -> Unit)? = null
) {
    val isDarkMode = Theme.darkMode
    val img = if (label.isEmpty()) UserLabelMeta.image(level, isDarkMode) else UserLabelMeta.image(label)
    val text = label.ifEmpty { UserLabelMeta.label(level) }

    val size = DpSize(92.4.dp, 35.2.dp)
    val padding = PaddingValues(start = 12.3.dp, end = 12.3.dp, top = 15.2.dp, bottom = 5.3.dp)

    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
        Box(
            modifier = Modifier.size(size).condition(onClick != null) {
                silentClick(onClick = onClick).pointerIcon(PointerIcon.Hand)
            },
            contentAlignment = Alignment.Center
        ) {
            Image(res = img, modifier = Modifier.fillMaxSize().zIndex(1f))
            SimpleClipText(
                text = text,
                modifier = Modifier.fillMaxSize().padding(padding).zIndex(2f),
                color = if (isDarkMode) Colors.White else Colors.Dark,
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
            key = remember { DateEx.TodayString },
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