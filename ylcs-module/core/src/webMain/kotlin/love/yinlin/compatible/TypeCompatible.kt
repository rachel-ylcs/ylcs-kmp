@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compatible

import love.yinlin.extension.*
import kotlin.js.ExperimentalWasmJsInterop

/**
 * external 互操作 Kotlin <-> Js
 * 1. [Byte], [Short], [Char], [Int], [Float], [Double] <-> Number
 * 2. [Long] <-> BigInt
 * 3. [Boolean] <-> Boolean
 * 4. [String] <-> String
 * 5. [Array] <-> Array
 * 6. [ByteArray], [BooleanArray] <-> Int8Array
 * 7. [ShortArray] <-> Int16Array
 * 8. [IntArray] <-> Int32Array
 * 9. [CharArray] <-> Uint16Array
 * 10. [FloatArray] <-> Float32Array
 * 11. [DoubleArray] <-> Float64Array
 * 12. [LongArray] <-> BigInt64Array
 * 13. [Unit] <-> undefined
 * 14. [Enum] <-> Enum
 * 15. [Any] <-> [JsReference]
 *
 * external 互操作 Kotlin <-> Js
 * 1. [Byte], [Short], [Char], [Int], [UByte], [UShort], [UInt], [Float], [Double] <-> Number
 * 2. [Long], [ULong] <-> BigInt
 * 3. [Boolean] <-> Boolean
 * 4. [String] <-> String
 * 5. [Unit] <-> undefined
 * 6. [Function] <-> Function
 * 7. [Any] <-> [JsReference]
 *
 * 注意以下常见的转换都不支持
 * 1. [Array] 系列
 * 2. [Enum]
 */
@Suppress("unused")
object TypeCompatible