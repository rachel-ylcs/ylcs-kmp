# 代理能力

当前仓库未在 `ServerEngine` 或 `APIScope` 中提供 HTTP 转发路由。客户端 `ClientEngine` 也已移除旧版 `proxy()` URL 辅助函数，因此不能依据旧文档假定 `/proxy` 端点存在。

## 第三方请求现状

`ylcs-app/third-party` 中的 `TPProxy` 保留 `proxy(url)` 与 `proxyRes(url)` 两个内部函数。当前 Web 与非 Web 分支均原样返回传入地址，源码注明 Web 服务端转发尚未实现。

这些函数不编码代理地址、不转发请求，也不解决浏览器 CORS。第三方调用能否成功，取决于目标服务的访问规则与实际部署环境。

## 与服务端引擎的关系

`ServerEngine` 负责静态资源、类型化 POST 与 WebSocket。普通 `API.post` 接口不是通用 HTTP 代理，不会自动复制上游方法、请求头、状态码或流式响应。

若部署环境另有网关代理，其路径、参数与访问规则属于该网关配置，当前仓库源码不能证明其存在或可用。

## 扩展要求

需要转发能力时，应先定义明确的目标服务与请求结构，再由独立路由或业务服务实现。目标 URL、重定向、可转发请求头、响应大小和超时需受控，避免将任意外部输入直接交给服务端网络客户端。

代理实现应与鉴权、日志和缓存策略保持一致。包含 Cookie 或令牌的内容不宜直接拼接到可被记录的公共 URL。

浏览器 CORS 由目标服务或受信任的同源转发端决定，客户端字符串处理不会自行授予跨域访问许可。

## 当前转发辅助函数的实现

业务中的两个辅助函数目前只返回原地址：

示例来源：[TPProxy.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/third-party/src/commonMain/kotlin/love/yinlin/common/TPProxy.kt#L9)。

```kotlin
internal fun proxy(url: String): String = Platform.use(*Platform.Web, ifTrue = { url }, ifFalse = { url })
internal fun proxyRes(url: String): String = Platform.use(*Platform.Web, ifTrue = { url }, ifFalse = { url })
```

虽然使用了平台分支，两侧表达式相同。这些函数属于 `internal` 业务实现，不是可用于建立服务端代理的公共 API，也不会消除跨域限制。

## 源码入口

- `ylcs-app/third-party/src/commonMain/kotlin/love/yinlin/common/TPProxy.kt`
- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/ClientEngine.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/ServerEngine.kt`
