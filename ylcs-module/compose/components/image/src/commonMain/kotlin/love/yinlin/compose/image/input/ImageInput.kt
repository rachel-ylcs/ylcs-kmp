package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageInputData
import kotlin.reflect.KClass

@Stable
interface ImageInput<S : ImageSource> {
    val sourceClass: KClass<S>
    fun cacheKey(source: S): String // 必须包含输入类型的命名空间和所有影响内容的属性；不是注册名
    suspend fun read(source: S, context: ImageInputContext): ImageInputData
}
