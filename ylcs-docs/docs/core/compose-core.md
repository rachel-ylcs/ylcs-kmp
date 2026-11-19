---
description: compose/core 提供的状态工厂、延迟引用、一次执行标记、窗口生命周期与图形辅助。
---

# Compose 核心

<p class="rc-lead"><code>ylcs-module/compose/core</code> 是主题、组件与页面体系的共同底座。它不定义具体控件，而是提供状态创建、生命周期信号、窗口可见性与平台上下文访问。</p>

## 状态工厂

状态辅助函数位于 `love.yinlin.compose.extension`：

```kotlin title="状态委托"
var count by rememberValueState(0)
var enabled by rememberFalse()
var selected by rememberNull<String>()
val title by rememberDerivedState { "项目数量：$count" }
```

| 接口 | 用途 |
| --- | --- |
| `rememberState { ... }` | 创建普通可变状态 |
| `rememberRefState { ... }` | 使用引用相等策略的状态 |
| `rememberTrue()`、`rememberFalse()` | Boolean 状态 |
| `rememberNull<T>()`、`rememberRefNull<T>()` | 可空状态 |
| `rememberValueState(...)` | Int、Long、Float、Double 状态 |
| `rememberDerivedState { ... }` | 基于快照读取的派生状态 |

这些函数使用 `remember`，不自动提供进程恢复能力。需要跨配置变化保留的小型界面状态，仍应使用合适的 `rememberSaveable` 与 Saver。

## 延迟状态引用

`LazyStateReference<T>` 在首次 `init(value)` 后保存引用，并通过 Compose 状态通知读取方。后续 `init` 不会覆盖已保存值。

```kotlin title="应用实例"
private val applicationReference = LazyStateReference<MainApplication>()
val application: MainApplication by applicationReference
```

| 特性 | 说明 |
| --- | --- |
| 首次初始化 | 由平台入口注入 |
| 后续初始化 | 不覆盖已保存值 |
| 读取前未初始化 | 失败 |
| 适用对象 | 应用实例等首次初始化后保持稳定的对象 |

持续变化的业务数据应使用普通可变状态，而不是延迟引用。

## 一次执行标记

`LaunchFlag` 使用原子标记区分首次与后续调用：

```kotlin title="LaunchFlag"
private val starter = LaunchFlag()

fun prepare() {
    starter(
        update = { refreshExistingState() },
        init = { initializeOnce() },
    )
}
```

标记在调用 `init` 前写入；即使 `init` 抛出异常，也不会自动重试。作用域为当前 `LaunchFlag` 实例。异步工作应通过具有明确所有者的协程域启动。

## 窗口生命周期

窗口状态辅助位于 `love.yinlin.compose.window`，基于当前 `LocalLifecycleOwner` 读取生命周期：

| API | 语义 |
| --- | --- |
| `rememberFocusWindowState()` | 生命周期是否达到 `RESUMED` |
| `FocusWindowEffect` | 聚焦状态副作用 |
| `rememberOffScreenWindowState()` | 在 `START` 阶段为 `true`，停止或离开组合时为 `false` |
| `OffScreenWindowEffect` | 离屏状态副作用 |

名称中的 `OffScreen` 不表示 `true` 对应离屏，应按实际值语义判断。主窗口、独立窗口与页面导航条目可能拥有不同的 `LifecycleOwner`，观察范围也随之不同。

## 可移动内容

`rememberMovableContent` 封装 `movableContentOf`，适合在布局位置变化时保留同一段组合及其状态：

```kotlin title="可移动内容"
val content = rememberMovableContent {
    ProfilePanel(state)
}

if (device.type == Device.Type.LANDSCAPE) {
    Row { Aside(); content() }
} else {
    Column { content() }
}
```

布局分支应共享业务状态与稳定标识，避免因横竖布局切换重复创建页面模型或发起请求。原生视图的重挂载能力受宿主限制，仍需遵守[平台原生组件](../ui/adaptive.md)的复用与释放契约。

## 图形与上下文

`graphics`、`extension` 与平台源集提供颜色、几何、绘制及平台图像转换。公共组件应优先使用 Compose 公共类型，在与原生 API 交互的边界执行平台转换。

`LocalPlatformContext.current` 返回 `PlatformContextProvider`，由 `Application.ComposedLayout()` 提供：

```kotlin title="上下文"
@Composable
fun ContextContent() {
    val provider = LocalPlatformContext.current
    val rawContext = provider.rawContext
}
```

组合树之外的业务逻辑应显式接收所需能力，避免把 CompositionLocal 当作隐藏的全局服务入口。

## 分层选择

| 能力 | 模块 |
| --- | --- |
| 通用 Kotlin、协程与序列化 | `core` |
| 文件、网络与平台基础能力 | `foundation/*` |
| Compose 状态与底层图形辅助 | `compose:core` |
| 颜色、排版、尺寸与动画约定 | `compose:theme` |
| 可交互组件与布局 | `compose:ui` |
| 页面模型与导航 | `compose:screen` |

## 实际状态声明

Gallery 的图片外观示例使用状态委托保存整数选择、Boolean 开关与 Float 透明度：

```kotlin title="ImagePage.kt"
var quality by rememberValueState(1)
var circular by rememberFalse()
var tint by rememberFalse()
var fadeIn by rememberTrue()
var alpha by rememberValueState(1f)
var version by rememberValueState(0)
```

这些状态属于组合位置，不能据此推断图片缓存或页面业务数据会跨进程保留。

## 下一步

继续阅读[基础能力模块](capabilities.md)，了解文件、网络、启动与数据处理的完整能力面。
