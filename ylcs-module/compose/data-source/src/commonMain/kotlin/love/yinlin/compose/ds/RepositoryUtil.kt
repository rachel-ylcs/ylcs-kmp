package love.yinlin.compose.ds

inline fun <reified T : DataRepository> DataSource.repository(): T = GlobalDataSource.cast() ?: throw DataSourceException(this, T::class)