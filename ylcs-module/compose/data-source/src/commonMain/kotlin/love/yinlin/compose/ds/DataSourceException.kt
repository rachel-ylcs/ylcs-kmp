package love.yinlin.compose.ds

import love.yinlin.reflect.metaRawSimpleClassName
import kotlin.reflect.KClass

class DataSourceException(
    @Suppress("unused") source: DataSource,
    cls: KClass<*>
) : Exception("Can not find data source: ${cls.metaRawSimpleClassName}.")