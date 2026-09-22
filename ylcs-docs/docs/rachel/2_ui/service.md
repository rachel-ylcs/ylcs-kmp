# 应用服务与启动编排

`StartupPool` 管理应用级长生命周期能力。服务不是单纯的全局单例：它有明确 ID、依赖、调度器、两阶段初始化和反向销毁顺序。

## 服务模型

```kotlin
class SessionStartup(
    pool: StartupPool
) : AsyncStartup(pool) {
    lateinit var session: Session
        private set

    override suspend fun init() {
        session = restoreSession()
    }

    override suspend fun initLater() {
        // UI 宿主可用后再注册需要窗口的能力
    }

    override fun destroyBefore() {
        // UI 宿主销毁前解除绑定
    }

    override fun destroy() {
        session.close()
    }

    class Factory : AsyncStartupFactory<SessionStartup>() {
        override val id = StartupID<SessionStartup>()
        override val dependencies = listOf(StartupID<StartupKV>())
        override fun build(pool: StartupPool) = SessionStartup(pool)
    }
}
```

同步服务继承 `SyncStartup` / `SyncStartupFactory`；异步服务继承 `AsyncStartup` / `AsyncStartupFactory`，也可以覆写 `dispatcher`。

## 在应用中注册

```kotlin
val kv by startup(StartupKVFactory(configPath))
val session by startup(SessionStartup.Factory())

// 服务完成前允许 UI 先组合；读取结果可空且可观察
val musicPlayer by startupLazy(StartupMusicPlayer.Factory())

// 一次性小任务
private val pathsReady by async(id = "pathsReady") {
    dataPath.mkdir()
    cachePath.mkdir()
}
```

注册必须发生在启动池清理 factory 列表之前，通常就是应用构造阶段。应用启动后再调用 `startup()` 会抛出“pool already clean”。

`startup` 委托要求服务已可用，过早读取会失败；`startupLazy` 在异步初始化期间返回 `null`。Compose 应用使用状态化 StartupMap，服务写入后读取 `startupLazy` 的界面会重组。

## 依赖执行规则

启动池先拓扑排序并验证整张图，然后：

1. 按拓扑顺序构建全部同步服务。
2. 并发启动异步服务；每个任务在自己的依赖完成后才执行。
3. UI 宿主建立后，对所有已完成服务执行 `initLater`；尚在运行的服务会先等待自己的初始化任务。
4. 关闭时按逆拓扑顺序调用 `destroyBefore`，最终退出再按逆序调用 `destroy`。

同步服务不能依赖异步服务，因为同步构造阶段无法等待它。若一个能力需要 I/O，就把它整体定义为异步，或把同步外壳与异步准备拆成两个清晰阶段。

未知依赖和依赖环在启动前报错。初始化异常包装为 `StartupError` 并标明服务 ID 与阶段；不要在工厂中吞掉异常再留下半初始化对象。

## 内置服务

| 服务 | 用途与关键约束 |
| --- | --- |
| `StartupKV` | MMKV 跨平台键值存储，支持基本类型与 JSON；需要初始化目录 |
| `StartupConfig` | 基于 KV 的 Compose 状态配置委托；固定依赖 `StartupKV` |
| `StartupPicker` | 文件/目录选择；平台宿主能力在后期才可用 |
| `StartupExceptionHandler` | 统一记录未处理异常，可配置持久化回调 |
| `StartupUrlImage` | 初始化 Sketch、缓存、GIF/WebP 与图片质量策略 |
| `StartupCache` | 应用层可组合的缓存管理示例 |

真实应用的组合顺序可参考 `AbstractRachelApplication`：先确定 data/cache/config 路径并异步建目录，再注册缓存、选择器、图片、KV、配置和异常处理器；主题值直接从配置服务派生。

## 配置委托

`StartupConfig` 提供 Boolean、Int、Float、String、JSON、列表、映射等状态委托。键默认由属性名推导，并可带版本，适合“修改即持久化”的用户设置：

```kotlin
class AppConfig(pool: StartupPool) : StartupConfig(pool) {
    var darkMode by boolean(default = false)
    var fontScale by float(default = 1f)
}
```

属性名也是存储键的一部分，重命名需要迁移。复杂结构要考虑旧 JSON 的默认值和版本，而不是只依赖 `ignoreUnknownKeys`。

## 服务边界

适合 Startup 的对象：数据库、持久配置、播放器、共享缓存、全局客户端、系统集成。以下内容不适合：

- 只服务一个页面的加载状态；放入 `Screen`。
- 只在一个组合可见时存在的动画或订阅；使用 Compose effect。
- 无状态工具函数；保留为普通对象/函数。
- 必须持有 Activity 的永久对象；保存 Provider，并在需要时读取当前宿主。

服务图的价值不在“统一成单例”，而在把初始化先后、并行机会和资源所有权变成可检查的数据。
