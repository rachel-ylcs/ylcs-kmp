# 服务端服务

`ServerService` 管理数据库、Redis 等与 ServerEngine 同生命周期的基础设施：

```kotlin
abstract class ServerService(protected val scope: APIScope) {
    abstract val name: String
    open suspend fun onStart() {}
    open suspend fun onClose() {}
}
```

## 注册与顺序

```kotlin
class AppServerScope(engine: ServerEngine) : APIScope(engine) {
    val mysql = MysqlService(this)
    val redis = RedisService(this)

    override val services: List<ServerService> = listOf(
        mysql,
        redis
    )
}
```

启动严格按列表顺序，关闭严格反序。这里没有 StartupPool 的依赖分析、并行启动或环检测；依赖关系必须由列表和服务实现共同保证。任一 `onStart` 抛错会中断启动，已启动服务的自动回滚并未在当前循环中实现，因此服务应可幂等关闭，部署监控要发现半启动退出。

## config.json

内置服务从以 `name` 命名的节点读取配置：

```json
{
  "mysql": {
    "host": "127.0.0.1",
    "port": 3306,
    "name": "app",
    "username": "app",
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

节点缺失或反序列化失败时，当前服务会使用默认配置，其中 MySQL 默认 `root` 且空密码、Redis 默认本机且空密码。生产入口应在创建连接前显式校验必需字段，并通过文件权限/秘密管理提供密码。

## MySQL

`MysqlService` 使用连接池，每次 helper 获取连接并在 `finally` 归还：

```kotlin
val user = mysql.throwQuerySQLSingle(
    "SELECT uid, name FROM user WHERE uid = ?",
    uid
).to<User>()

mysql.throwExecuteSQL(
    "UPDATE user SET name = ? WHERE uid = ?",
    name, uid
)
```

常用方法：

| 方法 | 语义 |
| --- | --- |
| `querySQL` / `querySQLSingle` | 失败/无结果可空 |
| `throwQuerySQL` / `throwQuerySQLSingle` | 不满足预期时抛错 |
| `throwExecuteSQL` | 执行写语句，失败抛错 |
| `updateSQL` / `deleteSQL` | 返回是否成功 |
| `throwInsertSQLDuplicateKey` | 返回 `true` 表示键重复，`false` 表示插入成功 |
| `throwInsertSQLGeneratedKey` | 返回生成键 |
| `throwTransaction` | 在同一事务 executor 中执行 block |

最后两个布尔语义容易读反，调用处应使用具名变量如 `duplicated`。始终使用参数占位，不拼接用户输入到 SQL；动态列名/排序字段必须来自 allowlist。

## Redis

```kotlin
redis["session:$token"] = uid.toString()
val value = redis["session:$token"]
redis.setex("code:$id", code, 5.minutes)
redis.remove("session:$token")

redis.pipeline {
    // 多条 Redis 操作
}
```

当前 helper 在 client 尚为空时部分操作会静默不执行/返回 null。正常请求只应在 `onStart` 成功后到达；测试或自定义生命周期中不要把空 client 当作缓存 miss。

## 自定义服务

```kotlin
class SearchService(scope: APIScope) : ServerService(scope) {
    override val name = "search"

    override suspend fun onStart() {
        // 读取 scope.engine.config[name]，建立连接
    }

    override suspend fun onClose() {
        // 停止接收任务，等待在途工作，释放资源
    }
}
```

服务适合连接池、共享客户端和进程级 worker。一次请求的用户、事务或临时文件不应存为 service 字段。关闭时先停止新工作，再等待/取消在途任务，最后释放底层句柄；ServerEngine 本身不会替每个自定义服务完成排空。
