package love.yinlin.gradle

object BuildHost {
    val platform: BuildPlatform get() = System.getProperty("os.name").lowercase().let {
        when {
            it.startsWith("windows") -> BuildPlatform.Windows
            it.startsWith("mac") -> BuildPlatform.Mac
            else -> BuildPlatform.Linux
        }
    }

    val architecture: BuildArchitecture get() = System.getProperty("os.arch").lowercase().let {
        when {
            it.startsWith("aarch64") -> BuildArchitecture.AARCH64
            it.startsWith("arm") -> BuildArchitecture.ARM
            it.startsWith("amd64") || it.startsWith("x86_64") -> BuildArchitecture.X86_64
            else -> error("Unsupported architecture: $it")
        }
    }

    val resourceTag: String get() {
        val platformName = when (platform) {
            BuildPlatform.Windows -> "windows"
            BuildPlatform.Linux -> "linux"
            BuildPlatform.Mac -> "macos"
        }
        val architectureName = when (architecture) {
            BuildArchitecture.X86_64 -> "x64"
            BuildArchitecture.AARCH64 -> "arm64"
            BuildArchitecture.ARM -> "arm"
        }
        return "$platformName-$architectureName"
    }
}
