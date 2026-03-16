package love.yinlin.annotation

import kotlin.annotation.AnnotationTarget.*

/**
 * 松散输入类型
 *
 * 使用字符串、json、配置对象或其他无法经过编译器校验的输入作为严格类型：类名、枚举等等的替代
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@MustBeDocumented
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class LooseTyped(val reason: String = "")