@file:OptIn(ExperimentalContracts::class)
package love.yinlin.extension

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// 语言高阶作用域函数

/**
 * kotlin 2.4 起 let 的返回值必须要接受
 *
 * then 作为不需要返回值的情景替代
 */
inline fun <T, R> T.then(block: (T) -> R) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val _ = block(this)
}

/**
 * cast 用于将基类转换为派生类并执行 then,
 * 如果转换失败则无事发生
 */
inline fun <reified B, reified D : B, R> B.cast(block: (D) -> R) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    val d = (this as? D)
    if (d != null) {
        val _ = block(d)
    }
}