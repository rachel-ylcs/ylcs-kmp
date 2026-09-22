# 客户端引擎

`love.yinlin.cs:client-engine` 把共享 API 契约变成可调用的 Ktor Client 扩展。它负责路由拼接、JSON 数组/multipart 编码、响应解码和统一错误映射；鉴权令牌、重试、缓存与业务状态仍由应用管理。

## 初始化

应用启动时设置一次基础地址：

```kotlin
class MainApplication(context: PlatformContext) :
    PlatformApplication<MainApplication>(appReference, context) {

    init {
        ClientEngine.init("https://api.example.com")
    }
}
```

基础地址应包含 scheme 与 host，且末尾不要带 `/`：

- API 请求使用 `baseUrl + route`，路由本身以 `/` 开头。
- 静态资源使用 `baseUrl + "/" + resourcePath`。
- WebSocket 只从基础地址解析 host，当前固定连接 `wss:443`。

`baseUrl` 是全局可变值，`Common`、`File`、`Socket` 底层客户端按需惰性创建。它适合单后端应用；需要同时访问多个同构后端时，不要在并发请求之间来回修改它，应扩展为独立实例模型。

## 依赖关系

共享协议模块声明 API：

```kotlin
val ApiAccountLogin by
    API.post.i<String, String, Platform>().o<String>()
```

客户端模块为这个精确类型生成可用扩展：

```kotlin
val error = ApiAccountLogin.request(account, password, platform) { token ->
    saveToken(token)
}
```

输入数量和类型不匹配会在编译期失败；输出回调参数也由声明推导。路径由属性名生成，详情见[C/S 共享契约](../1_start/cs_core.md)。

## 三套调用风格

同一 API 可以根据调用位置选择：

| 风格 | 返回 | 适合场景 |
| --- | --- | --- |
| `request(args) { outputs }` | `Throwable?` | 页面操作，成功时直接更新状态，失败统一 Tip |
| `request(args)` | `Data<APIResultN<...>>` | 必须区分成功与具体异常的业务流程 |
| `requestNull(args)` | `APIResultN<...>?` | 分页/探测等允许把失败折叠为空的内部流程 |

不要为了代码短而到处使用 `requestNull`。它会把未授权、业务失败、超时、网络错误和解码错误全部折叠成 `null`，UI 无法给出正确反馈。

## HTTP 到异常的映射

| 状态 | 客户端结果 |
| --- | --- |
| 200 OK | 解码成功数组 |
| 202 Accepted | `FailureException`，消息来自响应文本 |
| 401 Unauthorized | `UnauthorizedException` |
| 408 / 504 | `RequestTimeoutException` |
| 其他状态 | `IllegalArgumentException("HTTP Error: ...")` |
| 连接/编码/解码异常 | 原异常进入所选结果风格 |

服务端的业务失败使用 202 是此框架的内部约定，不是通用 REST 语义。若接入其他后端，应使用通用 `NetClient` 或单独适配，不要假设它也遵守这张表。

## 生命周期

请求是挂起函数，应在页面 `viewModelScope`、Startup 的协程域或其他明确拥有者中运行：

```kotlin
class ScreenLogin : Screen() {
    private fun login(id: String, password: String) = launch {
        ApiAccountLogin.request(id, password, platform) { token ->
            app.config.userToken = token
            pop()
        }.errorTip
    }
}
```

当前生成包装使用返回异常/空值的 catching 逻辑，取消也可能被折叠到相同结果通道。调用方仍应把任务绑定到正确生命周期，并避免在页面已退出后用返回值更新外部状态。

## 引擎不负责的事情

- 不自动添加 Authorization header；令牌通常就是显式 API 输入。
- 不自动刷新令牌或重放请求。
- 不缓存响应。
- 不为表单推断 MIME、文件名或大小限制。
- 不实现 WebSocket 重连。
- `proxy()` 只生成一个 URL，不提供代理服务。

这些边界让共享协议保持简单；应用可以在 DataSource/Startup 中按业务需求组合策略，而不会把特定登录或缓存规则固化到所有请求。
