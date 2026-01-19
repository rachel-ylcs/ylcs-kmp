package love.yinlin.annotation

import kotlin.annotation.AnnotationTarget.*

/**
 * 1. 存在风险的操作
 * 2. 未确定的操作
 * 3. 运用到内部属性的操作
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@MustBeDocumented
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class UnsafeRachelApi(val reason: String = "")