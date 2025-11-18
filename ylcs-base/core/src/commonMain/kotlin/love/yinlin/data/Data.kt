package love.yinlin.data

sealed interface Data<out D> {
    data class Failure(val throwable: Throwable) : Data<Nothing>
    data class Success<out D>(val data: D) : Data<D>
}

inline fun <T, R> Data<T>.map(map: (T) -> R): Data<R> = when (this) {
    is Data.Success -> Data.Success(map(this.data))
    is Data.Failure -> this
}

inline fun <T, R> Data<T>.map(error: (Throwable) -> Throwable, success: (T) -> R): Data<R> = when (this) {
    is Data.Success -> Data.Success(success(this.data))
    is Data.Failure -> Data.Failure(error(this.throwable))
}