package love.yinlin.nativebridge

import java.io.File

class NativeDesktopDistribution {
    internal var enabled = false
    var entryPoint: String = "main"
    var arguments: List<String> = emptyList()
    var workingDirectory: File? = null
    var packageName: String = "application"
    var displayName: String = "Application"
    var packageVersion: String = "1.0.0"
    var description: String = ""
    var vendor: String = ""
    var author: String = ""
    var copyright: String = ""
    var bundleId: String = ""
    var iconFile: File? = null
    val windows = NativeDesktopPlatform()
    val linux = NativeDesktopPlatform()
    val macos = NativeDesktopPlatform()

    fun windows(block: NativeDesktopPlatform.() -> Unit) { windows.block() }
    fun linux(block: NativeDesktopPlatform.() -> Unit) { linux.block() }
    fun macos(block: NativeDesktopPlatform.() -> Unit) { macos.block() }

    internal fun icon(target: String): File? = when (target) {
        "mingw_x64" -> windows.iconFile
        "linux_x64" -> linux.iconFile
        else -> macos.iconFile
    } ?: iconFile

    internal fun metadata(): Map<String, String> = mapOf("name" to packageName, "displayName" to displayName, "version" to packageVersion, "description" to description, "vendor" to vendor, "author" to author, "copyright" to copyright, "bundleId" to bundleId)

    internal fun validate() {
        require(packageName.isNotBlank() && packageName.toByteArray(Charsets.UTF_8).size <= 250 && packageName !in setOf(".", "..") && packageName.none { it < ' ' || it in "/\\:\"<>|?*" }) { "desktop.packageName must be a portable directory/file name of at most 250 UTF-8 bytes" }
        require(packageName == packageName.trimEnd(' ', '.') && !Regex("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]").matches(packageName.substringBefore('.').uppercase())) { "desktop.packageName is a reserved or normalized Windows filename" }
        require(metadata().values.all { value -> value.length <= 8192 && value.none { it < ' ' && it !in "\t\r\n" } }) { "Desktop application metadata contains an invalid control character or exceeds 8192 characters" }
    }
}
