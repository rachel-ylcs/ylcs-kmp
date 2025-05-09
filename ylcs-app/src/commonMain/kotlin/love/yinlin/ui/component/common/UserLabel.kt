package love.yinlin.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.LocalDarkMode
import love.yinlin.common.LocalDevice
import love.yinlin.common.ThemeStyle
import org.jetbrains.compose.resources.DrawableResource
import love.yinlin.resources.*
import love.yinlin.ui.component.image.MiniImage

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

	fun image(level: Int, darkMode: Boolean): Pair<DrawableResource, Color> {
		val drawable = when (level) {
			in 1..4 -> Res.drawable.img_label_fucaoweiying
			in 5..8 -> Res.drawable.img_label_pifuduhai
			in 9..12 -> Res.drawable.img_label_fenghuaxueyue
			in 13..16 -> Res.drawable.img_label_liuli
			in 17..20 -> if (darkMode) Res.drawable.img_label_lidishigongfen2 else Res.drawable.img_label_lidishigongfen1
			in 21..99 -> Res.drawable.img_label_shanseyouwuzhong
			else -> Res.drawable.img_label_fucaoweiying
		}
		val color = if (darkMode && level in 17 .. 20) Colors.White else Colors.Black
		return drawable to color
	}

	fun image(name: String): Pair<DrawableResource, Color> = Res.drawable.img_label_special to Colors.Black
}

@Composable
fun UserLabel(
	label: String,
	level: Int
) {
	val isDarkMode = LocalDarkMode.current
	val (img, color) = remember(label, level, isDarkMode) {
		if (label.isEmpty()) UserLabelMeta.image(level, isDarkMode)
		else UserLabelMeta.image(label)
	}
	val text = remember(label, level) {
		label.ifEmpty { UserLabelMeta.label(level) }
	}

	val device = LocalDevice.current
	val size = when (device.size) {
        Device.Size.SMALL -> DpSize(84.dp, 32.dp)
        Device.Size.MEDIUM -> DpSize(92.dp, 35.dp)
        Device.Size.LARGE -> DpSize(100.dp, 38.dp)
    }
	val padding = when (device.size) {
		Device.Size.SMALL -> PaddingValues(start = 11.2.dp, end = 11.2.dp, top = 14.dp, bottom = 4.8.dp)
		Device.Size.MEDIUM -> PaddingValues(start = 12.3.dp, end = 12.3.dp, top = 15.4.dp, bottom = 5.3.dp)
		Device.Size.LARGE -> PaddingValues(start = 13.4.dp, end = 13.4.dp, top = 16.8.dp, bottom = 5.8.dp)
	}

	Box(
		modifier = Modifier.size(size),
		contentAlignment = Alignment.Center
	) {
		MiniImage(
			res = img,
			modifier = Modifier.fillMaxSize()
		)
		Text(
			modifier = Modifier.fillMaxSize().padding(padding),
			text = text,
			color = color.copy(alpha = 0.8f),
			style = ThemeStyle.bodyExtraSmall,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Clip
		)
	}
}