package love.yinlin.cs

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class APIParam(val name: String, val default: String = "", val description: String = "")

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class APIReturn(val name: String = "", val description: String = "")

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class APIDoc(val description: String = "")