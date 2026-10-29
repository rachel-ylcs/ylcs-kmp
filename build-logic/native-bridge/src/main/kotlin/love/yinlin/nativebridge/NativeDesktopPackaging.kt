package love.yinlin.nativebridge

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Zip
import java.io.File
import java.util.zip.ZipFile

internal object NativeDesktopPackaging {
    fun executableDirectory(desktop: NativeDesktopDistribution, target: String): String = if (desktop.enabled && target == "macos_arm64") "${desktop.packageName}.app/Contents/MacOS" else ""

    fun resourceDirectory(desktop: NativeDesktopDistribution, target: String): String = if (desktop.enabled && target == "macos_arm64") "${desktop.packageName}.app/Contents/Resources" else ""

    fun configureArchive(archive: Zip, directory: Provider<File>, executablePath: String) {
        with(archive) {
            from(directory) {
                // Path patterns also traverse parent directories inside a macOS .app bundle.
                include(executablePath)
                filePermissions { unix("rwxr-xr-x") }
            }
            from(directory) {
                exclude(executablePath)
                filePermissions { unix("rw-r--r--") }
            }
            dirPermissions { unix("rwxr-xr-x") }
            doFirst {
                val executable = directory.get().resolve(executablePath)
                require(executable.isFile && executable.length() > 0) { "Missing staged Native executable: $executable" }
            }
            doLast {
                ZipFile(archiveFile.get().asFile).use { zip ->
                    val executable = zip.getEntry(executablePath)
                    require(executable != null && !executable.isDirectory && executable.size > 0 && executable.size == directory.get().resolve(executablePath).length()) { "Native archive is missing its executable: $executablePath" }
                }
            }
        }
    }

    fun finish(directory: File, executable: File, target: String, desktop: NativeDesktopDistribution) {
        if (!desktop.enabled) return
        desktop.validate()
        val icon = desktop.icon(target)
        val expected = when (target) {
            "mingw_x64" -> "ico"
            "macos_arm64" -> "icns"
            else -> "png"
        }
        require(icon == null || icon.isFile && icon.extension.equals(expected, ignoreCase = true)) { "$target desktop icon must be an existing .$expected file: $icon" }
        require(icon == null || icon.length() in 1 .. 16L * 1024 * 1024) { "Desktop icons must be at most 16 MiB" }
        when (target) {
            "mingw_x64" -> NativeWindowsResources.write(executable, desktop, icon)
            "macos_arm64" -> {
                if (icon != null) {
                    val resources = directory.resolve(resourceDirectory(desktop, target)).apply { mkdirs() }
                    icon.copyTo(resources.resolve("application-icon.icns"), overwrite = true)
                }
                val version = Regex("\\d+(?:\\.\\d+){0,2}").find(desktop.packageVersion)?.value ?: error("macOS packageVersion needs numeric components")
                val properties = linkedMapOf("CFBundlePackageType" to "APPL", "CFBundleExecutable" to executable.name, "CFBundleIdentifier" to desktop.bundleId.ifBlank { "application.${desktop.packageName}" }.replace('_', '-'), "CFBundleName" to desktop.packageName, "CFBundleDisplayName" to desktop.displayName, "CFBundleShortVersionString" to version, "CFBundleVersion" to version, "NSHumanReadableCopyright" to desktop.copyright, "CFBundleGetInfoString" to listOf(desktop.description, desktop.vendor, desktop.author).filter { it.isNotBlank() }.joinToString(" — "), "SDL_FILESYSTEM_BASE_DIR_TYPE" to "resource")
                if (icon != null) properties["CFBundleIconFile"] = "application-icon.icns"
                val entries = properties.entries.joinToString("\n") { (key, value) -> "    <key>${xml(key)}</key><string>${xml(value)}</string>" }
                directory.resolve("${desktop.packageName}.app/Contents/Info.plist").writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n<plist version=\"1.0\"><dict>\n$entries\n    <key>NSHighResolutionCapable</key><true/>\n</dict></plist>\n")
            }
        }
    }

    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
