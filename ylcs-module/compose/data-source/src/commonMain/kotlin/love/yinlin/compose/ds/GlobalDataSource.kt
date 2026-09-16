package love.yinlin.compose.ds

import kotlin.reflect.KClass

@PublishedApi
internal object GlobalDataSource : DataSource {
    val repositories = mutableMapOf<KClass<*>, DataRepository>()

    inline fun <reified T : DataRepository> register(factory: () -> T): GlobalDataSource {
        val cls = T::class
        if (cls !in repositories) repositories[cls] = factory()
        return this
    }

    inline fun <reified T : DataRepository> cast(): T? = repositories[T::class] as? T
}