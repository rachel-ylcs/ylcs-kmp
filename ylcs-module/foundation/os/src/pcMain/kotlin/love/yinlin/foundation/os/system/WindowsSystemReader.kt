package love.yinlin.foundation.os.system

internal object WindowsSystemReader {
    fun formatSystemVersion(major: Int, minor: Int, build: Int): String = when {
        major > 10 -> "Windows $major"
        major == 10 && build >= 22000 -> "Windows 11"
        major == 10 -> "Windows 10"
        major == 6 && minor == 3 -> "Windows 8.1"
        major == 6 && minor == 2 -> "Windows 8"
        major == 6 && minor == 1 -> "Windows 7"
        major == 6 && minor == 0 -> "Windows Vista"
        major == 5 && minor == 2 -> "Windows XP x64 / Server 2003"
        major == 5 && minor == 1 -> "Windows XP"
        major == 5 && minor == 0 -> "Windows 2000"
        major == 4 && minor == 0 -> "Windows NT 4.0"
        else -> "Windows $major.$minor"
    }
}
