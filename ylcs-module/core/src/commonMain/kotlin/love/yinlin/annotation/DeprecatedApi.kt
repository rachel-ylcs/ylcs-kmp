package love.yinlin.annotation

import kotlin.annotation.AnnotationTarget.*

/**
 * 弃用API
 *
 * 将在指定版本开始彻底移除它
 */
@Target(CLASS, ANNOTATION_CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE, TYPEALIAS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class DeprecatedApi(val version: String)