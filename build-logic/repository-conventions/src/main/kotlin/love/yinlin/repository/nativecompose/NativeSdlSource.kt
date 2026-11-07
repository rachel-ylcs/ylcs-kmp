package love.yinlin.repository.nativecompose

import java.io.File

internal object NativeSdlSource {
    private const val upstream = "#define SDL_DYNAMIC_API 1"
    private const val prepared = "#define SDL_DYNAMIC_API 0 /* Rachel: direct calls for the static Compose window host. */"

    fun disableDynamicApi(directory: File) {
        // SDL deliberately requires a source edit; a compiler definition triggers an error in this header.
        val header = directory.resolve("src/dynapi/SDL_dynapi.h")
        val text = header.readText()
        val enabled = text.lineSequence().count { it == upstream }
        val disabled = text.lineSequence().count { it == prepared }
        check(enabled == 1 && disabled == 0 || enabled == 0 && disabled == 1) { "Review SDL's dynamic API switch after the upstream update: $header" }
        if (disabled == 1) return
        header.writeText(Regex("(?m)^${Regex.escape(upstream)}(?=\\r?$)").replace(text, prepared))
    }
}
