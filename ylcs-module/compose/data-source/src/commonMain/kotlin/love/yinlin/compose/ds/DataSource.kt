package love.yinlin.compose.ds

interface DataSource {
    companion object {
        inline operator fun <reified T1 : DataRepository> invoke(
            noinline factory1: () -> T1,
        ): DataSource = GlobalDataSource.register(factory1)

        inline operator fun <reified T1 : DataRepository, reified T2 : DataRepository> invoke(
            noinline factory1: () -> T1,
            noinline factory2: () -> T2,
        ): DataSource = GlobalDataSource.register(factory1).register(factory2)

        inline operator fun <reified T1 : DataRepository, reified T2 : DataRepository, reified T3 : DataRepository> invoke(
            noinline factory1: () -> T1,
            noinline factory2: () -> T2,
            noinline factory3: () -> T3,
        ): DataSource = GlobalDataSource.register(factory1).register(factory2).register(factory3)

        inline operator fun <reified T1 : DataRepository, reified T2 : DataRepository, reified T3 : DataRepository, reified T4 : DataRepository> invoke(
            noinline factory1: () -> T1,
            noinline factory2: () -> T2,
            noinline factory3: () -> T3,
            noinline factory4: () -> T4,
        ): DataSource = GlobalDataSource.register(factory1).register(factory2).register(factory3).register(factory4)

        inline operator fun <reified T1 : DataRepository, reified T2 : DataRepository, reified T3 : DataRepository, reified T4 : DataRepository, reified T5 : DataRepository> invoke(
            noinline factory1: () -> T1,
            noinline factory2: () -> T2,
            noinline factory3: () -> T3,
            noinline factory4: () -> T4,
            noinline factory5: () -> T5,
        ): DataSource = GlobalDataSource.register(factory1).register(factory2).register(factory3).register(factory4).register(factory5)
    }
}