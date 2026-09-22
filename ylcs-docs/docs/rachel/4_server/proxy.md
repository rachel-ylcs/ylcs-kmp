# 代理能力现状

旧文档中的服务端代理能力在当前 `main` 基线中已经不存在。现有代码只有客户端 URL 生成函数：

```kotlin
ClientEngine.proxy(proxy = "proxy", url = target)
```

结果形如：

```text
https://api.example.com/proxy?proxy=https%3A%2F%2Ftarget.example%2Fdata
```

它只对目标 URL 做 percent encoding 并拼接地址；不会注册服务端路由、发起转发、复制 header 或绕过浏览器 CORS。若部署端没有另外实现 `/proxy`，请求只会得到 404。

## 当前项目里的用途

第三方 Web 数据访问在浏览器目标上使用这个 helper，把外部 URL 指向预期的同源 `/proxy`；非 Web 平台直接请求外站。部分资源请求还会追加编码后的 Cookie/Referer 参数或自定义头。

这代表部署环境需要有独立代理实现，可能位于未纳入当前 ServerEngine 的反向代理/网关中。仅从本仓库当前服务端源码，不能推断该端点已经可用。

## 如果要实现代理

不要做一个“接收任意 URL 并原样请求”的开放端点。至少需要：

1. 固定用途或 host allowlist，默认拒绝所有未知目标。
2. 解析后拒绝 loopback、私网、链路本地、云元数据地址和非 HTTP(S) scheme。
3. 每次 DNS 解析及重定向后重新校验目标，防止 DNS rebinding/重定向绕过。
4. 限制方法、请求头、响应头、请求体和响应体大小。
5. 设置连接、首字节和总时长超时，并限制并发。
6. 不允许客户端任意注入 `Host`、Authorization、Cookie、转发头。
7. 审计目标、调用身份、耗时与字节数，但不记录敏感正文。
8. 对图片/媒体流式转发，避免完整缓存在内存。

如果目标集合已知，专用端点（如 `/third-party/weibo/image?id=...`）通常比通用 URL 代理更安全，也更容易缓存和限流。

## CORS 的正确位置

浏览器 CORS 是目标服务端/同源代理给浏览器的许可。客户端改 URL 或添加 header 不能自行授予许可。可选方案：

- 目标 API 官方支持 CORS；
- 可信服务端专用转发；
- 构建期抓取并静态托管允许公开的数据。

不要通过关闭浏览器安全策略来验证生产方案。开发代理也要尽量复用生产 allowlist，避免上线后行为完全不同。

## 与客户端 helper 对齐

若保留当前 helper，服务端约定就是：路径与 query key 同名，例如 `/proxy?proxy=<encoded-url>`。额外参数的名字和编码必须由代理实现明确验证。更可维护的做法是把代理请求定义成共享 API DTO，而不是不断在 URL 后拼接敏感 header 参数。
