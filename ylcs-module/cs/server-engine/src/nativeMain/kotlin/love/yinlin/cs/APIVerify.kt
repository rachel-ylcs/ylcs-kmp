package love.yinlin.cs

interface APIVerify<T> {
    infix fun check(value: T)
    infix fun check(values: List<T>)
}
