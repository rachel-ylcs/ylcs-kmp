package love.yinlin.foundation.parser.toml

val TomlElement?.Boolean: Boolean get() = (this as TomlPrimitive).internalBoolean!!
val TomlElement?.BooleanNull: Boolean? get() = (this as? TomlPrimitive)?.internalBoolean
val TomlElement?.Int: Int get() = (this as TomlPrimitive).internalInt!!
val TomlElement?.IntNull: Int? get() = (this as? TomlPrimitive)?.internalInt
val TomlElement?.Long: Long get() = (this as TomlPrimitive).internalLong!!
val TomlElement?.LongNull: Long? get() = (this as? TomlPrimitive)?.internalLong
val TomlElement?.Float: Float get() = (this as TomlPrimitive).internalFloat!!
val TomlElement?.FloatNull: Float? get() = (this as? TomlPrimitive)?.internalFloat
val TomlElement?.Double: Double get() = (this as TomlPrimitive).internalDouble!!
val TomlElement?.DoubleNull: Double? get() = (this as? TomlPrimitive)?.internalDouble
val TomlElement?.String: String get() = (this as TomlPrimitive).content
val TomlElement?.StringNull: String? get() = (this as? TomlPrimitive)?.content
val TomlElement?.Object: TomlObject get() = this as TomlObject
val TomlElement?.ObjectNull: TomlObject? get() = this as? TomlObject
val TomlElement?.ObjectEmpty: TomlObject get() = this as? TomlObject ?: buildTomlObject { }
val TomlElement?.Array: TomlArray get() = this as TomlArray
val TomlElement?.ArrayNull: TomlArray? get() = this as? TomlArray
val TomlElement?.ArrayEmpty: TomlArray get() = this as? TomlArray ?: buildTomlArray { }