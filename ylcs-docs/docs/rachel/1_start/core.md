# 核心库

`ylcs-module/core` 提供不依赖 Compose 的通用能力。平台上下文、文件系统、网络与启动编排分别由 `foundation` 下的独立模块提供。

## 平台识别

`love.yinlin.platform.platform` 返回当前运行平台。`Platform` 的枚举值包括 Android、IOS、Windows、Linux、MacOS、WebWasm、WebJs、WindowsNative、LinuxNative 和 MacOSNative。

`Phone`、`Desktop`、`DesktopNative` 与 `Web` 是平台数组，通过 `Platform` 的辅助函数使用：

```kotlin
Platform.use(*Platform.Web) {
    // JavaScript 或 WasmJS 平台逻辑。
}

Platform.useNot(*Platform.Desktop) {
    // JVM Desktop 以外的平台逻辑。
}

val isPhone = Platform.contains(*Platform.Phone)
```

平台判断适合小范围行为分支。需要使用平台专属类型或系统 API 时，应将实现放入相应平台源集。

## 协程与异常处理

`Coroutines` 提供上下文切换、毫秒超时和单结果等待：

```kotlin
val text = Coroutines.io { readContent() }
val model = Coroutines.cpu { parseContent(text) }
Coroutines.main { updateState(model) }

Coroutines.timeout(3_000) {
    fetchContent()
}
```

`mainContext`、`cpuContext` 与 `ioContext` 根据平台提供实际调度上下文。`Coroutines.with(context)` 用于恢复或指定其他协程上下文。阻塞 I/O 应置于适合的平台调度器中。

异常辅助函数位于 `love.yinlin.extension`：

| 函数 | 普通异常处理 |
| --- | --- |
| `catching` | 忽略普通异常 |
| `catchingError` | 成功返回 `null`，失败返回异常 |
| `catchingNull` | 失败返回 `null` |
| `catchingDefault` | 失败返回指定值或异常转换结果 |
| `throwing` | 转换普通异常并重新抛出 |
| `cleaning` | 在 `finally` 中执行清理 |

`catching` 系列与 `throwing` 均重新抛出 `CancellationException`。协程取消不会被转换为普通失败值。

## JSON 与序列化

全局 `Json` 关闭格式化输出并启用 `ignoreUnknownKeys`。常用转换如下：

```kotlin
val encoded = profile.toJsonString()
val restored = encoded.parseJsonValue<Profile>()
val element = profile.toJson()
val decoded = element.to<Profile>()

val payload = makeObject {
    "name" with "示例"
    "enabled" with true
    arr("items") {
        add("first")
        add("second")
    }
}
```

`String.parseJson` 返回 `JsonElement`；`Object`、`Array`、`Int`、`String` 等扩展用于读取已知结构，带 `Null` 后缀的扩展用于允许缺失或类型不匹配的读取。

序列化接口及适配器集中在 `SerializerEx.kt`。持久化与协议模型应明确默认值、字段名和版本迁移规则；忽略未知字段仅解决新增未知字段的读取问题。

## URI

`Uri` 是可序列化的公共结构，字段包括 `scheme`、`host`、`port`、`path` 与 `query`：

```kotlin
val uri = Uri.parse("rachel://app/open?id=demo")
val id = uri?.params?.get("id")
val encodedValue = Uri.encodeUri("含空格的值")
val decodedValue = Uri.decodeUri(encodedValue)
```

解析器要求 `scheme://` 形式。`params` 按 `&` 和 `=` 拆分查询串，不自动执行百分号解码；需要解码时应显式调用 `decodeUri`。`encodeUri` 对传入字符串中的保留字符编码，适合编码查询参数值，不应直接用于替代完整 URL 的结构化构造。

## 文件系统

`File` 与 `PlatformFileSystem` 位于 `love.yinlin.fs`，由 `foundation:filesystem` 提供：

```kotlin
val cachePath = PlatformFileSystem.cachePath(rawContext, "example")
cachePath.mkdir()

val file = File(cachePath, "payload.json")
file.writeText(profile.toJsonString())
val text = file.readText()
```

平台目录通过 `appPath(context, appName)`、`dataPath(context, appName)` 与 `cachePath(context, appName)` 获取，返回 `File`。数据目录用于持久内容，缓存目录用于可重建内容。安装位置不应直接作为默认用户数据目录。

文件操作包括元数据、目录遍历、创建、删除、复制、移动及字节和文本读写。带 `Sync` 后缀的接口执行同步操作；挂起接口按实现切换调度上下文。`read`、`write` 及 Source/Sink 接口适合流式处理大文件。

`RegularUri`、Android 的 `ContentUri` 与 iOS 的 `SandboxUri` 保留不同的平台文件访问语义。`ImplicitUri` 不保证可转换为普通绝对路径。

## 其他公共能力

| 源码 | 能力 |
| --- | --- |
| `collection/DependencyAnalyzer.kt` | 拓扑排序、传递依赖、未知依赖与依赖环检查 |
| `extension/EnumEx.kt` | 枚举查找、默认值与集合操作 |
| `extension/DateEx.kt` | 时间与格式辅助 |
| `extension/Lazy.kt`、`Reference.kt` | 延迟引用和属性委托 |
| `io/ByteArrayIO.kt` | 整数、字符串及大小端字节读写 |
| `io/CRC32.kt`、`Adler32.kt` | 校验值计算 |
| `encoding/*` | UTF、GBK、ASCII 等编码转换 |
| `concurrent/*` | 原子值、锁与互斥封装 |
| `data/Data.kt` | `Data.Success` 与 `Data.Failure` 结果模型 |

## 源码入口

核心实现位于 `ylcs-module/core/src/commonMain/kotlin/love/yinlin/`；文件系统位于 `ylcs-module/foundation/filesystem/src/`。涉及平台权限、路径或线程语义时，应同时核对目标平台的实现。
