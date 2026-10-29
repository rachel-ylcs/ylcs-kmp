# Compose 核心

`ylcs-module/compose/core` 为上层主题与组件提供状态扩展、图形辅助、窗口状态和平台上下文访问能力。

## 状态工厂

状态辅助函数位于 `love.yinlin.compose.extension`：

```kotlin
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

这些函数使用 `remember`，不自动提供进程恢复能力。需要保存小型界面状态时，应选择合适的 `rememberSaveable` 与 Saver；业务数据由页面或应用服务管理。

## 延迟状态引用

`LazyStateReference<T>` 实现 `BaseLazyReference<T>`，首次 `init(value)` 后保存引用，并通过 Compose 状态通知读取方。后续 `init` 不覆盖已保存值。

```kotlin
private val applicationReference = LazyStateReference<MainApplication>()
val application: MainApplication by applicationReference
```

此类型用于应用实例等首次初始化后保持稳定的对象。初始化前读取委托值会失败。持续变化的业务数据应使用普通可变状态。

## 一次执行标记

`LaunchFlag` 使用原子标记区分首次与后续调用：

```kotlin
private val starter = LaunchFlag()

fun prepare() {
    starter(
        update = { refreshExistingState() },
        init = { initializeOnce() }
    )
}
```

标记在调用 `init` 前写入；即使 `init` 抛出异常，也不会自动重试。其作用域为当前 `LaunchFlag` 实例。`init` 和 `update` 是普通函数，异步工作需通过具有明确所有者的协程域启动。

## 生命周期状态

窗口状态辅助位于 `love.yinlin.compose.window`，基于当前 `LocalLifecycleOwner` 读取生命周期：

- `rememberFocusWindowState()` 与 `FocusWindowEffect` 反映生命周期是否达到 `RESUMED`。
- `rememberOffScreenWindowState()` 与 `OffScreenWindowEffect` 在 `START` 阶段给出 `true`，在停止或离开组合时给出 `false`。
- 主窗口与独立窗口各自提供的 LifecycleOwner 决定观察范围；页面内读取时还需考虑 Navigation 条目的 Owner。

`OffScreen` 系列名称不表示 `true` 对应离屏。应按实际值的语义判断可见期间的工作，并关注当前 LifecycleOwner 的归属。页面 `initialize`、策略回调 `resume` 与这些组合期生命周期信号具有不同用途，见[导航](../2_ui/navigation.md)。

## 可移动内容

`rememberMovableContent` 封装 `movableContentOf`，适合在布局位置变化时保留同一组合中的状态。布局分支应共享业务状态和稳定标识，避免因横竖布局切换重复创建页面模型或发起请求。

原生 View 的重挂载能力受宿主限制。涉及平台视图时，仍需遵守[平台组件](../2_ui/platform_component.md)的复用与释放契约。

## 图形与上下文

`graphics`、`extension` 和平台源集提供颜色、几何、绘制及平台图像转换。公共组件应优先使用 Compose 公共类型，在与原生 API 交互的边界执行平台转换。

`LocalPlatformContext.current` 返回 `PlatformContextProvider`，由 `Application.ComposedLayout()` 提供。具体用法见[平台上下文](../2_ui/context.md)。深链中继位于 `compose:screen` 的 `DeepLink`，见[导航](../2_ui/navigation.md)。

## 分层选择

| 能力 | 模块 |
| --- | --- |
| 通用 Kotlin、协程和序列化 | `core` |
| 文件、网络和平台基础能力 | `foundation/*` |
| Compose 状态和底层图形辅助 | `compose:core` |
| 颜色、排版、尺寸与动画约定 | `compose:theme` |
| 可交互组件与布局 | `compose:ui` |
| 页面模型与导航 | `compose:screen` |

## Gallery 中的状态声明

图片外观示例使用状态委托分别保存整数选择、Boolean 开关与 Float 透明度。以下声明位于 `AppearanceExamples()` 的组合作用域中：

示例来源：[ImagePage.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/gallery/src/commonMain/kotlin/love/yinlin/page/ImagePage.kt#L309)。

```kotlin
var quality by rememberValueState(1)
var circular by rememberFalse()
var tint by rememberFalse()
var fadeIn by rememberTrue()
var alpha by rememberValueState(1f)
var version by rememberValueState(0)
```

`rememberValueState` 按参数类型选择状态；委托读写还需导入 Compose 的 `getValue` / `setValue`。这些状态属于组合位置，不能据此推断图片缓存或页面业务数据会跨进程保留。

## 源码入口

- `ylcs-module/compose/core/src/commonMain/kotlin/love/yinlin/compose/extension/StateEx.kt`
- `ylcs-module/compose/core/src/commonMain/kotlin/love/yinlin/compose/LaunchFlag.kt`
- `ylcs-module/compose/core/src/commonMain/kotlin/love/yinlin/compose/window/`
