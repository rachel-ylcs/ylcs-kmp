# 应用服务与启动编排

`StartupPool` 管理应用级服务的注册、依赖解析、初始化和销毁。服务通过 ID 建立依赖关系，并明确同步构建或异步初始化方式。

## 服务与工厂

```kotlin
class SessionStartup(pool: StartupPool) : AsyncStartup(pool) {
    override suspend fun init() {
        restoreSession()
    }

    override suspend fun initLater() {
        prepareHostIntegration()
    }

    override fun destroyBefore() {
        detachHostIntegration()
    }

    override fun destroy() {
        closeSession()
    }

    class Factory : AsyncStartupFactory<SessionStartup>() {
        override val id: String = StartupID<SessionStartup>()
        override val dependencies: List<String> = [StartupID<StartupKV>()]
        override fun build(pool: StartupPool) = SessionStartup(pool)
    }
}
```

上例的会话操作由业务实现。同步服务继承 `SyncStartup`，由 `SyncStartupFactory` 构建；其 `init()` 是不可覆写的空实现，必要的同步初始化发生在构造或工厂构建阶段。

异步服务继承 `AsyncStartup`，在挂起 `init()` 中完成准备工作。`AsyncStartupFactory.dispatcher` 默认使用 `cpuContext`，可按任务性质覆写。

## 注册与读取

在应用构造阶段注册服务：

```kotlin
val kv by startup(StartupKVFactory(configPath))
val session by startup(SessionStartup.Factory())
val player by startupLazy(StartupMusicPlayer.Factory())
```

`startup` 读取时要求服务已经存在；异步初始化尚未结束时不能提前读取。`startupLazy` 返回可空服务，完成前为 `null`。Compose 应用使用 `StateStartupMap`，服务写入后可通知观察该委托的界面。

注册接口还支持显式附加依赖：

```kotlin
val service by startup(
    CustomStartup.Factory(),
    dependencies = [StartupID<StartupKV>()]
)
```

启动池解析工厂列表后停止接受注册。运行过程中新增 `startup` 或 `startupLazy` 声明会失败。

## 执行规则

1. `initPool` 使用 `DependencyAnalyzer` 对服务拓扑排序，并检查未知依赖与依赖环。
2. 按拓扑顺序构建同步服务，同步服务的依赖必须全部属于同步服务。
3. 调度异步服务，各任务等待自己的异步依赖完成后再构建并调用 `init()`。
4. `initPoolLater` 并行调度服务的 `initLater()`，等待该服务已有的初始化任务。
5. `destroyPoolBefore` 和 `destroyPool` 分别按逆拓扑顺序清理当前已经创建的服务。

第二阶段不是全体服务的统一屏障，也不自动按照依赖顺序完成所有 `initLater()`。若服务需要另一服务的后期准备结果，应显式设计同步协议。

初始化和清理中的普通异常通常包装为包含服务 ID 与阶段的 `StartupError`。取消异常保留传播。当前销毁循环遇到异常可提前结束，因此各服务应保持清理过程可靠，避免将关键数据写入完全延后到应用退出。

## 一次性任务

```kotlin
private val preparePaths by async(
    id = "preparePaths"
) {
    dataPath.mkdir()
    cachePath.mkdir()
}

private val prepareCache by async(
    id = "prepareCache",
    dependencies = ["preparePaths"]
) {
    restoreCache()
}
```

`sync` 注册普通同步动作，`async` 注册挂起动作。任务声明的先后顺序不替代依赖关系；依赖目录或文件的服务应显式声明对应任务 ID。

## 内置能力

| 模块或服务 | 作用 |
| --- | --- |
| `StartupKV` / `compose:startup:mmkv-kmp` | 跨平台键值存储 |
| `StartupConfig` / `compose:startup:config` | 基于 KV 的可观察持久配置 |
| `StartupPicker` / `compose:startup:picker` | 文件与目录选择等平台交互 |
| `StartupExceptionHandler` / `compose:startup:exception` | 未处理异常收集与自定义记录 |
| `ImageManager` / `compose:components:image` | 图片解码与缓存；由应用持有初始化及关闭生命周期 |
| `compose:cache` | 内存状态缓存与磁盘缓存抽象 |

`ylcs-app/app/global` 中的 `StartupCache` 与 `StartupImage` 是业务应用的服务组合示例，后者使用同步工厂初始化 `ImageManager`，在 `destroy()` 中关闭管理器；它们不是基础库导出的内置 Startup。配置服务与异常记录等依赖可参考 `AbstractRachelApplication`，新增依赖时仍需核对真实图关系。

## 配置委托

```kotlin
class AppConfig(pool: StartupPool) : StartupConfig(pool) {
    var darkMode by booleanState(default = false)
    var fontScale by floatState(default = 1f)
}

val config by startup(StartupConfig.custom(::AppConfig))
```

`StartupConfig.custom` 固定依赖 `StartupKV`。配置接口包括 `booleanState`、`intState`、`longState`、`floatState`、`doubleState`、`stringState`、`enumState`、`jsonState`、`listState`、`mapState` 与 `cacheState`。

配置键由属性名与版本参数按实现组合。重命名属性或调整版本会影响读取的存储键，应明确旧值迁移或重置规则。复杂 JSON 的兼容性由配置模型负责。

## 生命周期边界

数据库、播放器、持久配置和全局客户端适合应用服务。页面加载状态由 `ScreenModel` 管理，组合可见期间的订阅由 Compose effect 管理，无状态逻辑保留为普通函数或对象。

`Startup` 本身不提供自动独立协程域。服务长期任务应绑定明确的应用级所有者，并在服务结束时停止。

## 业务图片服务的完整封装

`app/global` 中的 `StartupImage` 展示了同步服务的常见写法：工厂构造时初始化依赖，应用销毁时关闭它。该类型属于业务模块：

示例来源：[StartupImage.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/app/global/src/commonMain/kotlin/love/yinlin/startup/StartupImage.kt#L11)。

```kotlin
@Stable
class StartupImage(pool: StartupPool, appName: String, configuration: ImageConfiguration) : SyncStartup(pool) {
    class Factory(
        private val appName: String = "Rachel",
        private val configuration: ImageConfiguration = ImageConfiguration(),
    ) : SyncStartupFactory<StartupImage>() {
        override val id: String = StartupID<StartupImage>()
        override fun build(pool: StartupPool): StartupImage = StartupImage(pool, appName, configuration)
    }

    init {
        ImageManager.initialize(pool.rawContext, appName, configuration)
    }

    suspend fun clearCache() = ImageManager.clearCache()

    override fun destroy() = ImageManager.shutdown()
}
```

基础库使用方可以采用相同的生命周期模式，并按依赖关系注册工厂。`StartupImage` 不会让 `compose:startup:config`、MMKV 或 Picker 自动获得桌面 Native 支持；这几个可选模块的目标需要分别核对。依赖执行顺序与逆序销毁的测试见 `foundation/startup` 的 `TestStartupDependencies`。

## 源码入口

- `ylcs-module/foundation/startup/src/commonMain/kotlin/love/yinlin/foundation/StartupPool.kt`
- `ylcs-module/compose/app/src/commonMain/kotlin/love/yinlin/compose/StateStartupMap.kt`
- `ylcs-module/compose/startup/config/src/commonMain/kotlin/love/yinlin/startup/StartupConfig.kt`
