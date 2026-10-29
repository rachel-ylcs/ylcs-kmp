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

连接管理器成功创建后，收包循环由 `cleaning` 包围。正常结束、普通异常、取消或 `onError()` 抛错，均在 `finally` 中进入 `Coroutines.wait` 的不可取消清理上下文：先调用 `onClose()`，再通过嵌套 `finally` 尝试以 NORMAL 原因关闭 socket。即使 `onClose()` 抛错，底层关闭仍会尝试执行。

取消异常继续传播，不调用普通错误处理来代替取消。管理器 factory 在清理作用域建立前执行，构造失败不触发该管理器的 `onClose()`；构造过程中已经创建的资源需要自行处理。`onClose()` 和关闭 session 本身仍可能失败，业务回调应明确资源所有权。

## 鉴权与共享状态

HTTP 请求参数不会自动成为 Socket 鉴权上下文。业务应定义连接后登录、登录前消息限制与会话失效处理。

每个 Manager 只属于单条连接；房间、在线列表等跨连接状态应由独立所有者管理，并按并发访问方式同步。传输层 ping 不替代业务消息确认、去重与恢复规则。

## 客户端地址配合

当前客户端辅助函数固定使用 WSS 443。部署端应将声明路径转发到原生服务端的本机监听端口。客户端具体限制见[客户端 WebSocket](../3_client/sockets.md)。

## 业务中的 Socket 注册

游戏接口注册函数为每条歌词游戏连接创建新的管理器：

示例来源：[GameAPI.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/server/src/nativeMain/kotlin/love/yinlin/cs/GameAPI.kt#L153)。

```kotlin
LyricsSockets.connect { LyricsSocketsManager(mysql, AN, it) }
```

`mysql` 与 `AN` 是 `ServerScope` 的共享服务，`it` 是本次握手的 session。`LyricsSocketsManager` 处理登录、邀请与游戏消息；传输层不自动复用普通 HTTP 请求的鉴权。共享路径及消息 DTO 位于 `ylcs-app/cs` 的 `LyricsSockets.kt`。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APIScope.kt` 中的 `Sockets.connect`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/SocketsManager.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/plugin/WebSocketPlugin.kt`
