# 服务端 WebSocket

服务端用共享 `Sockets` 声明注册路径，并为每条连接创建独立的 `SocketsManager`。WebSocket 插件负责 ping、超时、帧大小与序列化配置；业务 Manager 负责鉴权、消息状态机和共享房间。

## 安装插件

```kotlin
override val plugins: List<BasicServerPlugin> = listOf(
    JsonPlugin,
    WebSocketPlugin(
        socketPingPeriod = 15.seconds,
        socketTimeout = 15.seconds,
        socketMaxFrameSize = 1L * 1024 * 1024,
        socketMasking = false
    )
)
```

仓库真实服务把 `socketMaxFrameSize` 设为 `Long.MAX_VALUE`；对外部署时应按协议设置合理上限，避免单帧耗尽内存。TLS 通常由本机前面的反向代理处理。

## 注册连接

```kotlin
object GameSockets : Sockets("/game", "实时游戏")

fun AppServerScope.gameAPI() {
    GameSockets.connect { session ->
        GameSocketsManager(session, rooms, auth)
    }
}
```

`connect` 只能在 `APIScope.api()` 注册阶段调用，因为它依赖当前 Routing。每次连接都会调用 factory 创建 Manager。

## 实现 Manager

```kotlin
class GameSocketsManager(
    session: Any,
    private val rooms: RoomRegistry,
    private val auth: Auth
) : SocketsManager(session) {

    override suspend fun onMessage(msg: String) {
        when (val data = msg.parseJsonValue<ClientMessage>()) {
            is ClientMessage.Login -> login(data.token)
            is ClientMessage.Answer -> submit(data)
        }
    }

    override suspend fun onError(err: Throwable) {
        send(ServerMessage.Error("连接异常").toJsonString())
    }

    override suspend fun onClose() {
        rooms.leave(this)
    }
}
```

`send(String)` 是受保护方法，底层把 `session: Any` 转为 `WebSocketServerSession`。只有文本帧会传给 `onMessage`，其他帧被跳过。

## 结束顺序

连接循环中的异常交给 `onError`；随后总会调用 `onClose`，最后用 NORMAL 原因关闭 socket。`onError` 自己再抛异常会影响后续清理，因此它应尽量简单、容错，资源释放放在 `onClose` 并保证幂等。

正常客户端断开也进入 `onClose`。不要只在错误回调移除在线用户。

## 鉴权与并发

HTTP API 的令牌不会自动带到 WebSocket Manager。常见做法是连接后第一条消息登录，并在成功前拒绝其他消息；也可以在代理/握手层扩展鉴权，但当前 helper 没有暴露 call 参数。

房间、在线列表等结构会被多个连接协程并发访问，必须使用 Mutex/Actor/线程安全数据结构。Manager 实例只属于单连接；共享状态应注入专门 registry，而不是放进 companion object 后无保护修改。

## 协议可靠性

ping 只检查传输连接，不保证业务状态。实时协议还应定义：

- 登录超时和重新鉴权；
- 消息类型/协议版本；
- 客户端消息 ID 与服务端确认；
- 重复消息处理；
- 房间恢复或明确不可恢复；
- 服务关闭时的通知与排空。

当前客户端 helper 固定 `wss:443`，部署代理必须把声明路径原样转发到本机 Native 服务端。
