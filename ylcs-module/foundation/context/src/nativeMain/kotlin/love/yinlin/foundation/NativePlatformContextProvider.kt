package love.yinlin.foundation

open class NativePlatformContextProvider {
    /** 借用的原生窗口句柄；窗口关闭后由宿主清空，不负责释放窗口。 */
    var windowHandle: Long? = null
}
