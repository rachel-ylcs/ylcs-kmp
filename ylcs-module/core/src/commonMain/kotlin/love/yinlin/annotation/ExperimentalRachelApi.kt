package love.yinlin.annotation

import kotlin.annotation.AnnotationTarget.*

/**
 * 1. 未稳定的操作
 * 2. 未公开的操作
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@MustBeDocumented
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalRachelApi(val reason: String = "")