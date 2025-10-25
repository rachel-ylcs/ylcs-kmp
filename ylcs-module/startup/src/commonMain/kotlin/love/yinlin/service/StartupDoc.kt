package love.yinlin.service

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class StartupDoc(vararg val args: StartupArg<*>)