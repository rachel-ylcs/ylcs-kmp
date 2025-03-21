package love.yinlin.ui.component.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.common.Colors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import love.yinlin.resources.*

private object UserLabelMeta {
	private val labelNameFromLevel = arrayOf("BUG",
		"风露婆娑", "剑心琴魄", "梦外篝火", "烈火胜情爱", "青山撞入怀",
		"雨久苔如海", "明雪澄岚", "春风韵尾", "银河万顷", "山川蝴蝶",
		"薄暮忽晚", "沧流彼岸", "清荷玉盏", "颜如舜华", "逃奔风月",
		"自在盈缺", "青鸟遁烟", "天生妙罗帷", "梦醒般惊蜕", "韶华的结尾",
	)

	fun label(level: Int): String = labelNameFromLevel.getOrNull(level) ?: labelNameFromLevel[0]

	fun image(level: Int): DrawableResource = when (level) {
		in 1..3 -> Res.drawable.img_label_fucaoweiying
		in 4..6 -> Res.drawable.img_label_pifuduhai
		in 7..10 -> Res.drawable.img_label_fenghuaxueyue
		in 11..13 -> Res.drawable.img_label_liuli
		in 14..17 -> Res.drawable.img_label_lidishigongfen
		in 18..99 -> Res.drawable.img_label_shanseyouwuzhong
		else -> Res.drawable.img_label_fucaoweiying
	}

	fun image(name: String): DrawableResource = Res.drawable.img_label_special
}

@Composable
fun UserLabel(
	label: String,
	level: Int
) {
	val img = remember(label, level) {
		if (label.isEmpty()) UserLabelMeta.image(level)
		else UserLabelMeta.image(label)
	}
	val text = remember(label, level) {
		label.ifEmpty { UserLabelMeta.label(level) }
	}

	Box(
		modifier = Modifier.width(84.dp).height(32.dp),
		contentAlignment = Alignment.Center
	) {
		Image(
			modifier = Modifier.fillMaxSize(),
			painter = painterResource(img),
			contentDescription = null
		)
		Text(
			modifier = Modifier.fillMaxSize()
				.padding(start = 11.2.dp, end = 11.2.dp, top = 13.5.dp, bottom = 4.8.dp),
			text = text,
			color = Colors.Black,
			style = if (text.length >= 5) MaterialTheme.typography.bodySmall else
				MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Clip
		)
	}
}