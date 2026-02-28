package love.yinlin.compose.ui.text

import kotlin.reflect.KClass

/**
 * 需要由外部提供绘制器或解析器的注入
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class RichExternal(vararg val cls: KClass<*>)