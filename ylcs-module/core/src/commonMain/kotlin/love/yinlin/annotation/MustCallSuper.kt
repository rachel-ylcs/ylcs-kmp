package love.yinlin.annotation

import kotlin.annotation.AnnotationTarget.*

/**
 * 必须调用父类的 super 方法
 */
@Target(CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@MustBeDocumented
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class MustCallSuper