package love.yinlin.data

interface Failed

object Empty : Failed

enum class RequestError : Failed {
	ClientError,
	Timeout,
	Canceled,
	Unauthorized,
	InvalidArgument,
}

sealed interface Data<out D> {
	data class Error(val type: Failed = Empty, val throwable: Throwable? = null) : Data<Nothing>
	data class Success<out D>(val data: D) : Data<D>
}

inline fun <T, R> Data<T>.map(map: (T) -> R): Data<R> = when(this) {
	is Data.Error -> this
	is Data.Success -> Data.Success(map(data))
}