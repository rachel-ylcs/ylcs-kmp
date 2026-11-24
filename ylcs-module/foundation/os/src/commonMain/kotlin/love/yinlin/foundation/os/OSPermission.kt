package love.yinlin.foundation.os

import kotlin.annotation.AnnotationTarget.*

@Target(CLASS, ANNOTATION_CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE, TYPEALIAS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class OSPermission(vararg val permissions: String)