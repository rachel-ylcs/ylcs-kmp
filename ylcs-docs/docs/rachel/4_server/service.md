# 服务端服务

`ServerService` 管理与原生服务端同生命周期的连接池、客户端及共享基础设施。

```kotlin
abstract class ServerService(protected val scope: APIScope) {
    abstract val name: String
    open suspend fun onStart() { }
    open suspend fun onClose() { }
}
```

## 注册与执行顺序

```kotlin
class DemoScope(engine: ServerEngine) : APIScope(engine) {
    val mysql = MysqlService(this)
    val redis = RedisService(this)
    override val services: List<ServerService> = [mysql, redis]

    override fun api() {
        demoAPI()
    }
}
```

启动按列表顺序执行 `onStart()`，正常退出后按逆序执行 `onClose()`。该机制不使用客户端 `StartupPool` 的依赖图，也不提供自动并行启动与依赖环检查。

启动或关闭回调抛错可能中断后续流程。当前引擎未统一实现已启动服务的异常回滚，自定义服务应明确部分初始化后的资源处理。

## 配置

MySQL 与 Redis 分别读取 `config.json` 中的 `mysql` 和 `redis` 节点：

```json
{
  "mysql": {
    "host": "127.0.0.1",
    "port": 3306,
    "name": "example",
    "username": "example",
    "password": "replace-me",
    "maxPoolSize": 10,
    "idleTimeout": 30000,
    "maxLifetime": 1800000
  },
  "redis": {
    "host": "127.0.0.1",
    "port": 6379,
    "username": null,
    "password": "replace-me",
    "maxConnection": 5000,
    "timeoutMillis": 10000,
    "maxPending": 1000,
    "maxIdle": 100,
    "minIdle": 10
  }
}
```

示例密码为占位值。配置节点缺失或解码失败时，内置服务回退到默认配置。MySQL 默认使用本机、`mysql` 数据库、`root` 和空密码，Redis 默认使用本机与空密码。部署入口应检查必要字段，避免将默认回退误认为有效配置。

## MySQL

`MysqlService` 使用 SQLx4k MySQL 连接池，普通辅助方法在获取连接后通过 `cleaning` 关闭连接。

| 方法 | 当前语义 |
| --- | --- |
| `throwQuerySQL` | 查询并返回 JSON 数组，失败抛出 |
| `querySQL` | 查询普通失败返回 `null` |
| `throwQuerySQLSingle` | 要求结果恰好一行，否则抛出 |
| `querySQLSingle` | 不满足单行或普通查询失败时返回 `null` |
| `throwExecuteSQL` | 要求执行成功且影响行数大于零 |
| `updateSQL`、`deleteSQL` | 影响行数大于零返回 `true` |
| `throwInsertSQLDuplicateKey` | 插入成功返回 `true`，重复键 1062 返回 `false`，其他错误抛出 |
| `throwInsertSQLGeneratedKey` | 返回生成键，失败或没有生成键时抛出 |
| `throwTransaction` | 将同一事务的 `QueryExecutor` 传入挂起 block |

连接获取错误可能在 nullable 查询封装外抛出，不能将其描述为所有故障均返回空值。取消异常继续传播。

```kotlin
val user = mysql.throwQuerySQLSingle(
    "SELECT uid, name FROM user WHERE uid = ?",
    uid
)

mysql.throwTransaction { transaction ->
    transaction.throwExecuteSQL(
        "UPDATE user SET name = ? WHERE uid = ?",
        name,
        uid
    )
}
```

值使用占位符绑定，动态表名、列名等结构元素由受控集合选择。事务中应使用传入 executor，避免重新获取独立连接。

## Redis

```kotlin
redis["session:example"] = "42"
val value = redis["session:example"]
redis.setex("verification:example", "placeholder", 5.minutes)
redis.remove("session:example")

redis.pipeline {
    // 组合相关 Redis 操作。
}
```

`minutes` 来自 `kotlin.time.Duration.Companion`。服务使用 ReThis 并开启连接池。部分方法在 client 为 `null` 时直接不执行或返回 `null`，因此调用方需遵守服务已启动的生命周期前提。

## 自定义服务

自定义实现可在 `onStart()` 中读取 `scope.engine.config[name]` 并建立资源，在 `onClose()` 中停止任务、解除绑定和关闭连接。

请求用户、事务和临时文件不应成为共享服务字段。长期后台任务需要明确的作用域、并发限制和关闭流程。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/ServerService.kt`
- `ylcs-module/cs/server-service-mysql/src/nativeMain/kotlin/love/yinlin/cs/service/`
- `ylcs-module/cs/server-service-redis/src/nativeMain/kotlin/love/yinlin/cs/service/`
