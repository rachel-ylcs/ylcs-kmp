package love.yinlin.annotation

/**
 * 标记为规避 Native Compose SAM 链接错误而保留的显式对象实现。
 *
 * 配合使用处的 `@Suppress("ObjectLiteralToLambda")`，避免 IDE 建议改回 lambda。
 * 编译链修复后可全局搜索本注解，逐处验证并移除兼容写法。
 * 本注解仅作源码标记，不影响编译行为，也不会传递警告抑制。
 */
@Target(AnnotationTarget.EXPRESSION)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class NativeComposeSAM
