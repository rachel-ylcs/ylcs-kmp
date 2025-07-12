package love.yinlin.data

import androidx.compose.runtime.Stable

@Stable
interface ErrorType

data object Empty : ErrorType

enum class RequestError : ErrorType {
    ClientError,
    Timeout,
    Canceled,
    Unauthorized,
    InvalidArgument,
}

@Stable
sealed interface Data<out D> {
	data class Failure(val type: ErrorType = Empty, val message: String? = null, val throwable: Throwable? = null) : Data<Nothing>
	data class Success<out D>(val data: D, val message: String? = null) : Data<D>
}

inline fun <T, R> Data<T>.map(map: (T) -> R): Data<R> = when (this) {
	is Failure -> this
	is Success -> Data.Success(map(data), message)
}