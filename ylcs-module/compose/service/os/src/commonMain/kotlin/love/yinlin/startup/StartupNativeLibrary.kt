package love.yinlin.startup

import androidx.compose.runtime.Stable
import love.yinlin.Context
import love.yinlin.StartupArgList
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup
import love.yinlin.data.NativeLibrary
import love.yinlin.platform.loadNativeLibrary
import love.yinlin.platform.platform

@StartupArgList(name = "libraries", type = NativeLibrary::class)
@Stable
class StartupNativeLibrary : SyncStartup {
    override fun init(context: Context, args: StartupArgs) {
        val libraries = args.map<NativeLibrary>()
        for (library in libraries) {
            if (platform in library.platform) loadNativeLibrary(library.name)
        }
    }
}