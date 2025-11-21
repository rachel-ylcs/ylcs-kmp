`Rachel`框架是基于`Kotlin Multiplatform`和`Compose Multiplatform`二次开发的，
在使用框架前默认你已经掌握了`KMP`和`CMP`基础， 如果你不了解请先学习相关前置知识。

!!! Tip
    前置知识相关文档链接

    Kotlin: [https://kotlinlang.org/docs/home.html](https://kotlinlang.org/docs/home.html)

    Jetpack Compose: [https://developer.android.google.cn/compose](https://developer.android.google.cn/compose)

    Kotlin Multiplatform: [https://kotlinlang.org/docs/multiplatform/get-started.html](https://kotlinlang.org/docs/multiplatform/get-started.html)

简单起见，从CMP项目中创建一个target为desktop的项目，然后在Main.kt中填写如下代码，这便是一个最简单的Rachel框架客户端程序。

```kotlin
private val mApp = LazyReference<RachelApplication>()
val app: RachelApplication by mApp

class MyApplication : PlatformApplication<MyApplication>(mApp, PlatformContextDelegate) {
    @Composable
    override fun Content() {
        Text("hello world!")
    }
}

fun main() = MyApplicaiton().run()
```

通过运行`desktopRun`即可看到应用程序的主窗口，并显示一行文本`hello world!`。

想要了解更多信息，请根据你感兴趣的模块找到指定分类阅读。

- [快速开始](../1_start/entry.md)：初步介绍Rachel框架，编译环境和核心模块。
- [跨平台UI](../2_ui/entry.md)：介绍Rachel框架客户端中UI相关模块。
- [客户端引擎](../3_client/entry.md)：介绍Rachel框架C/S双端交互所使用的客户端引擎。
- [服务端引擎](../4_server/entry.md)：介绍Rachel框架C/S双端交互所使用的服务端引擎。
- [模块](../5_module/entry.md)：其他Rachel模块，可以按需引入。