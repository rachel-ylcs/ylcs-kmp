package love.yinlin.gradle

class DesktopInterop internal constructor() {
    internal val sources = linkedSetOf<String>()

    /** 在默认 JVM C++ 目录之外跟踪源码目录，并通过 RACHEL_CPP_SOURCE_DIRS 提供给 CMake。 */
    fun cpp(path: String) {
        sources += path
    }
}
