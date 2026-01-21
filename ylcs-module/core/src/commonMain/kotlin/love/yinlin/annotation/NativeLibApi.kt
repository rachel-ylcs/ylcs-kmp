package love.yinlin.annotation

import kotlin.annotation.AnnotationTarget.*

/**
 * 引用了 native 库的操作，需要提前加载库
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class NativeLibApi(vararg val libs: String)