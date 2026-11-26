package love.yinlin.compose.graphics

import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ShaderUniformsTest {
    @Test
    fun snapshotsRetainTheirUniformValues() {
        val values = floatArrayOf(0.25f, 0.5f)
        val uniforms = ShaderUniforms().apply {
            float("progress", 0.2f)
            floatArray("weights", values)
        }
        val first = uniforms.snapshot().associateBy { it.name }

        values[0] = 1f
        uniforms.float("progress", 0.8f)
        val second = uniforms.snapshot().associateBy { it.name }

        assertEquals(0.2f, first.getValue("progress").values(Size.Zero).single())
        assertEquals(0.8f, second.getValue("progress").values(Size.Zero).single())
        assertContentEquals(floatArrayOf(0.25f, 0.5f), first.getValue("weights").values(Size.Zero))
    }

    @Test
    fun sizeUniformsResolveAtDrawTime() {
        val uniforms = ShaderUniforms().apply {
            resolution("size")
            resolutionWithAspectRatio("sizeAndAspect")
        }.snapshot().associateBy { it.name }

        assertContentEquals(floatArrayOf(20f, 10f), uniforms.getValue("size").values(Size(20f, 10f)))
        assertContentEquals(floatArrayOf(10f, 20f, 0.5f), uniforms.getValue("sizeAndAspect").values(Size(10f, 20f)))
    }
}
