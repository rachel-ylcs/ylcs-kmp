package love.yinlin.data

data class AppInfo(
    val appName: String,
    val name: String,
    val version: Int,
    val versionName: String,
    val minVersion: Int,
    val minVersionName: String,
    val packageName: String
)