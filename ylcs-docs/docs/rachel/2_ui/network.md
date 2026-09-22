# 通用网络

`love.yinlin.foundation:network` 是对 Ktor Client 的轻量跨平台封装，适合访问第三方 HTTP、下载文件或建立自定义 WebSocket。调用 Rachel 自己的类型化后端时，应优先使用[客户端引擎](../3_client/entry.md)，不要手工重复协议编码。

## 客户端类型

```kotlin
NetClient.Common   // 默认请求超时 10 秒
NetClient.File     // 默认请求超时 180 秒

buildCommonNetClient(timeout = 15_000)
buildFileClient(timeout = 300_000)
buildSocketClient()
```

实例使用 `lazy` 创建并可复用。不要为每次请求创建新的底层 `HttpClient`；连接池、Cookie/插件初始化和 Native 资源都会因此浪费。

## 发起请求

`RequestScope` 统一 method、URL、body、表单、headers 和 cookies；`ResponseScope` 暴露状态码、最终 URL、响应头、Cookie、原始字节、字符串和解析后的 body。

```kotlin
val profile: Profile? = NetClient.Common.request<Profile, Profile>(
    onRequest = {
        method = HttpMethod.Get
        url = "https://example.com/profile.json"
        headers = headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
    },
    onResponse = {
        require(status.isSuccess())
        body
    }
)
```

也有按 `String`、`ByteArray`、`JsonObject` 提供响应体的重载：

```kotlin
val html: String? = NetClient.Common.request(
    onRequest = { url = "https://example.com" },
    onResponse = { text: String -> text }
)
```

异常由 `catchingNull` 转为 `null`，HTTP 非 2xx 也不会自动抛错；是否接受状态码由 `onResponse` 决定。如果业务需要区分网络失败、解析失败和 HTTP 失败，应在这一层包装成自己的结果类型，而不是只向 UI 返回空值。

`form` 表示 `application/x-www-form-urlencoded`。`data` 是原始 `ByteArray`；设置 JSON body 时还要显式提供正确 Content-Type。multipart 文件上传不是这个 RequestScope 的职责，类型化上传见[客户端文件](../3_client/file.md)。

## 下载

大文件直接写入 `Sink`：

```kotlin
val ok = target.write { sink ->
    NetClient.File.download(
        url = url,
        sink = sink,
        isCancel = { !coroutineContext.isActive },
        onGetSize = { total -> expectedSize = total },
        onTick = { current, total -> progress = current.toFloat() / total }
    )
}
```

进度回调按大约 64 KiB 的增量节流。服务器未提供总长度时，`total` 可能暂时为 0；计算比例前必须检查。取消回调抛出取消异常并返回失败，临时文件是否删除由调用方决定。

小内容可以 `download(url): ByteArray?`，但不要用它载入不受控的大文件。更稳妥的下载流程是写临时文件、校验长度/摘要，再移动到最终路径。

## WebSocket

```kotlin
val connection = object : WebSocketClient.Connection() {
    override suspend fun onConnect() { send("hello") }
    override suspend fun onMessage(msg: String) { /* decode */ }
    override suspend fun onError(err: Throwable) { /* report */ }
    override suspend fun onDisconnect() { /* update state */ }
}

buildSocketClient().connect(
    host = "api.example.com",
    path = "/events",
    connection = connection
)
```

当前封装固定使用 `wss` 与默认 443 端口，只处理文本帧。`connect` 会挂起并持续收集消息，结束后调用 `onDisconnect`、关闭 session 并清空引用。调用方应在页面/服务协程域中启动它，以便生命周期结束时取消。

重连、退避、心跳、鉴权刷新和消息确认均不是封装内置能力。若需要可靠长连接，应在 `Connection` 外层建立状态机；不要从 `onError` 直接无延迟递归连接。

## 平台差异

底层引擎由各平台 `actual` 构建，公共调用方式相同，但浏览器仍受 CORS、Cookie 和混合内容策略限制；移动端还受网络权限与 TLS 配置约束。客户端 URL 拼接工具不能绕过这些安全策略，必要的跨域代理必须由可信服务端显式实现。
