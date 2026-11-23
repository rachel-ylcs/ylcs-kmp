@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlinx.browser.window
import love.yinlin.RachelApplication
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.FileSystemFileHandle
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import love.yinlin.uri.WebFileUri
import org.w3c.dom.url.URL
import kotlin.uuid.Uuid

private fun registerLaunchConsumer(initialUrl: String, onLink: (String) -> Unit, onFile: (FileSystemFileHandle) -> Unit): Unit = js("""
    {
        if (!window.launchQueue) return;
        var firstLaunch = true;
        window.launchQueue.setConsumer(function (launch) {
            // The navigation that loaded this document can also appear in launchQueue.
            var alreadyForwarded = firstLaunch && launch.targetURL === initialUrl;
            firstLaunch = false;
            var mod = launch.files.find(function (file) {
                return file.kind === "file" && file.name.toLowerCase().endsWith(".rachel");
            });
            if (mod) onFile(mod);
            else if (launch.targetURL && !alreadyForwarded) onLink(launch.targetURL);
        });
    }
""")

internal class WebRachelApplication : RachelApplication(PlatformContext.Instance) {
    private var pendingMod: Pair<Uri, ImplicitUri>? = null

    fun listenForLaunches() {
        val initialUrl = window.location.href
        forwardLink(initialUrl)
        registerLaunchConsumer(initialUrl, ::forwardLink, ::forwardMod)
    }

    private fun forwardLink(targetUrl: String) {
        val url = URL(targetUrl)
        if (url.origin != window.location.origin) return
        val uri = url.searchParams.get("deeplink")?.let(Uri::parse) ?: return
        if (uri.scheme == ProtocolScheme || uri.scheme == Scheme.Rachel) {
            DeepLink.navigate(uri.copy(scheme = Scheme.Rachel))
        }
    }

    private fun forwardMod(handle: FileSystemFileHandle) {
        val uri = Uri(Scheme.Content, host = ModAuthority, path = "/${Uuid.generateV7()}")
        pendingMod = uri to WebFileUri(handle)
        DeepLink.navigate(uri)
    }

    override fun onDeepLink(manager: ScreenManager, uri: Uri) = cleaning({
        super.onDeepLink(manager, uri)
    }) {
        if (pendingMod?.first == uri) pendingMod = null
    }

    override fun resolveModImport(uri: Uri): ImplicitUri {
        if (uri.scheme != Scheme.Content || uri.host != ModAuthority) return super.resolveModImport(uri)
        val mod = pendingMod
        check(mod != null && mod.first == uri) { "请重新打开 MOD 文件" }
        return mod.second
    }

    private companion object {
        val ProtocolScheme = Scheme("web+rachel")
        const val ModAuthority = "web-mod"
    }
}
