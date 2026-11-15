package love.yinlin.data.rachel.account

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import love.yinlin.extension.ByteArraySerializer
import love.yinlin.extension.DateEx
import love.yinlin.extension.DelegateSerializer

@Stable
@Serializable(SigninData.Serializer::class)
class SigninData(val data: ByteArray) {
    val currentDate = DateEx.Current.date

    companion object {
        const val YEAR_DAY_BYTE_COUNT = 46 // 46 * 8 = 368 > 365
        val Empty = SigninData(ByteArray(YEAR_DAY_BYTE_COUNT))
    }

    object Serializer : DelegateSerializer<SigninData, ByteArray>() {
        override val delegate: KSerializer<ByteArray> = ByteArraySerializer
        override fun encode(value: SigninData): ByteArray = value.data
        override fun decode(value: ByteArray): SigninData = SigninData(value)
    }

    operator fun set(index: Int, value: Byte) { data[index] = value }

    inline fun <R> check(block: (Boolean, Int, Int, Int) -> R): R {
        val dayIndex = currentDate.dayOfYear - 1
        val byteIndex = dayIndex / 8
        val bitIndex = dayIndex % 8
        val byteValue = data[byteIndex].toInt()
        val isSignin = (byteValue shr bitIndex) and 1 == 1
        return block(isSignin, byteValue, byteIndex, bitIndex)
    }

    val signinDayOfYear: Pair<Int, Int> get() {
        val year = currentDate.year
        val startOfYear = LocalDate(year, 1, 1)
        val startOfNextYear = LocalDate(year + 1, 1, 1)
        var count = 0
        for (b in data) count += (b.toInt() and 0xFF).countOneBits()
        return count to startOfYear.daysUntil(startOfNextYear)
    }

    override fun toString(): String = data.toHexString()
}
