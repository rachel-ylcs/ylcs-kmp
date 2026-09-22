# 核心库

`ylcs-module/core` 放置不依赖 Compose 的通用能力；`foundation` 在其上加入平台上下文、文件系统、网络与启动编排。它们可以被 UI、命令行工具和 Kotlin/Native 服务端共同使用。

## 平台模型

`platform` 是当前运行目标的 `Platform` 值：

```kotlin
enum class Platform {
    Android, IOS,
    Windows, Linux, MacOS,
    WebWasm, WebJs,
    AndroidNative, WindowsNative, LinuxNative, MacOSNative
}
```

框架还定义了 `Phone`、`Desktop`、`DesktopNative`、`Web`、`Native` 等平台组。可以用集合判断，也可以让代码只在目标平台运行：

```kotlin
Platform.Web.use {
    // WebJs 或 WebWasm
}

Platform.Desktop.useNot {
    // 非 JVM Desktop
}
```

平台判断适合小范围行为差异。只要需要平台类型、生命周期或系统 API，就应使用 `expect/actual`，避免在共享代码里堆叠分支。

## 协程约定

`Coroutines` 统一了项目中的上下文切换和异常处理：

```kotlin
val value = Coroutines.io { readFromDisk() }
val parsed = Coroutines.cpu { parse(value) }
Coroutines.main { state.value = parsed }

Coroutines.timeout(3_000) {
    fetchSomething()
}
```

常用能力包括：

- `with(context)`、`main`、`cpu`、`io`：切换到约定调度器。
- `timeout(milliseconds)`：使用毫秒整数作为超时。
- `isActive`、`requireActive`：显式检查取消状态。
- `sync(SyncFuture)`：等待框架的单结果同步对象。
- `catching*`：把普通异常转为值或回调，同时保留 `CancellationException` 的传播。

最后一点很重要：取消不是业务失败。自己写 `catch (Throwable)` 时也必须重新抛出取消异常，否则页面离开或应用关闭后，旧任务仍可能继续工作。

## JSON

全局 `Json` 配置启用了 `ignoreUnknownKeys`，协议演进时客户端可以忽略新增字段。扩展函数把序列化和 `JsonElement` 转换简化为：

```kotlin
val text = profile.toJsonString()
val profile = text.parseJsonValue<Profile>()
val element = profile.toJson()
val restored = element.to<Profile>()
```

`makeObject` 和 `makeArray` 用于构造协议数据。C/S 引擎正是用 JSON 数组保持参数顺序。对持久化数据仍建议显式加默认值并规划版本迁移；忽略未知字段不能解决字段被删除或类型被改变的问题。

## URI

`Uri` 是可序列化的跨平台结构：

```kotlin
val uri = Uri.parse("rachel://app/openSong?id=demo")
val id = uri?.params?.get("id")

val encoded = Uri.encodeUri("https://example.com/a b")
val decoded = Uri.decodeUri(encoded)
```

结构字段包含 `scheme`、`host`、`port`、`path` 和 `query`，`params` 从查询串派生。解析器要求完整的 `scheme://` 形式；应用内部深链也应遵守这一点。`Scheme` 收录常用协议名，文件模块还提供平台 URI 转换。

## 文件系统

跨平台 `File` 同时提供挂起和同步操作：

```kotlin
val cacheFile = File(PlatformFileSystem.cachePath, "payload.json")

cacheFile.writeText(payload.toJsonString())
val restored = cacheFile.readText()

if (cacheFile.exists()) {
    cacheFile.rename("payload.old.json")
}
```

主要能力包括元数据、`mkdir`、`list`、移动、重命名、递归删除、字节/文本读写，以及基于 `kotlinx-io` 的 `rawSource`、`rawSink`、`read`、`write`。大文件应通过 Source/Sink 流式处理，不要先整体读成 `ByteArray`。

`PlatformFileSystem` 暴露：

- `appPath`：应用/可执行文件相关位置。
- `dataPath`：需要跨运行保留的数据。
- `cachePath`：可重新生成的缓存。
- 当前目录、路径分隔符和设置当前目录的能力。

URI 分三类：普通路径用 `RegularUri`；Android 文档提供器使用 `ContentUri`；iOS 沙盒路径使用 `SandboxUri`。`ImplicitUri` 表示“由平台解释”的输入，不应在共享代码里假设它总能转换成本地绝对路径。

## 依赖分析

`DependencyAnalyzer` 接受项目、键和依赖列表，输出拓扑排序结果及传递依赖表。它会区分：

- `UnknownDependencyError`：引用了不存在的键。
- `LoopDependencyError`：依赖图存在环。

启动系统在初始化之前使用它，因此错误会尽早暴露，而不是在某个服务读取空值时才失败。相同工具也适合插件、流水线阶段和任务编排。

## 常用小工具

核心库还包含几组贯穿项目的辅助类型：

- `LazyReference` / `BaseLazyReference`：只能初始化一次的延迟引用。
- 原子值包装与 `Mutex`：屏蔽平台实现差异。
- 日期格式和 `DateEx` 当前时间辅助。
- 集合增删、批量替换和空值处理扩展。
- `Data.Success` / `Data.Failure`：显式承载成功值或异常。

这些工具的目标是统一语义，不是隐藏所有平台差异。涉及安全边界、文件权限、时钟精度或线程亲和性时，仍应阅读对应平台的 `actual` 实现。
