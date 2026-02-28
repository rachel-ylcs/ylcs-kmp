package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import kotlin.jvm.JvmName

@Stable
interface MeasureId

typealias MeasureIdProvider = Modifier.() -> Modifier

@Stable
fun MeasureId.provider(modifier: Modifier): Modifier = modifier.layoutId(this)

@Stable
fun Modifier.measureId(measureId: MeasureId): Modifier = this.layoutId(measureId)

fun List<Measurable>.find(id: MeasureId): Measurable? = this.find { it.layoutId == id }
fun List<Measurable>.require(id: MeasureId): Measurable = requireNotNull(this.find { it.layoutId == id }) { "Measurable with $id does not exist." }

@JvmName("findIntrinsic")
fun List<IntrinsicMeasurable>.find(id: MeasureId): IntrinsicMeasurable? = this.find { (it.parentData as? LayoutIdParentData)?.layoutId == id }
@JvmName("requireIntrinsic")
fun List<IntrinsicMeasurable>.require(id: MeasureId): IntrinsicMeasurable = requireNotNull(this.find { (it.parentData as? LayoutIdParentData)?.layoutId == id }) { "Measurable with $id does not exist." }