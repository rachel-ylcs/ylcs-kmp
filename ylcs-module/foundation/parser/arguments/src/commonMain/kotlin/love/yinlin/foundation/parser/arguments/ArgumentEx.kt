package love.yinlin.foundation.parser.arguments

import love.yinlin.reflect.metaSimpleClassName

private inline fun <reified T> Argument?.convert(conversion: (String) -> T?): T {
    val value = this?.firstOrNull ?: throw ArgumentStateException("Argument has no value.")
    return conversion(value) ?: throw ArgumentStateException("Cannot convert argument value '$value' to ${metaSimpleClassName<T>()}.")
}

val Argument?.String: String get() = this?.firstOrNull ?: throw ArgumentStateException("Argument is missing.")
val Argument?.StringNull: String? get() = this?.firstOrNull
val Argument?.Boolean: Boolean get() = convert { it.toBooleanStrictOrNull() }
val Argument?.BooleanNull: Boolean? get() = this?.firstOrNull?.toBooleanStrictOrNull()
val Argument?.Byte: Byte get() = convert { it.toByteOrNull() }
val Argument?.ByteNull: Byte? get() = this?.firstOrNull?.toByteOrNull()
val Argument?.Short: Short get() = convert { it.toShortOrNull() }
val Argument?.ShortNull: Short? get() = this?.firstOrNull?.toShortOrNull()
val Argument?.Int: Int get() = convert { it.toIntOrNull() }
val Argument?.IntNull: Int? get() = this?.firstOrNull?.toIntOrNull()
val Argument?.Long: Long get() = convert { it.toLongOrNull() }
val Argument?.LongNull: Long? get() = this?.firstOrNull?.toLongOrNull()
val Argument?.UByte: UByte get() = convert { it.toUByteOrNull() }
val Argument?.UByteNull: UByte? get() = this?.firstOrNull?.toUByteOrNull()
val Argument?.UShort: UShort get() = convert { it.toUShortOrNull() }
val Argument?.UShortNull: UShort? get() = this?.firstOrNull?.toUShortOrNull()
val Argument?.UInt: UInt get() = convert { it.toUIntOrNull() }
val Argument?.UIntNull: UInt? get() = this?.firstOrNull?.toUIntOrNull()
val Argument?.ULong: ULong get() = convert { it.toULongOrNull() }
val Argument?.ULongNull: ULong? get() = this?.firstOrNull?.toULongOrNull()
val Argument?.Float: Float get() = convert { it.toFloatOrNull() }
val Argument?.FloatNull: Float? get() = this?.firstOrNull?.toFloatOrNull()
val Argument?.Double: Double get() = convert { it.toDoubleOrNull() }
val Argument?.DoubleNull: Double? get() = this?.firstOrNull?.toDoubleOrNull()
val Argument?.Char: Char get() = convert { it.singleOrNull() }
val Argument?.CharNull: Char? get() = this?.firstOrNull?.singleOrNull()
