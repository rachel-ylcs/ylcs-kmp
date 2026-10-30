package love.yinlin.foundation

import kotlin.annotation.AnnotationTarget.*

/**
 * 引用了 native 库的操作，需要提前加载库
 */
@Target(CLASS, ANNOTATION_CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE, TYPEALIAS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class NativeLibApi(vararg val libs: String)