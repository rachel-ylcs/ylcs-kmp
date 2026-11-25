package love.yinlin.foundation.os.system

import love.yinlin.fs.File

internal object LinuxSystemVersion {
    private suspend fun readFile(path: String): String? = File(path).readText()?.trim()?.takeIf { it.isNotEmpty() }

    suspend fun getSystemVersion(): String? {
        val release = readFile("/etc/os-release")
        if (release != null) {
            val values = mutableMapOf<String, String>()
            for (line in release.lineSequence()) {
                val separator = line.indexOf('=')
                if (separator <= 0) continue
                val key = line.substring(0, separator)
                val value = line.substring(separator + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                values[key] = value
            }

            values["PRETTY_NAME"]?.takeIf { it.isNotBlank() }?.let { return it }

            val name = values["NAME"]?.takeIf { it.isNotBlank() }
            val version = values["VERSION_ID"]?.takeIf { it.isNotBlank() }
            if (name != null) return if (version != null) "$name $version" else name
        }

        return readFile("/proc/sys/kernel/osrelease")?.let { "Linux $it" }
    }
}
