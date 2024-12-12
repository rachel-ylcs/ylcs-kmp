package love.yinlin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform