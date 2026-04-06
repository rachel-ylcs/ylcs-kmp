### Service Startup

Application支持任意数量、任意类型、同步或异步、携带任意参数、绑定上下文、延迟初始化等多种功能支持的服务依赖启动。

所有启动项支持自动依赖排序、并发初始化。

#### 服务

服务（Startup）是一个包含若干类似功能的外部提供者，可以提供有限的接口给外部调用。
服务需要在上下文环境中初始化，可能携带若干参数或配置，可能需要在协程里构建。

服务可以根据是否在协程环境初始化来分为同步服务（SyncStartup）和异步服务（AsyncStartup）。
同步服务会在Application创建中服务构建时完成，异步服务会在Application的协程作用域里进行。
同步服务始终优先于异步服务完成初始化，任何服务失败都会导致整个Application抛出异常从而结束进程。

服务之间存在依赖关系，无依赖的服务始终最先初始化，其他服务会自动等待其依赖的服务初始化完成后才能初始化。

服务初始化分为默认初始化和延迟初始化，默认初始化通常发生在Application创建时立即调用；
而延迟初始化通常与强相关的上下文绑定，例如Android的Activity创建或Desktop的Window创建hwnd时调用。

服务的创建通常可以分为匿名服务、自定义服务、派生服务。

#### 匿名服务

在Application的init块中使用sync或async就可以创建一个匿名服务。

```kotlin
class MyApplication : PlatformApplication<MyApplication>(mApp, PlatformContext.Instance) {
    init {
        sync {
            println("同步启动1")
        }
        
        val v1 by async(id = "v1") {
            println("异步启动")
        }
        
        async(dependencies = listOf("v1")) {
            println("异步启动 $v1")
            mkdir("...")
        }
    }
    
    @Composable
    override fun Content() { }
}
```

匿名服务十分简洁，可以直接写在Application的init块中，注意所有创建的服务都是以工厂形式构造，它们不会立即触发服务初始化，而是等到Application初始化时才会在依赖解析中初始化。

匿名服务可以快速完成一些需要在Application启动时完成的任务，而不需要定义过多的复杂类。

匿名服务也可以配置依赖，通过by委托可以获取匿名服务对象，但这通常没什么意义，因为匿名服务不能提供可用的接口，对象始终为Startup基类。

#### 自定义服务

你可以创建一个服务类并继承自`SyncStartup`或`AsyncStartup`从而获得提供接口的服务。
除此之外，你还需要提供一个可以创建服务的工厂类。

服务工厂类需要为服务提供这些信息：
1. id: String，服务的唯一标识，通常可以用StartupID指定类名创建。
2. dependencies: List String，服务依赖，由ID列表构成，服务初始化会等待前置依赖完成初始化。
3. dispatcher: CoroutineContext?：协程调度器，同步服务必须设置为null，异步服务默认是CPU协程上下文。它决定了服务在协程中并发初始化时的初始上下文环境。
4. build：创建你的服务

```kotlin
class MyStartup(pool: StartupPool, args: Int) : AsyncStartup(pool) {
    class Factory(private val args: Int) : AsyncStartupFactory<MyStartup>() {
        override val id: String = StartupID<MyStartup>()
        override fun build(pool: StartupPool): MyStartup = MyStartup(pool, args)
    }
    
    var result: Boolean = false
    
    override suspend fun init() {
        result = someFunction(args)
    }
}
```

服务工厂的基类需要和服务的同步异步类型保持一致，可以使用StartupID方法快速为服务提供一个默认的ID，在build中根据相关参数来创建服务。

服务在初始化时回调用init方法，在这里你可以完成相关内容的处理、数据的绑定、第三方库的初始化或加载，同时将结果或对象保存在服务类中。

服务类可以暴露出你希望提供给外部的接口，外部有两种方式可以访问。

第一个是定义服务处：
```kotlin
class MyApplication : PlatformApplication<MyApplication>(mApp, PlatformContext.Instance) {
    val myStartup by startup(MyStartup.Factory(123))
    
    @Composable
    override fun Content() {
        println(myStartup.result)
    }
}
```

你可以在Application类中使用startup创建服务工厂，这一步不会触发服务创建，而是在Application初始化时才触发。
初始化完成后你就可以在其他地方使用服务提供的接口，例如访问其result或其他对象。

第二个是任意位置，例如非Application类内或其他服务希望访问其依赖的服务的接口。
你可以通过全局app实例访问相关服务, 服务池提供了下面四种方式获取具体的服务，分为是否可空或直接从类查找。

```kotlin
inline fun <reified S : Startup> require(id: String): S = startupMap[id] as S

inline fun <reified S : Startup> requireOrNull(id: String): S? = startupMap[id] as? S

inline fun <reified S : Startup> requireClass(): S = startupMap[StartupID<S>()] as S

inline fun <reified S : Startup> requireClassOrNull(): S? = startupMap[StartupID<S>()] as? S

fun test() {
    println(app.requireOrNull("MyStartup")?.result)
    
    val myStartup = app.requireClass<MyStartup>()
    println(myStartup.result)
}
```

外部访问时，如果服务初始化完成那么必定返回一个非空的服务对象。

#### 典型的应用模式

一个典型的应用模式可以如下形式：

```kotlin
class MyApplication : PlatformApplication<MyApplication>(mApp, PlatformContext.Instance) {
    // 这里定义一系列服务，服务之间具有依赖关系
    val startup1 by startup(MyStartup1.Factory(123))
    val startup2 by startup(MyStartup2.Factory(123, "111"))
    val startup3 by startup(MyStartup3.Factory(123, "111"))
    
    // 定义一些匿名服务，可以设置依赖
    val startup4 by async(dependencies = listOf(StartupID<MyStartup3>())) {
        // ...
    }
    
    init {
        // 这里可以直接同步调用相关初始化
        sync {
            syncSomething()
        }
        // 异步初始化可以由匿名服务完成
        async {
            asyncSomething()
        }
    }
    
    @Composable
    override fun Content() {
        
    }
}
```