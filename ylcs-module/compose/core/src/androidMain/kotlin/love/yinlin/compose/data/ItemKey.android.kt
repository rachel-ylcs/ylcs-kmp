package love.yinlin.compose.data

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Stable
@Serializable(with = ItemKey.Serializer::class)
actual data class ItemKey actual constructor(val value: String) : Parcelable {
    companion object CREATOR : Parcelable.Creator<ItemKey> {
        override fun createFromParcel(parcel: Parcel): ItemKey = ItemKey(parcel)
        override fun newArray(size: Int): Array<ItemKey?> = arrayOfNulls(size)
    }
    constructor(parcel: Parcel) : this(parcel.readString() ?: "")
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) = dest.writeString(value)

    object Serializer : KSerializer<ItemKey> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.ItemKey", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: ItemKey) = encoder.encodeString(value.value)
        override fun deserialize(decoder: Decoder): ItemKey = ItemKey(decoder.decodeString())
    }
}