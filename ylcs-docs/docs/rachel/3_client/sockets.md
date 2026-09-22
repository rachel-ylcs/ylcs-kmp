# 客户端 WebSocket

共享 `Sockets` 只声明路径和标题，客户端通过 `WebSocketClient.Connection` 实现连接生命周期与文本消息处理。

## 声明协议入口

```kotlin
object GameSockets : Sockets(
    path = "/game",
    title = "实时游戏"
)

@Serializable
sealed interface ClientMessage {
    @Serializable data class Login(val token: String) : ClientMessage
    @Serializable data class Answer(val value: String) : ClientMessage
}

@Serializable
sealed interface ServerMessage {
    @Serializable data class Error(val message: String) : ServerMessage
    @Serializable data class State(val value: GameState) : ServerMessage
}
```

消息结构属于应用协议，不由 `Sockets` 自动生成。密封序列化模型应保留兼容策略；服务端新增消息时，旧客户端至少要能忽略未知内容或安全断开。

## 建立连接

```kotlin
private val connection = object : WebSocketClient.Connection() {
    override suspend fun onConnect() {
        isConnected = true
        send(ClientMessage.Login(token).toJsonString())
    }

    override suspend fun onMessage(msg: String) {
        when (val data = catchingNull { msg.parseJsonValue<ServerMessage>() }) {
            is ServerMessage.Error -> slot.tip.warning(data.message)
            is ServerMessage.State -> state = data.value
            null -> Unit
        }
    }

    override suspend fun onError(err: Throwable) {
        slot.tip.error(err.message)
    }

    override suspend fun onDisconnect() {
        isConnected = false
    }
}

override suspend fun initialize() {
    launch { GameSockets.openConnection(connection) }
}
```

`openConnection` 会一直挂起并收集文本帧，适合放在页面 `viewModelScope` 或应用服务作用域中。再次连接同一 `Connection` 会先关闭已有 session。

`send(text)` 在 session 存在并成功交给 Ktor 时返回 `true`，否则返回 `false`；这不代表对端已经处理或持久化消息。重要操作需要协议层的确认 ID 与超时。

## 当前连接地址规则

```text
ClientEngine.baseUrl = https://api.example.com:8443/base
实际 Socket       = wss://api.example.com:443/<Sockets.path>
```

当前实现只解析基础 URL 的 host，然后固定 `WSS`、默认端口 443 和声明 path；它不会继承 scheme、端口或基础路径。因此开发环境的 `http://localhost:1211` 不能直接通过该帮助函数连接本地明文 Socket。

生产部署应在 443 上由反向代理终止 TLS 并转发路径。若必须支持自定义端口、`ws` 或 base path，应扩展 `WebSocketClient.connect`/客户端引擎，而不是通过字符串技巧修改 host。

## 生命周期顺序

正常或异常结束时：

1. 连接/收包异常交给 `onError`。
2. 调用 `onDisconnect`。
3. 关闭 session。
4. 清空 connection 内部 session 引用。

只有文本帧进入 `onMessage`，二进制帧会被忽略。回调运行在连接收集协程中，长耗时处理会阻塞后续消息；应快速验证并把 CPU/I/O 工作切到合适调度器。

## 重连与可靠性

框架不自动重连。推荐在外层状态机中实现：

- 指数退避和最大等待；
- 前后台/网络可用性判断；
- 每次重连重新鉴权；
- 心跳与服务端超时；
- 消息序号、去重和确认；
- 页面销毁后停止重连。

不要在 `onError` 中直接调用 `openConnection`，因为旧连接的结束清理尚未完成，也容易形成无延迟递归。让外层循环等待本次 `openConnection` 返回，再决定是否重试。
