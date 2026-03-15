package love.yinlin.fs

import love.yinlin.foundation.PlatformContextDelegate

expect object PlatformFileSystem {
    /**
     * 路径分隔符
     */
    val PathSeparator: Char

    /**
     * 换行符
     */
    val LineSeparator: String

    /**
     * App目录
     */
    fun appPath(context: PlatformContextDelegate, appName: String): File

    /**
     * 数据目录
     */
    fun dataPath(context: PlatformContextDelegate, appName: String): File

    /**
     * 缓存目录
     */
    fun cachePath(context: PlatformContextDelegate, appName: String): File
}