package love.yinlin.api

sealed interface APIType {
    data object Post : APIType
    data object Form : APIType
}