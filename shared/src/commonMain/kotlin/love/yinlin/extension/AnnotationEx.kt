package love.yinlin.extension

@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY,
	AnnotationTarget.TYPE,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.LOCAL_VARIABLE,
)
annotation class MainThread

@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY,
	AnnotationTarget.TYPE,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.LOCAL_VARIABLE,
)
annotation class IOThread

@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY,
	AnnotationTarget.TYPE,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.LOCAL_VARIABLE,
)
annotation class CPUThread