package love.yinlin.repository

/** Collects relative project paths; parent nodes are projects as well as scopes for their children. */
class RepositoryModules internal constructor(
    private val modulePaths: MutableList<String>,
    private val parentPath: String = "",
) {
    fun module(path: String) {
        register(path)
    }

    fun module(path: String, block: RepositoryModules.() -> Unit) {
        val qualifiedPath = register(path)
        RepositoryModules(modulePaths, qualifiedPath).block()
    }

    private fun register(path: String): String {
        require(path.isNotBlank() && path.split(':').all { it.isNotEmpty() }) {
            "module() requires a relative project path without empty segments: $path"
        }
        val qualifiedPath = if (parentPath.isEmpty()) path else "$parentPath:$path"
        modulePaths.add(qualifiedPath)
        return qualifiedPath
    }
}
