# 客户端 WebSocket

共享 `Sockets` 声明连接路径，客户端通过 `WebSocketClient.Connection` 处理文本消息和连接事件。

## 声明入口

```kotlin
object DemoSockets : Sockets("/demo/events", "示例事件")
```

消息 DTO、序列化格式、鉴权和业务确认由应用协议定义。`Sockets` 不自动生成这些规则。

## 连接示例

以下片段放置在页面中：

```kotlin
private val connection = object : WebSocketClient.Connection() {
    override suspend fun onConnect() {
        send("hello")
        Coroutines.main { isConnected = true }
    }

    override suspend fun onMessage(msg: String) {
        val message = decodeMessage(msg)
        Coroutines.main { applyMessage(message) }
    }

    override suspend fun onError(err: Throwable) {
        Coroutines.main { slot.tip.error(err.message) }
    }

    override suspend fun onDisconnect() {
        Coroutines.main { isConnected = false }
    }
}

override fun initialize() {
    launch {
        DemoSockets.openConnection(connection)
    }
}
```

`openConnection` 切换到 I/O 上下文并持续挂起收取消息。页面 `initialize()` 为普通函数，连接工作通过 `launch` 绑定 `viewModelScope`。共享服务连接也应使用明确的协程所有者。

`send(text)` 在 session 不存在时返回 `false`；底层发送异常会传播。返回 `true` 只代表本地发送调用完成，不代表对端已经处理消息。

## 地址规则

```text
ClientEngine.baseUrl = https://api.example.com:8443/base
实际连接地址         = wss://api.example.com:443/demo/events
```

当前实现只解析基础地址的 host，固定使用 WSS 默认端口与 `Sockets.path`，不会继承基础 scheme、端口和路径。`http://localhost:1211` 不能通过此辅助函数直接建立明文 WS 连接。

需要自定义 scheme、端口或握手参数时，应在网络层实现对应能力，不能将其作为当前辅助函数已有的配置。

## 消息与退出

只有文本帧进入 `onMessage`。回调在连接收集流程中执行，耗时处理会延迟后续消息，应按工作性质切换上下文。

正常结束或普通收包异常后，当前代码调用 `onDisconnect`、关闭 session 并清空引用。取消异常会由 `catchingError` 重新抛出；`onError` 或 `onDisconnect` 本身抛错也可提前中断后续代码。因此不能将这些回调描述为所有退出情况下均保证执行的 `finally` 清理钩子。

同一个 `Connection` 不应被并发用于多次连接。再次连接时虽然会尝试关闭已有 session，但封装不提供并发连接状态机。

## 重连与可靠性

当前封装不自动重连。应用需要按协议定义断线状态、退避、重新鉴权、心跳与消息确认。重试循环应等待本次连接结束，并在所属生命周期取消后停止，避免在错误回调中递归连接。

服务端注册与消息管理见[服务端 WebSocket](../4_server/sockets.md)。

## 源码入口

- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/ClientEngine.kt`
- `ylcs-module/foundation/network/src/commonMain/kotlin/love/yinlin/foundation/WebSocketClient.kt`
