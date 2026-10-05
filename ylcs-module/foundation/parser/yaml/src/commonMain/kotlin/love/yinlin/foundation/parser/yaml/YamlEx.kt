package love.yinlin.foundation.parser.yaml

val YamlElement?.Boolean: Boolean get() = (this as YamlPrimitive).internalBoolean!!
val YamlElement?.BooleanNull: Boolean? get() = (this as? YamlPrimitive)?.internalBoolean
val YamlElement?.Int: Int get() = (this as YamlPrimitive).internalInt!!
val YamlElement?.IntNull: Int? get() = (this as? YamlPrimitive)?.internalInt
val YamlElement?.Long: Long get() = (this as YamlPrimitive).internalLong!!
val YamlElement?.LongNull: Long? get() = (this as? YamlPrimitive)?.internalLong
val YamlElement?.Float: Float get() = (this as YamlPrimitive).internalFloat!!
val YamlElement?.FloatNull: Float? get() = (this as? YamlPrimitive)?.internalFloat
val YamlElement?.Double: Double get() = (this as YamlPrimitive).internalDouble!!
val YamlElement?.DoubleNull: Double? get() = (this as? YamlPrimitive)?.internalDouble
val YamlElement?.String: String get() = if (this == YamlNull) error("") else (this as YamlPrimitive).content
val YamlElement?.StringNull: String? get() = if (this == YamlNull) null else (this as? YamlPrimitive)?.content
val YamlElement?.Object: YamlObject get() = this as YamlObject
val YamlElement?.ObjectNull: YamlObject? get() = this as? YamlObject
val YamlElement?.ObjectEmpty: YamlObject get() = this as? YamlObject ?: buildYamlObject { }
val YamlElement?.Array: YamlArray get() = this as YamlArray
val YamlElement?.ArrayNull: YamlArray? get() = this as? YamlArray
val YamlElement?.ArrayEmpty: YamlArray get() = this as? YamlArray ?: buildYamlArray { }