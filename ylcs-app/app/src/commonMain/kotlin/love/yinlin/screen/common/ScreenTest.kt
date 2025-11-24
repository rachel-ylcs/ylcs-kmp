package love.yinlin.screen.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager

@Stable
class ScreenTest(manager: ScreenManager) : Screen(manager) {
    override val title: String = "测试页"

    private var img: ImageBitmap? = null

    override suspend fun initialize() {

    }

    @Composable
    override fun Content(device: Device) {
        Canvas(modifier = Modifier.fillMaxSize().background(Colors.White)) {
            img?.let {
                drawIntoCanvas { canvas ->
                    val tl = Offset(200f, 0f)
                    val bl = Offset(0f, 256f)
                    val br = Offset(512f, 256f)
                    val tr = Offset(600f, 0f)
                    val width = it.width.toFloat()
                    val height = it.height.toFloat()
                    val paint = Paint()

                    // Paint 使用图片着色器
                    // Canvas 用这张图片作为填充纹理
                    paint.shader = ImageShader(it, TileMode.Clamp, TileMode.Clamp)

                    // 2. 定义目标位置的顶点 (User defined quad)
                    // 顺序必须与纹理坐标一一对应
                    val positions = listOf(tl, bl, br, tr)

                    // 3. 定义原始图片的纹理坐标 (Source rect)
                    // 对应上面的点：左上(0,0), 左下(0,h), 右下(w,h), 右上(w,0)
                    val texCoords = listOf(
                        Offset(0f, 0f),
                        Offset(0f, height),
                        Offset(width, height),
                        Offset(width, 0f)
                    )

                    // 4. 定义三角形索引
                    // 将四边形切分为两个三角形: (0,1,2) 和 (0,2,3)
                    val indices = listOf(0, 1, 2, 0, 2, 3)

                    // 5. 构建 Vertices 对象
                    val vertices = Vertices(
                        vertexMode = VertexMode.Triangles,
                        positions = positions,
                        textureCoordinates = texCoords,
                        colors = listOf(Colors.Transparent, Colors.Transparent, Colors.Transparent, Colors.Transparent),
                        indices = indices
                    )

                    canvas.drawVertices(vertices, BlendMode.SrcOver, paint)
                }
            }
        }
    }
}