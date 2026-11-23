package love.yinlin.screen

internal expect object PwaInstallation {
    val shouldSuggest: Boolean
    val installAction: (() -> Unit)?
}
