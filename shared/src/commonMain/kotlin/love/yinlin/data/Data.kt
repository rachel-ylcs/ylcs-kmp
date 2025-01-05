package love.yinlin.data

interface Failed

object Empty : Failed

enum class RequestError : Failed {
	Forbidden,
	Unauthorized,
	Failed
}

sealed interface Data<out D> {
	data class Error(val error: Failed = Empty) : Data<Nothing>
	data class Success<out D>(val data: D) : Data<D>
}

inline fun <T, R> Data<T>.map(map: (T) -> R): Data<R> {
	return when(this) {
		is Data.Error -> Data.Error(error)
		is Data.Success -> Data.Success(map(data))
	}
}