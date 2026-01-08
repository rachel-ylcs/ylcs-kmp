package love.yinlin.platform

object SharedLibs {
    private val libs = mutableListOf<SharedLib>()

    operator fun plusAssign(lib: SharedLib) { libs += lib }
    operator fun plusAssign(lib: List<SharedLib>) { libs += lib }

    fun load() {
        for (lib in libs) lib.load()
    }
}