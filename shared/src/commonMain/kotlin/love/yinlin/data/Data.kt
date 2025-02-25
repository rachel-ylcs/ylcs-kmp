package love.yinlin.data

import androidx.compose.runtime.Stable

@Stable
interface Failed

@Stable
object Empty : Failed

@Stable
enum class RequestError : Failed {
	ClientError,
	Timeout,
	Canceled,
	Unauthorized,
	InvalidArgument,
}

@Stable
sealed interface Data<out D> {
	@Stable
	data class Error(val type: Failed = Empty, val message: String? = null, val throwable: Throwable? = null) : Data<Nothing>
	@Stable
	data class Success<out D>(val data: D, val message: String? = null) : Data<D>
}

@Stable
inline fun <T, R> Data<T>.map(map: (T) -> R): Data<R> = when(this) {
	is Data.Error -> this
	is Data.Success -> Data.Success(map(data), message)
}