package love.yinlin.cs

sealed interface APIType {
    data object Post : APIType
    data object Form : APIType
}