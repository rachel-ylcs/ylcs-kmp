# 服务端 WebSocket

服务端通过共享 `Sockets` 注册 WebSocket 路径，并为每条连接创建独立的 `SocketsManager`。

## 插件配置

```kotlin
override val plugins: List<BasicServerPlugin> = [
    JsonPlugin,
    WebSocketPlugin(
        socketPingPeriod = 15.seconds,
        socketTimeout = 15.seconds,
        socketMaxFrameSize = 1024L * 1024L,
        socketMasking = false
    ),
]
```

`seconds` 来自 `kotlin.time.Duration.Companion`。以上为示例配置，帧上限应依据实际消息规模确定。仓库业务入口目前使用 `Long.MAX_VALUE`，不代表框架具有自动业务消息大小限制。

## 注册连接

```kotlin
object DemoSockets : Sockets("/demo/events", "示例事件")

fun DemoScope.demoSockets() {
    DemoSockets.connect { session ->
        DemoSocketsManager(session)
    }
}
```

该注册函数需从 `APIScope.api()` 调用。每次握手建立后调用 factory，新建连接管理器。

```kotlin
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

错误记录与连接状态清理由业务实现。`send(String)` 为受保护方法，底层将 session 转为 Ktor WebSocket session。连接循环仅转发文本帧。

## 退出路径

正常结束后执行 `onClose()`，再以 NORMAL 原因关闭 socket。普通收包或处理异常先交给 `onError()`，随后继续上述路径。

取消异常由 `catchingError` 重新抛出；`onError` 与 `onClose` 本身抛错也可中断后续代码。当前循环不使用统一 `finally`，因此这些回调并非所有断开情况下均保证完成的资源清理机制。

## 鉴权与共享状态

HTTP 请求参数不会自动成为 Socket 鉴权上下文。业务应定义连接后登录、登录前消息限制与会话失效处理。

每个 Manager 只属于单条连接；房间、在线列表等跨连接状态应由独立所有者管理，并按并发访问方式同步。传输层 ping 不替代业务消息确认、去重与恢复规则。

## 客户端地址配合

当前客户端辅助函数固定使用 WSS 443。部署端应将声明路径转发到原生服务端的本机监听端口。客户端具体限制见[客户端 WebSocket](../3_client/sockets.md)。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APIScope.kt` 中的 `Sockets.connect`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/SocketsManager.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/plugin/WebSocketPlugin.kt`
