package love.yinlin.compose.cache

import kotlin.test.Test

class TestXXHash64 {
    @Test
    fun testHash() {
        listOf(
            "hello world",
            "hello world!",
            "love.yinlin",
            "XXHash64XXHash64XXHash64XXHash64XXHash64XXHash64XXHash64",
        ).forEach {
            println("$it -> ${XXHash64.hash(it)}")
        }
    }
}