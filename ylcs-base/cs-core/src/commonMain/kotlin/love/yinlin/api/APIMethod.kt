package love.yinlin.api

sealed interface APIMethod {
    data object None : APIMethod
    data object Get : APIMethod
    data object Post : APIMethod
    data object Form : APIMethod
}