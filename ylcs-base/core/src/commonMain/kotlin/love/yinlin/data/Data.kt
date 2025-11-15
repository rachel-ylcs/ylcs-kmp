package love.yinlin.data

interface ErrorType

data object Empty : ErrorType

sealed interface Data<out D> {
    data class Failure(val type: ErrorType = Empty, val message: String? = null, val throwable: Throwable? = null) : Data<Nothing>
    data class Success<out D>(val data: D, val message: String? = null) : Data<D>
}

inline fun <T, R> Data<T>.map(map: (T) -> R): Data<R> = when (this) {
    is Data.Failure -> this
    is Data.Success -> Data.Success(map(data), message)
}