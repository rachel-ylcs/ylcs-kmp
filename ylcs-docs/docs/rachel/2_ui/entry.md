一个`Rachel`客户端应用程序分为逻辑入口与实际入口。

实际入口是指程序启动调用的`main`函数，当然在某些平台可能是其他入口，例如`Android`的`Application`或`Activity`，
而逻辑入口是指你需要定义`MyApplication`应用程序类，并继承自`PlatformApplication`类。

下面是快速开始中的示例:

```kotlin
private val mApp = LazyReference<RachelApplication>()
val app: RachelApplication by mApp

class MyApplication(delegate: PlatformContextDelegate) : PlatformApplication<MyApplication>(mApp, delegate) {
    @Composable
    override fun Content() {
        Text("hello world!")
    }
}

fun main() = MyApplicaiton().run()
```

## 逻辑入口

为什么需要逻辑入口？因为你的应用程序是跨平台的，它们在不同平台上可能有独立实现的部分，
例如Android上需要绑定设备硬件、桌面上需要设置窗口标题栏或托盘等等。

但是除了这些平台特定的配置外，更多的是公共配置代码，所以我们需要定义一个公共的基类`MyApplication`，继承自`PlatformApplication<MyApplication>`。

如果你不需要编写平台侧的特定代码，`MyApplication`可以是普通`class`。否则将它修改为`abstract class`，然后在平台侧分别创建对应的平台应用程序子类。

你需要传递一个自引用实例和平台上下文用于构建`MyApplication`。

#### 自引用实例：
```kotlin
private val mApp = LazyReference<RachelApplication>()
val app: RachelApplication by mApp
```

为什么需要自引用实例？因为你的业务代码可能发生在其他地方，为了能够在任意位置获取到你的应用程序实例和服务，我们有必要维护一个全局字段。
这个全局字段由`mApp`交付给`MyApplication`委托，而你只需要使用`app`在全局引用即可。

平台上下文请参看下一节。

随后在真正的实际入口处启动你的应用程序即可。

## 实际入口

### Android

在`Android`中，你需要创建一个`MainApplication`类和`MainActivity`类，这是原生安卓要求的，与框架无关。

你必须将它们分别继承自`ComposeApplication`和`ComposeActivity`，然后在`AndroidManifest.xml`中注册。

这样你的`Activity`便交给框架托管，你不需要写任何其他代码。

```kotlin
// MainActivity.kt
class MainActivity : ComposeActivity()
```

而在`MainApplication`中是真正的入口，你只需要将你的`MyApplication`重写instance便交给框架托管，你也不需要写任何其他代码。

```kotlin
// MainApplication.kt
class MainApplication : ComposeApplication() {
    override val instance = MyApplication(this)
}
```

!!! Warning
    注意，只有Android与其他平台不同，你不需要主动调用Application的run方法，只需要作为instance的值即可。

启动`Android`程序后会首先创建`MainApplication`并构造`MyApplication`，然后创建`MainActivity`展示UI。

### iOS

定义函数`MainViewController`并将结果返回到swift代码中(此处可以参看KMP的官方iOS项目示例)。

而`MainViewController`便是实际入口。

```kotlin
// MainViewController.kt
fun MainViewController() = MyApplication(PlatformContextDelegate).run()
```

### Desktop

定义main函数便是实际入口。

```kotlin
// main.kt
fun main() = MyApplication(PlatformContextDelegate).run()
```

### Web

定义main函数便是实际入口。

```kotlin
// main.kt
fun main() = MyApplication(PlatformContextDelegate).run()
```

## 平台侧代码

如果你有需要重写平台侧的配置，那么将`MyApplication`定义成抽象类，然后只需要将平台侧的调用改为`object`继承即可。

以桌面端为例，我需要设置一些标题栏、窗口配置、托盘等信息。

```kotlin
// main.kt
fun main() = object : MyApplication(PlatformContextDelegate) {
    // 重写后便可以设置平台特定的配置
    override val title: String = "MyApplication"
    override val icon: DrawableResource = Res.drawable.img_logo
    override val actionAlwaysOnTop: Boolean = true
    override val tray: Boolean = true
}.run()
```