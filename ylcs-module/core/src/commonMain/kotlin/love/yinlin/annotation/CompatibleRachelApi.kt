package love.yinlin.annotation

import kotlin.annotation.AnnotationTarget.*

/**
 * kotlin 官方未提供但后续版本可能会提供的操作，
 * 在kotlin 官方正式版本发布后应当替换为官方实现
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@MustBeDocumented
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class CompatibleRachelApi(val reason: String = "")