package love.yinlin.gradle

enum class BuildArchitecture {
    X86_64, ARM, AARCH64;

    override fun toString(): String = when (this) {
        X86_64 -> "x86_64"
        ARM -> "arm"
        AARCH64 -> "aarch64"
    }
}
