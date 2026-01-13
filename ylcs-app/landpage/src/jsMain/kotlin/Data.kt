const val DOWNLOAD_URL = "https://pan.baidu.com/s/1Ujv4UeRMXUSbXas-Qy6UKw?pwd=1211"
const val WEBAPP_URL = "https://web.yinlin.love"
const val GITHUB_URL = "https://github.com/rachel-ylcs/ylcs-kmp"
const val DEVELEOPMENT_URL = "https://kotlinlang.org"
const val BEIAN_URL = "https://beian.miit.gov.cn"

enum class ContactInfo(
    val title: String,
    val icon: String,
    val url: String
) {
    Weibo(title = "微博", icon = "fa-brands fa-weibo", url = "https://weibo.com/u/7969517430"),
    Bilibili(title = "B站", icon = "fa-brands fa-bilibili", url = "https://space.bilibili.com/3546773870086322"),
    QQGroup(title = "QQ水群", icon = "fa-brands fa-qq", url = "https://qm.qq.com/q/8sfRdA1XPy");
}

enum class AppPlatformVersion(val title: String, val textColor: String, val bgColor: String) {
    STABLE("Stable", "text-green-500", "bg-green-500/10"),
    ALPHA("Alpha", "text-pink-500", "bg-pink-500/10"),
    BETA("Beta", "text-orange-500", "bg-orange-500/10");
}

enum class AppPlatform(
    val title: String,
    val content: String,
    val iconColor: String,
    val icon: String,
    val label: AppPlatformVersion
) {
    Android("Android", "安卓平台支持度最高，覆盖全部已实现的功能，并且具有10M左右的安装包体积和极致的性能体验。", "from-purple-500 to-blue-500", "fa-brands fa-android", AppPlatformVersion.STABLE),
    IOS("iOS", "苹果端还在早期支持中，基本功能已实现，但安装只能通过自签侧载的方式, 并且可能存在一些隐性问题。", "from-yellow-400 to-green-500", "fa-brands fa-apple", AppPlatformVersion.ALPHA),
    Windows("Windows", "Windows端支持度与安卓相同，除了扫码等手机特有操作外基本完全覆盖，并且具备各类窗口操作的特性。", "from-blue-400 to-cyan-400", "fa-brands fa-windows", AppPlatformVersion.STABLE),
    Linux("Linux", "Linux端支持度除了音视频播放等功能外基本与Windows电脑端一致。", "from-purple-600 to-indigo-600", "fa-brands fa-linux", AppPlatformVersion.BETA),
    MacOS("macOS", "macOS端支持度除了音视频播放等功能外基本与Windows电脑端一致。", "from-rose-500 to-purple-800", "fa-solid fa-apple-whole", AppPlatformVersion.ALPHA),
    PWA("PWA", "PWA网页端除了听歌等文件存储相关的功能外基本已支持，后续将会实现完整的网页PWA应用，首次加载需要等待较长时间。", "from-cyan-500 to-blue-700", "fa-brands fa-edge", AppPlatformVersion.BETA),
    Web("Web", "JS网页端支持程度与PWA相同，加载速度较快，但性能可能略有下降。", "from-amber-500 to-orange-700", "fa-brands fa-internet-explorer", AppPlatformVersion.ALPHA),
    Server("Server", "服务器正在持续运行中。", "from-zinc-600 to-black", "fa-solid fa-server", AppPlatformVersion.STABLE),
}