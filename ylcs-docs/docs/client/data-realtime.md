---
description: 配置服务端 WebSocket、回调分发、MySQL、Redis，并理解代理与可靠性的现状。
---

# 数据与实时

<p class="rc-lead">实时连接、请求内回调和数据库服务解决的是三类不同问题：连接如何保持、业务如何处理、状态如何持久化。Rachel 为它们提供基础结构，但可靠性策略仍由应用定义。</p>

## 服务端 WebSocket

```kotlin title="插件"
override val plugins: List<BasicServerPlugin> = [
    JsonPlugin,
    WebSocketPlugin(
        socketPingPeriod = 15.seconds,
        socketTimeout = 15.seconds,
        socketMaxFrameSize = 1024L * 1024L,
        socketMasking = false,
    ),
]
```

```kotlin title="注册连接"
object DemoSockets : Sockets("/demo/events", "示例事件")

fun DemoScope.demoSockets() {
    DemoSockets.connect { session ->
        DemoSocketsManager(session)
    }
}
```

每次握手建立后调用 factory，新建连接管理器。`onMessage` 只接收文本帧；`onClose` 在正常结束、普通异常、取消或 `onError` 抛错时进入。

```kotlin title="连接管理器"
class DemoSocketsManager(session: Any) : SocketsManager(session) {
    override suspend fun onMessage(msg: String) {
        send(msg)
    }

    override suspend fun onError(err: Throwable) {
        recordConnectionError(err)
    }

    override suspend fun onClose() {
        removeConnectionState()
    }
}
```

!!! warning "清理路径不是成功路径"
    握手失败也会进入 `onClose`。应用应把该回调用于复位状态，而不是假定它只与成功的连接成对出现。

### 鉴权与共享状态

HTTP 请求参数不会自动成为 Socket 鉴权上下文。业务应定义连接后登录、登录前消息限制与会话失效处理。每个 Manager 只属于单条连接；房间、在线列表等跨连接状态应由独立所有者管理，并按并发访问方式同步。

传输层 ping 不替代业务消息确认、去重与恢复规则。

## 客户端连接

```kotlin title="页面中的连接"
override fun initialize() {
    launch {
        DemoSockets.openConnection(connection)
    }
}
```

页面 `initialize()` 是普通函数，连接工作通过 `launch` 绑定 `viewModelScope`。共享服务连接也应使用明确的协程所有者。重连、退避、重新鉴权、心跳与消息确认由应用实现。

## 回调分发

`APICallback` 用于按业务键调用处理器，可显式传递事务或其他请求上下文。它是进程内函数调用，不创建网络请求或持久任务。

```kotlin title="类型"
typealias APICallback<I, O, C> = suspend APICallbackScope.(C, I) -> O
typealias APICallbackMap<K, I, O, C> = MutableMap<K, APICallback<I, O, C>>
```

| 类型参数 | 含义 |
| --- | --- |
| `K` | 分发键 |
| `I` | 业务输入 |
| `O` | 处理结果 |
| `C` | 显式上下文，如 `QueryExecutor` |

```kotlin title="注册处理器"
callbacks["store"] = { transaction, text ->
    if (text.isBlank()) failure("内容不能为空")
    transaction.throwExecuteSQL(
        "INSERT INTO demo(value) VALUES(?)",
        text,
    )
    "处理完成"
}
```

```kotlin title="在事务中调用"
ApiDemoProcess.response { operation, text ->
    val callback = callbacks[operation] ?: failure("不支持的操作")
    val message = mysql.throwTransaction { transaction ->
        callback(transaction, text)
    }
    result(message)
}
```

回调本身不会创建事务。如果处理器通过服务重新获取连接，其操作不再由传入事务统一约束。回调 Map 是普通可变 Map，重复赋值会覆盖；注册宜在开始接收请求前完成。

## MySQL

`MysqlService` 使用连接池，普通辅助方法在获取连接后通过 `cleaning` 关闭连接。

| 方法 | 语义 |
| --- | --- |
| `throwQuerySQL` | 查询并返回 JSON 数组，失败抛出 |
| `querySQL` | 普通失败返回 `null` |
| `throwQuerySQLSingle` | 要求恰好一行 |
| `querySQLSingle` | 非单行或查询失败返回 `null` |
| `throwExecuteSQL` | 要求执行成功且影响行数大于零 |
| `updateSQL`、`deleteSQL` | 影响行数大于零返回 `true` |
| `throwInsertSQLDuplicateKey` | 重复键返回 `false`，其他错误抛出 |
| `throwInsertSQLGeneratedKey` | 返回生成键 |
| `throwTransaction` | 将同一事务的 `QueryExecutor` 传入 block |

```kotlin title="查询与事务"
val user = mysql.throwQuerySQLSingle(
    "SELECT uid, name FROM user WHERE uid = ?",
    uid,
)

mysql.throwTransaction { transaction ->
    transaction.throwExecuteSQL(
        "UPDATE user SET name = ? WHERE uid = ?",
        name,
        uid,
    )
}
```

值使用占位符绑定。动态表名、列名等结构元素应由受控集合选择，不能拼接用户输入。

## Redis

```kotlin title="Redis"
redis["session:example"] = "42"
val value = redis["session:example"]

redis.setex("verification:example", "placeholder", 5.minutes)
redis.remove("session:example")

redis.pipeline {
    // 组合相关操作。
}
```

服务使用连接池。部分方法在 client 为 `null` 时直接不执行或返回 `null`，因此调用方需遵守服务已启动的生命周期前提。

## 配置

MySQL 与 Redis 分别读取 `config.json` 中的 `mysql` 与 `redis` 节点：

```json title="config.json"
{
  "mysql": {
    "host": "127.0.0.1",
    "port": 3306,
    "name": "example",
    "username": "example",
    "password": "replace-me"
  },
  "redis": {
    "host": "127.0.0.1",
    "port": 6379,
    "username": null,
    "password": "replace-me"
  }
}
```

示例密码为占位值。配置缺失或解码失败时，内置服务会回退到默认配置；部署入口应检查必要字段，避免把默认回退误认为有效配置。

## 代理现状

当前仓库未在 `ServerEngine` 或 `APIScope` 中提供 HTTP 转发路由，客户端 `ClientEngine` 也没有通用的 `proxy()` 辅助函数。业务模块中的 `proxy` 函数目前原样返回传入地址，不编码、不转发，也不解决浏览器 CORS。

!!! danger "不要假设代理端点存在"
    需要转发能力时，应定义明确的目标服务与请求结构，再由独立路由或部署层实现。任意外部 URL 不应直接交给服务端网络客户端。

## 可靠性清单

- [ ] WebSocket 断线、退避、重新鉴权与消息去重已实现；
- [ ] 房间与在线状态有独立所有者，并明确并发策略；
- [ ] 事务中的处理器使用传入的 `QueryExecutor`；
- [ ] 上传临时文件在请求作用域内完成持久化；
- [ ] 数据库配置缺失会显式失败，而不是静默使用默认值；
- [ ] 日志不包含密码、令牌与完整隐私请求体；
- [ ] 反向代理、TLS 与 CORS 由部署层明确配置。

## 下一步

进入[UI 框架](../ui/index.md)，了解如何把这些状态与交互呈现在页面中。
