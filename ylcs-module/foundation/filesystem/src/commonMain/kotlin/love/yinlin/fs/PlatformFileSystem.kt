package love.yinlin.fs

import love.yinlin.foundation.PlatformContext

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
    fun appPath(context: PlatformContext, appName: String): File

    /**
     * 数据目录
     */
    fun dataPath(context: PlatformContext, appName: String): File

    /**
     * 缓存目录
     */
    fun cachePath(context: PlatformContext, appName: String): File
}