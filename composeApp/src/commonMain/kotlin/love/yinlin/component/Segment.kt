package love.yinlin.component

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Segment(
	items: List<String>,
	currentIndex: Int,
	modifier: Modifier = Modifier,
	onSelected: (index: Int) -> Unit,
) {
	SingleChoiceSegmentedButtonRow(modifier = modifier) {
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