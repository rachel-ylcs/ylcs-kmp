package love.yinlin.foundation

class StartupFactoryWithDependency<S : Startup>(
    private val delegate: StartupFactory<S>,
    dependencies: List<String>
) : StartupFactory<S> by delegate {
    override val dependencies: List<String> = delegate.dependencies + dependencies
    override fun toString(): String = delegate.toString()
}