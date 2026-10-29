# 通用网络

`foundation:network` 封装 Ktor Client，用于通用 HTTP、下载与文本 WebSocket。访问采用 Rachel 共享契约的后端时，使用[客户端引擎](../3_client/entry.md)。

## 客户端选择

```kotlin
NetClient.Common
NetClient.File

val commonClient = buildCommonNetClient(timeout = 15_000L)
val fileClient = buildFileClient(timeout = 300_000L)
val socketClient = buildSocketClient()
```

默认普通请求超时为 10 秒，文件请求超时为 180 秒，连接超时配置为 5 秒。平台 `actual` 选择底层引擎。公共客户端惰性创建并复用，调用方创建的客户端应在所属生命周期结束时关闭。

## HTTP 请求

```kotlin
val profile = NetClient.Common.request<Profile, Profile>(
    onRequest = {
        method = HttpMethod.Get
        url = "https://example.com/profile.json"
        headers = NetHeader(HttpHeaders.Authorization to "Bearer $token")
    },
    onResponse = {
        require(status.isSuccess())
        body
    }
)
```

`RequestScope` 提供 `method`、`url`、`contentType`、`data`、`form`、`headers` 和 `cookies`。`NetHeader` 位于 `love.yinlin.foundation.http`。

`ResponseScope<Body>` 提供状态码、最终 URL、响应头、Cookie、`rawBody`、`bodyString` 与 JSON 解码后的 `body`。实现会先读取完整响应字节，因此该请求接口不适合不受控的大文件。

文本响应可使用重载：

```kotlin
val text: String? = NetClient.Common.request(
    onRequest = { url = "https://example.com" },
    onResponse = { body: String -> body }
)
```

普通异常转换为 `null`，取消异常继续传播。此封装不统一拒绝非成功状态码；需要状态判断时应在响应回调中显式执行。

## 请求体

`data` 表示原始字节，`form` 表示 URL 编码表单。设置 `form` 会同步调整 Content-Type。JSON 字节请求应明确声明类型：

```kotlin
onRequest = {
    method = HttpMethod.Post
    url = "https://example.com/query"
    contentType = ContentType.Application.Json
    data = query.toJsonString().encodeToByteArray()
}
```

multipart 文件请求不由 `RequestScope.form` 表示，相关接口见[客户端文件上传](../3_client/file.md)。

## 文件下载

```kotlin
target.write { sink ->
    val success = NetClient.File.download(
        url = downloadUrl,
        sink = sink,
        isCancel = { cancelRequested },
        onGetSize = { total ->
            Coroutines.main { expectedSize = total }
        },
        onTick = { current, total ->
            Coroutines.main {
                if (total > 0L) progress = current.toFloat() / total
            }
        }
    )
    require(success)
}
```

写入型下载在成功状态且实际复制字节数大于零时返回 `true`。普通失败返回 `false`，取消仍抛出取消异常。调用方负责失败后的临时文件处理。

进度按约 64 KiB 增量更新，未知总长度时可能传入 `0`，也不保证最后一次回调恰好对应完成值。完成状态应依据函数结果或上层任务状态确定。小内容可使用返回 `ByteArray?` 的下载重载。

## WebSocket

```kotlin
val connection = object : WebSocketClient.Connection() {
    override suspend fun onConnect() {
        send("hello")
    }

    override suspend fun onMessage(msg: String) {
        processMessage(msg)
    }

    override suspend fun onError(err: Throwable) {
        reportError(err)
    }

    override suspend fun onDisconnect() {
        updateDisconnectedState()
    }
}

socketClient.connect("api.example.com", "/events", connection)
```

`connect` 固定使用 `wss` 与 443 端口，只将文本帧交给 `onMessage`，并持续挂起至连接结束。`send` 返回值仅反映本地发送调用，不代表对端业务确认。

连接流程通过 `cleaning` 的 `finally` 进入清理，并在 `Coroutines.wait` 的不可取消上下文中调用 `onDisconnect()`。即使断开回调抛错，内部 `finally` 仍会尝试清空当前连接引用并关闭本次 session；只有引用仍指向本次 session 时才清空，避免覆盖后来建立的连接。取消异常继续传播，清理本身也可能失败。`onDisconnect()` 因而既可能出现在正常结束后，也可能出现在握手失败或取消后，不能据此推断曾成功连接。重连、退避和业务确认由应用管理。

## 平台约束

浏览器请求仍受 CORS、Cookie 与混合内容策略限制。移动端需具备对应网络权限及平台配置。公共 URL 或请求封装不会改变平台网络访问规则。

## 业务网络请求示例

第三方业务的 `RadioAPI.requestUser` 使用 JSON 对象重载，再将响应交给业务解析器：

示例来源：[RadioAPI.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/third-party/src/commonMain/kotlin/love/yinlin/tpl/radio/RadioAPI.kt#L87)。

```kotlin
suspend fun requestUser(id: String): RadioUser? = NetClient.Common.request({
    url = RadioUrl.user(id)
}) { json: JsonObject ->
    Fetcher.extractUser(json)
}
```

`RadioUrl`、`RadioUser` 和 `Fetcher` 是该业务模块的类型。代码展示请求封装和响应类型的配合，不代表外部服务当前可访问或响应结构不会变化。`onResponse` 回到调用方协程上下文执行，若解析器计算量大，应显式安排 CPU 工作。

## 源码入口

- `ylcs-module/foundation/network/src/commonMain/kotlin/love/yinlin/foundation/NetClient.kt`
- `ylcs-module/foundation/network/src/commonMain/kotlin/love/yinlin/foundation/RequestScope.kt`
- `ylcs-module/foundation/network/src/commonMain/kotlin/love/yinlin/foundation/WebSocketClient.kt`
