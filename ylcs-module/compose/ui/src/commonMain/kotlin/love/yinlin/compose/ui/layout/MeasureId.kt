package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId

@Stable
interface MeasureId

typealias MeasureIdProvider = Modifier.() -> Modifier

@Stable
fun MeasureId.provider(modifier: Modifier): Modifier = modifier.layoutId(this)

@Stable
fun Modifier.measureId(measureId: MeasureId): Modifier = this.layoutId(measureId)

fun List<Measurable>.find(id: MeasureId): Measurable? = this.find { it.layoutId == id }
fun List<Measurable>.require(id: MeasureId) = requireNotNull(this.find { it.layoutId == id }) { "Measurable with $id does not exist." }