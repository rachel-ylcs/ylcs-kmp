因为`Rachel`框架是跨平台框架，它可能有`wasm`或`js`侧的`target`，此时客户端请求某些站点可能会有跨域的问题。

在客户端可以通过对应的`proxy`构造出服务端实际的代理地址，然后请求到服务端后，如果服务端也定义了`proxy`，
那么会在服务器中启动一个`Http`客户端，访问请求后再将响应结果转发回原客户端。

服务端定义代理的方式非常简单，只需要在引擎中重写`proxy`的路径名与白名单即可。示例如下:

```kotlin
override val proxy = Proxy(name = "proxy", whitelist = listOf(
    "(?:https?://)?m\\.weibo\\.cn.*".toRegex(),
    "(?:https?://)?visitor\\.passport\\.weibo\\.cn.*".toRegex(),
    "(?:https?://)?(?:wx|tvax)\\d+\\.sinaimg\\.cn.*".toRegex(),
    "(?:https?://)?f\\.video\\.weibocdn\\.com.*".toRegex(),
))
```

例如此处定义了代理的名称为`proxy`，那么客户端实际上发送的路径为`localhost:port/proxy?proxy={url}`。
客户端会将需要代理的目标地址作为参数传递到服务端，服务端接收后会通过一个正则表达式列表的白名单来决定是否代理。

请求转发包括了`body`数据与`header`。

!!! Tip
    这里的路径名与静态资源目录一样，最好在公共模块中定义，保证客户端和服务端访问的是同一个路径。