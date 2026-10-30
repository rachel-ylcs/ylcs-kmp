package love.yinlin.foundation

interface NativeDisplayContextProvider {
    /** 与窗口对应的显示连接句柄，由平台 Provider 保存和清理。 */
    var displayHandle: Long?
}
