package love.yinlin.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Segment(
	items: List<String>,
	currentIndex: Int,
	modifier: Modifier = Modifier,
	onSelected: (index: Int) -> Unit,
) {
	SingleChoiceSegmentedButtonRow(
		modifier = modifier.wrapContentWidth()
	) {
		items.forEachIndexed { index, item ->
			SegmentedButton(
				selected = index == currentIndex,
				shape = SegmentedButtonDefaults.itemShape(index, items.size),
				onClick = { onSelected(index) }
			) {
				Text(text = item)
			}
		}
	}
}