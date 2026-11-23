package love.yinlin.screen

internal actual object PwaInstallation {
    actual val shouldSuggest: Boolean get() = false
    actual val installAction: (() -> Unit)? get() = null
}
