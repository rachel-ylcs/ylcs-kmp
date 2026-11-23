@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlinx.browser.window
import love.yinlin.compose.screen.DeepLink
import love.yinlin.fs.FileSystemFileHandle
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import love.yinlin.uri.WebFileUri
import org.w3c.dom.url.URL

private val protocolScheme = Scheme("web+rachel")

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

internal fun listenForWebLaunches() {
    val initialUrl = window.location.href
    forwardLink(initialUrl)
    registerLaunchConsumer(initialUrl, ::forwardLink) { handle ->
        DeepLink.navigate(WebFileUri.registerLaunch(handle))
    }
}

private fun forwardLink(targetUrl: String) {
    val url = URL(targetUrl)
    if (url.origin != window.location.origin) return
    val uri = url.searchParams.get("deeplink")?.let(Uri::parse) ?: return
    if (uri.scheme == protocolScheme || uri.scheme == Scheme.Rachel) {
        DeepLink.navigate(uri.copy(scheme = Scheme.Rachel))
    }
}
