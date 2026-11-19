---
description: Rachel core 提供的平台识别、协程、异常、序列化、URI、文件、字节与依赖分析能力。
---

# 基础核心

<p class="rc-lead"><code>ylcs-module/core</code> 不依赖 Compose，也不假设界面存在。它提供的是一组跨平台基础语义：我在什么平台运行、工作应该在哪里执行、失败如何表达、数据如何编码。</p>

## 平台识别

`love.yinlin.platform.platform` 返回当前运行平台。`Platform` 同时提供平台分组，适合小范围行为分支：

```kotlin title="平台判断"
Platform.use(*Platform.Web) {
    // JavaScript 或 WasmJS 平台逻辑。
}

Platform.useNot(*Platform.Desktop) {
    // JVM Desktop 以外的平台逻辑。
}

val isPhone = Platform.contains(*Platform.Phone)
```

| 分组 | 典型成员 |
| --- | --- |
| `Phone` | Android、iOS |
| `Desktop` | JVM Desktop |
| `DesktopNative` | Windows、Linux、macOS Native |
| `Web` | JS、WasmJS |

!!! warning "平台判断不替代平台实现"
    需要平台专属类型或系统 API 时，应将实现放入对应源集。不要在 `commonMain` 中通过平台分支引用平台类型。

## 协程调度

`Coroutines` 提供上下文切换、超时与不可取消等待：

```kotlin title="调度"
val text = Coroutines.io { readContent() }
val model = Coroutines.cpu { parseContent(text) }
Coroutines.main { updateState(model) }

Coroutines.timeout(3_000) {
    fetchContent()
}
```

| API | 语义 |
| --- | --- |
| `mainContext`、`cpuContext`、`ioContext` | 平台提供的调度上下文 |
| `Coroutines.with(context)` | 恢复或指定其他上下文 |
| `Coroutines.mainImmediate` | 主线程立即调度语义 |
| `Coroutines.wait` | 切入 `NonCancellable`，用于必须完成的清理 |

浏览器中的 IO / CPU 上下文不等同于独立 Worker，不能仅凭切换上下文推断计算会并行执行。

## 异常处理

异常辅助函数位于 `love.yinlin.extension`：

| 函数 | 行为 |
| --- | --- |
| `catching` | 忽略普通异常 |
| `catchingError` | 成功返回 `null`，失败返回异常 |
| `catchingNull` | 失败返回 `null` |
| `catchingDefault` | 失败返回指定值或转换结果 |
| `throwing` | 转换普通异常并重新抛出 |
| `cleaning` | 在 `finally` 中执行清理 |

```kotlin title="异常通道"
val error = catchingError {
    saveProfile(profile)
}

if (error != null) {
    slot.tip.error(error.message)
}
```

`catching` 系列与 `throwing` 都会重新抛出 `CancellationException`。协程取消不会被折叠成普通失败值。

## JSON 与序列化

全局 `Json` 关闭格式化输出并启用 `ignoreUnknownKeys`。常用转换：

```kotlin title="JSON"
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

| API | 用途 |
| --- | --- |
| `toJsonString` | 序列化为字符串 |
| `parseJsonValue<T>` | 从字符串反序列化 |
| `toJson` / `to<T>` | 与 `JsonElement` 互转 |
| `makeObject` | 构建 JSON 对象 |
| `String.parseJson` | 返回 `JsonElement` |

持久化与协议模型应明确默认值、字段名与版本迁移规则。忽略未知字段只解决“新增字段”的读取，不解决语义变化。

## URI

`Uri` 是可序列化结构，包含 `scheme`、`host`、`port`、`path` 与 `query`：

```kotlin title="URI"
val uri = Uri.parse("rachel://app/open?id=demo")
val id = uri?.params?.get("id")

val encoded = Uri.encodeUri("含空格的值")
val decoded = Uri.decodeUri(encoded)
```

解析器要求 `scheme://` 形式。`params` 按 `&` 与 `=` 拆分查询串，不自动执行百分号解码；需要解码时显式调用 `decodeUri`。

## 文件系统

`File` 与 `PlatformFileSystem` 位于 `love.yinlin.fs`，由 `foundation:filesystem` 提供：

```kotlin title="应用目录"
val cachePath = PlatformFileSystem.cachePath(rawContext, "hello")
cachePath.mkdir()

val file = File(cachePath, "payload.json")
file.writeText(profile.toJsonString())
val text = file.readText()
```

| 目录 | 用途 |
| --- | --- |
| `appPath(context, appName)` | 安装或应用路径 |
| `dataPath(context, appName)` | 需要长期保留的用户数据 |
| `cachePath(context, appName)` | 可重建的缓存 |

获取路径不代表目录已经创建；调用方应按用途创建。浏览器没有进程工作目录，`getCurrentDirectory()` 返回 `null`。

`RegularUri`、Android 的 `ContentUri` 与 iOS 的 `SandboxUri` 保留不同的访问语义。`ImplicitUri` 不保证可以转换为普通绝对路径。

## 字节读写

`ByteArrayIO` 提供整数、字符串与大小端读写：

```kotlin title="字节缓冲"
val raw = ByteArray(4)
val wrapped = ByteArrayIO(raw)

wrapped.writeInt(0, 0x01020304)
assertContentEquals([1, 2, 3, 4], raw)

val created = ByteArrayIO(size = 2)
assertEquals(2, created.size)
```

`ByteArrayIO(raw)` 不复制输入，写入会修改原数组。读写索引与长度必须处于缓冲区范围内。显式指定 `Endian.LITTLE` 时，整数按小端写入。

## 结果模型

`Data` 表示成功或失败：

```kotlin title="Data"
when (val result = loadProfile()) {
    is Data.Success -> render(result.data)
    is Data.Failure -> showError(result.throwable)
}
```

`Data.map` 只转换成功数据，保留失败通道。需要区分网络、鉴权或协议错误时，优先使用显式结果，而不是把所有失败折叠为 `null`。

## 其他常用能力

| 源码 | 能力 |
| --- | --- |
| `collection/DependencyAnalyzer.kt` | 拓扑排序、传递依赖、未知依赖与依赖环检查 |
| `extension/EnumEx.kt` | 枚举查找、默认值与集合操作 |
| `extension/DateEx.kt` | 时间与格式辅助 |
| `extension/Lazy.kt`、`Reference.kt` | 延迟引用与属性委托 |
| `io/CRC32.kt`、`Adler32.kt` | 校验值计算 |
| `encoding/*` | UTF、GBK、ASCII 等编码转换 |
| `concurrent/*` | 原子值、锁与互斥封装 |

!!! tip "使用框架已有的语法糖"
    枚举、序列化、日期、字节读写与异常处理都有统一入口。新增通用工具前，先确认 `core` 是否已经提供。

## 下一步

基础能力之上是 Compose 状态层。继续阅读 [Compose 核心](compose-core.md)。
