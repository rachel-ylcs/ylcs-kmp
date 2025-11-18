关于服务器资源介绍部分请先参看[服务端资源部分](../4_server/resource.md)，这里仅介绍如何访问服务器资源。

访问服务器资源十分简单，只需要引用服务器资源清单对应的对象即可。

我们同样在公共模块中定义`ServerRes.kt`文件

```kotlin
object ServerRes : APIRes("public") {
    object Pics : APIRes(this) {
        val logo = APIRes(this, "logo.webp")
    }
}
```

例如我要获取服务器某个图片，则可以这样写：

```kotlin
println(ServerRes.Pics.logo)
```

根据你为引擎初始化的`BASE_URL`，那么它的输出将会是`"https://api.my.webiste/public/pics/logo.webp"`

这种引用方式与服务端引用完全一致！

!!! Warning
    注意，如果你使用了`APIRes`来管理静态资源，你需要在混淆配置中保留所有派生类的名称，因为它与子路径类名相关。
    混淆配置如下：`-keep class * extends love.yinlin.api.APIRes`