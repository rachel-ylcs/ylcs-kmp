---
description: 初始化客户端引擎，调用共享 API、上传文件、生成资源地址并建立 WebSocket。
---

# 客户端引擎

<p class="rc-lead"><code>cs:client-engine</code> 为共享 <code>API.post</code>、<code>API.form</code> 与 <code>Sockets</code> 提供请求扩展。它负责地址拼接、参数编码、响应解码与状态码映射，不替业务决定重试、令牌刷新或幂等。</p>

## 初始化

```kotlin title="初始化"
ClientEngine.init("https://api.example.com")
```

应在应用初始化阶段、首个请求之前完成配置。基础地址需要包含 scheme 与 host，通常不带末尾斜杠。

| 请求类型 | 地址规则 |
| --- | --- |
| API | `baseUrl + route` |
| 资源 | `baseUrl + "/" + resourcePath` |
| WebSocket | 解析基础地址的 host，固定 `wss` 与声明路径 |

`baseUrl` 是全局可变值。需要同时访问多个后端时，避免在并发请求之间反复修改。

!!! tip "初始化只赋值"
    `ClientEngine.init` 不进行连通性探测，也不加载远程配置。它成功不代表服务端已就绪。

## 调用共享接口

```kotlin title="声明与调用"
val ApiDemoEcho by API.post.i<String>().o<String>()

val error = ApiDemoEcho.request("example") { value ->
    updateContent(value)
}
```

| 调用形式 | 结果 |
| --- | --- |
| `request(args) { outputs -> ... }` | 成功回调后返回 `null`，普通失败返回 `Throwable` |
| `request(args)` | `Data<APIResultN<...>>`，无输出时为 `Data<Unit>` |
| `requestNull(args)` | 成功结果或 `null`，无输出时为可空 `Unit` |

三种形式都保留 `CancellationException` 的传播。`requestNull` 会把普通失败折叠为 `null`，不适合需要保留失败原因的提交、删除或权限处理。

## 状态码映射

| HTTP 状态 | 客户端行为 |
| --- | --- |
| 200 | 按声明读取输出 |
| 202 | 抛出 `FailureException`，消息来自响应文本 |
| 401 | 抛出 `UnauthorizedException` |
| 408、504 | 抛出 `RequestTimeoutException` |
| 其他 | 抛出 `IllegalArgumentException` |

202 表示业务失败是该引擎的协议约定。接入不遵守该协议的后端时，需要另行适配。

```kotlin title="错误分类"
when (error) {
    is UnauthorizedException -> requestLogin()
    is FailureException -> slot.tip.warning(error.message)
    is RequestTimeoutException -> slot.tip.error("请求超时")
    null -> Unit
    else -> slot.tip.error(error.message ?: "请求失败")
}
```

!!! note "失败不等于服务端未执行"
    回调中的本地异常也可能作为请求失败返回。写入类操作应结合服务端幂等语义与业务状态判断。

## 请求执行上下文

网络请求在 I/O 上下文执行；成功响应的回调切回调用方上下文后，才执行输出读取、解码与成功回调。体积较大的输出应避免在 UI 上下文执行昂贵处理。

## 文件上传

```kotlin title="上传"
val ApiDemoUpload by API.form.i<String, APIFile?>().o<String>()

ApiDemoUpload.request(
    "example",
    apiFile(selectedFiles),
) { resultId ->
    updateUploadedItem(resultId)
}.errorTip
```

| 调用 | 数据来源 |
| --- | --- |
| `apiFile(text)` | 字符串转换的字节内容 |
| `apiFile(bytes)` | `ByteArray` |
| `apiFile(rawSource)` | 单个 `RawSource` |
| `apiFile(sources)` | 多文件集合 |
| `apiFile(file)` | 挂起打开框架 `File` |
| `apiFile(files)` | 挂起打开文件列表；空列表或失败返回 `null` |

大文件宜使用流，避免先完整读入内存。上传参数是一次性消费对象，不能在请求期间提前关闭或并发读取；重试时重新打开文件并创建新的 `APIFile`。

!!! warning "流所有权"
    表单构建会把已加入请求的流收集到作用域中，并在成功、普通失败或取消时关闭。构造参数后、交给请求之前提前退出时，尚未移交的流仍需调用方释放。

## 资源地址

```kotlin title="资源 URL"
ClientEngine.init("https://api.example.com")
val avatarUrl = PublicRes.Users.User(42).avatar.url
```

结果为 `baseUrl + "/" + resourcePath`。`.url` 不执行存在性检查、查询编码、鉴权或缓存版本管理。

可变资源应显式使用版本参数或内容摘要名称。版本参与图片缓存键，不会自动拼接到 URL 上。

## WebSocket

```kotlin title="连接"
private val connection = object : WebSocketClient.Connection() {
    override suspend fun onConnect() {
        send("hello")
        Coroutines.main { isConnected = true }
    }

    override suspend fun onMessage(msg: String) {
        val message = decodeMessage(msg)
        Coroutines.main { applyMessage(message) }
    }

    override suspend fun onDisconnect() {
        Coroutines.main { isConnected = false }
    }
}
```

`openConnection` 切换到 I/O 上下文并持续挂起收取消息。页面中通过 `launch` 绑定 `viewModelScope`。

| 边界 | 说明 |
| --- | --- |
| 帧类型 | 仅文本帧进入 `onMessage` |
| 地址 | 固定 WSS 默认端口与声明路径，不继承基础地址的端口与路径 |
| 发送结果 | `true` 只表示本地发送调用完成 |
| 重连 | 不自动重连，由应用定义退避与鉴权 |
| 断开回调 | 握手失败或取消后也可能进入 |

连接实现使用 `cleaning` 包围握手与收包流程，并在不可取消上下文中调用 `onDisconnect()`。应用应把该回调用于复位状态，而不是假定它只与成功连接成对出现。

## 生命周期

- 请求是挂起函数：页面使用 `launch`，应用服务使用明确所有者；
- 长连接绑定页面或服务的生命周期，取消后停止重试；
- 全局客户端由引擎惰性创建并复用，调用方创建的其他客户端应自行关闭；
- 不自动合并重复请求，也不自动重试写入。

## 下一步

阅读[服务端引擎](server-engine.md)，了解同一份契约如何在原生服务端注册与响应。
