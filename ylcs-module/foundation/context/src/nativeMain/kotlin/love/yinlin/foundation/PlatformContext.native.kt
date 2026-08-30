package love.yinlin.foundation

// 不使用object是因为dokka对空object的文档生成有bug
actual class PlatformContext private constructor() {
    companion object {
        val Instance = PlatformContext()
    }
}