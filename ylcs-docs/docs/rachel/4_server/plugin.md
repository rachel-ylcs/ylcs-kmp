# 服务端插件

ServerEngine 在建立路由前按列表安装 `BasicServerPlugin`。插件负责 Ktor 应用级能力；业务 API 仍由 `APIScope` 注册。

## 插件接口

需要完整 Ktor 安装能力时直接实现：

```kotlin
interface BasicServerPlugin {
    fun Application.onInstall()
}
```

只想观察每次 call 时，可继承 `ServerPlugin`：

```kotlin
class RequestTagPlugin : ServerPlugin("RequestTag") {
    override fun onPipelineCall(call: PipelineCall) {
        // 此回调不是 suspend；保持轻量
    }
}
```

`ServerPlugin` 内部通过 `createApplicationPlugin` 安装 `onCall` hook。不要在非挂起回调中做阻塞数据库/网络 I/O；需要复杂管线行为时直接写 `BasicServerPlugin` 并使用 Ktor 对应 hook。

## 内置插件

### JsonPlugin

安装 ContentNegotiation，并使用项目全局 `Json`。类型化 POST API 依赖它接收 `JsonArray` 和响应 `JsonElement`：

```kotlin
override val plugins = listOf(JsonPlugin)
```

### WebSocketPlugin

安装 Ktor WebSockets，可配置 ping 周期、超时、最大帧、masking，并使用同一 JSON converter：

```kotlin
WebSocketPlugin(
    socketPingPeriod = 15.seconds,
    socketTimeout = 15.seconds,
    socketMaxFrameSize = 1L * 1024 * 1024,
    socketMasking = false
)
```

参数应按真实协议流量设置；`Long.MAX_VALUE` 不是安全的通用默认。

### IPLogPlugin

抽象插件读取 `X-Real-IP`，先经 `filterUri(uri)` 过滤，再调用 `onLog(uri, ip)`：

```kotlin
object : IPLogPlugin() {
    override fun filterUri(uri: String) = uri.startsWith("/account/")
    override fun onLog(uri: String, ip: String) {
        logger.info("$ip $uri")
    }
}
```

只有在可信反向代理覆盖并清洗该 header 时，它才代表客户端地址。若服务可被直接访问，攻击者可以伪造 `X-Real-IP`。代理还应只允许内部连接到固定 localhost 端口。

## 自定义安装示例

```kotlin
object StatusPagesPlugin : BasicServerPlugin {
    override fun Application.onInstall() {
        install(StatusPages) {
            // 只处理 APIScope 之外的路由/管线异常时要谨慎，
            // 不要与框架既有的 API 错误协议重复响应。
        }
    }
}
```

插件列表顺序就是安装顺序。认证、CORS、压缩、限流和访问日志等存在顺序依赖时，应写集成测试验证实际管线，而不是只凭列表推断。

## 错误边界

`APIScope.internalResponse` 只捕获 API handler 内部异常。插件在更早的管线阶段抛错不一定得到框架的 202/401/403 映射，应由插件或 StatusPages 明确处理，并避免把内部堆栈发给客户端。

## 生产检查表

- JSON 与 WebSocket 插件只在需要时安装，配置有上限。
- CORS 指定确切 origin、method 和 header，不使用无条件通配凭据。
- 反向代理与应用对 body/frame/超时都有限制。
- 真实 IP header 只信任代理写入值。
- 日志脱敏并有容量/保留策略。
- 鉴权放在每个需要的 API/Socket 协议中；安装一个日志插件不等于访问控制。
