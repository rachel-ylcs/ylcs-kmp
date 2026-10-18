# 客户端引擎

`cs:client-engine` 为共享 `API.post`、`API.form` 与 `Sockets` 提供请求扩展，负责地址拼接、参数编码、响应解码和状态码映射。

## 初始化

```kotlin
ClientEngine.init("https://api.example.com")
```

应在应用初始化阶段、首个请求之前完成配置。基础地址需包含 scheme 与 host，通常不带末尾 `/`。

| 请求类型 | 地址规则 |
| --- | --- |
| API | `baseUrl + route` |
| 资源 | `baseUrl + "/" + resourcePath` |
| WebSocket | 解析基础地址的 host，再固定连接 `wss:443` 与声明路径 |

`baseUrl` 为全局可变值。底层 Common、File 与 Socket 客户端分别惰性创建并复用。需要同时访问多个后端时，应避免在并发请求之间反复修改该全局地址。

## 共享声明

共享模块定义：

```kotlin
val ApiDemoEcho by API.post.i<String>().o<String>()
```

客户端调用：

```kotlin
val error = ApiDemoEcho.request("example") { value ->
    updateContent(value)
}
```

输入与输出由 API 类型约束。路由来自属性名，声明规则见[C/S 共享契约](../1_start/cs_core.md)。

## 调用形式

| 调用 | 结果 |
| --- | --- |
| `request(args) { outputs -> ... }` | 成功回调后返回 `null`，普通失败返回 `Throwable` |
| `request(args)` | `Data<APIResultN<...>>`，无输出时为 `Data<Unit>` |
| `requestNull(args)` | 成功结果或 `null`，无输出时为可空 Unit |

三种形式均保留 `CancellationException` 的传播。nullable 形式丢失普通失败原因，适用于允许失败折叠为空值的读取流程。

## 状态码映射

| HTTP 状态 | 客户端行为 |
| --- | --- |
| 200 | 按声明读取输出 |
| 202 | 抛出 `FailureException`，消息来自响应文本 |
| 401 | 抛出 `UnauthorizedException` |
| 408、504 | 抛出 `RequestTimeoutException` |
| 其他 | 抛出 `IllegalArgumentException` |

异常随后进入所选调用形式的结果通道，取消异常除外。202 对应业务失败是该引擎的协议约定，接入不遵守该协议的后端时应另行适配。

请求和响应读取在 I/O 上下文执行，成功解码回调恢复至调用方上下文。回调中的本地异常也可能作为请求失败返回，因此不能仅凭失败结果断定服务端未完成操作。

## 生命周期与能力边界

请求为挂起函数，页面中使用 `launch`，应用服务使用具有明确所有者的任务作用域。引擎不自动处理令牌刷新、重试、请求缓存、写入幂等或 WebSocket 重连。

鉴权信息应根据共享契约显式传递，或在应用自己的请求层中组合。当前 `ClientEngine` 没有旧文档中的 `proxy()` 辅助函数，代理现状见[代理能力](../4_server/proxy.md)。

## 源码入口

- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/ClientEngine.kt`
- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/PostRequest.kt`
- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/FormRequest.kt`
