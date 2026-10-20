package love.yinlin.repository

class RepositoryModules internal constructor(private val modulePaths: MutableList<String>, private val parentPath: String = "") {
    private fun checkPath(path: String) = require(path.isNotBlank() && path.split(':').all { it.isNotEmpty() }) {
        "module() requires a relative project path without empty segments: $path"
    }

    private fun register(path: String): String {
        checkPath(path)
        val qualifiedPath = if (parentPath.isEmpty()) path else "$parentPath:$path"
        modulePaths.add(qualifiedPath)
        return qualifiedPath
    }

    fun module(path: String) {
        register(path)
    }

    fun module(vararg paths: String) {
        for (path in paths) register(path)
    }

    fun module(path: String, block: RepositoryModules.() -> Unit) {
        val qualifiedPath = register(path)
        RepositoryModules(modulePaths, qualifiedPath).block()
    }
}
