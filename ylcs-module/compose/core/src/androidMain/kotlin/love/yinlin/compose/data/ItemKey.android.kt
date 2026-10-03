package love.yinlin.compose.data

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable(with = ItemKeySerializer::class)
actual data class ItemKey actual constructor(internal actual val value: String) : Parcelable {
    companion object CREATOR : Parcelable.Creator<ItemKey> {
        override fun createFromParcel(parcel: Parcel): ItemKey = ItemKey(parcel)
        override fun newArray(size: Int): Array<ItemKey?> = arrayOfNulls(size)
    }
    constructor(parcel: Parcel) : this(parcel.readString() ?: "")
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) = dest.writeString(value)
}