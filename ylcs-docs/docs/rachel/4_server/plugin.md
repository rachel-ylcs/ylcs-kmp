# 服务端插件

`ServerEngine` 在注册路由前按 `plugins` 列表安装 Ktor 应用插件。插件管理应用管线能力，业务接口由 `APIScope` 注册。

## 插件接口

```kotlin
interface BasicServerPlugin {
    fun Application.onInstall()
}
```

`Application` 指 `io.ktor.server.application.Application`。实现可以在 `onInstall()` 中安装需要的 Ktor 插件。

`ServerPlugin(name)` 封装 `createApplicationPlugin` 与 `onCall`，子类实现普通回调：

```kotlin
class RequestTracePlugin(private val logger: ServerLogger) :
    ServerPlugin("RequestTrace") {

    override fun onPipelineCall(call: PipelineCall) {
        logger.info(call.request.uri)
    }
}
```

`PipelineCall` 来自 Ktor server application 包，`uri` 为 request 扩展。回调不是挂起函数，应保持轻量；需要其他管线行为时直接实现 `BasicServerPlugin`。

## JSON

`JsonPlugin` 安装 ContentNegotiation，并使用框架全局 `Json`：

```kotlin
override val plugins: List<BasicServerPlugin> = [JsonPlugin]
```

类型化 POST 的数组接收和 JSON 响应依赖对应序列化配置。

## WebSocket

```kotlin
WebSocketPlugin(
    socketPingPeriod = 15.seconds,
    socketTimeout = 15.seconds,
    socketMaxFrameSize = 1024L * 1024L,
    socketMasking = false
)
```

四个参数均由调用方提供。插件设置 ping、超时、帧大小与 masking，并使用相同 JSON converter。示例上限不是框架默认值，需按实际协议确定。

## IP 日志

`IPLogPlugin` 读取 `X-Real-IP`，当 `filterUri(uri)` 为真且请求头存在时调用 `onLog(uri, ip)`：

```kotlin
val ipLog = object : IPLogPlugin() {
    override fun filterUri(uri: String) = uri.startsWith("/demo/")

    override fun onLog(uri: String, ip: String) {
        logger.info("$ip $uri")
    }
}
```

`logger` 来自声明该对象的服务端上下文。该插件不自行校验请求头来源，真实 IP 语义取决于可信代理是否覆盖和清洗该头。没有该头时不会自动回退到连接地址。

## 顺序与错误边界

安装顺序由列表决定。插件异常不一定发生在 `APIScope.internalResponse` 的捕获范围内，不能假定都转换为 API 的 202、401 或 403 协议。

当前内置插件不自动提供业务鉴权、通用代理、完整 CORS 策略或上传限流。需要的能力应通过明确的插件、业务接口或部署配置添加，并核对目标平台的依赖可用性。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/BasicServerPlugin.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/ServerPlugin.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/plugin/`
