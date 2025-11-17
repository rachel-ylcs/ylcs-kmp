服务端引擎集成了`mysql`与`redis`服务，我们只需要简单的在引擎定义处启用即可。

```kotlin
object : ServerEngine() {
    override val configFile: String = "config.json"
    override val useDatabase: Boolean = true
    override val useRedis: Boolean = true

    override fun Database.onDatabaseCreate() {
        
    }
    
    override fun Redis.onRedisCreate() {
        
    }
}
```

在这个示例中，我们重写了`configFile`，因为服务需要从本地配置中获取端口或密码等隐私信息。

我们需要在`src`目录下的`resources`中创建和`configFile`相同文件名的配置文件`config.json`。

根据服务的`Config`类填写类似字段，部分字段可由`Config`默认值决定：

```json
{
    "database": {
        "port": 3306,
        "name": "test",
        "username": "root",
        "password": "123456"
    },
    "redis": {
        "port": 1234,
        "password": "abcdefg"
    }
}
```

然后重写`useDatabase`和`useRedis`为`true`即可开启对应的服务。

服务开启后我们便可以直接在`APIScope`接口作用域下访问`db`或`redis`成员变量来操纵`mysql`或`redis`。

有时候我们希望服务启动后可以完成一些初始化操作，例如数据库表的统计或redis相关密钥生成，此时可以重写相应的`onCreate`函数。