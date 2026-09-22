# Compose 核心

`love.yinlin.compose:core` 位于原始 Compose 与 Rachel 主题/UI 之下。它提供状态引用、图形转换、窗口与生命周期辅助，但不引入 Material 组件。多数应用会通过 `compose:theme` 或 `compose:ui` 间接得到它。

## 状态引用

项目既需要普通延迟引用，也需要在初始化时触发重组的引用：

```kotlin
private val currentUserRef = LazyStateReference<User>()
val currentUser by currentUserRef

// 在生命周期内只初始化一次
currentUserRef.init(user)
```

`LazyStateReference` 适合应用实例、晚到达但之后稳定的服务等“一次赋值”对象。可持续变化的数据仍应使用 `MutableState`；不要通过反复创建延迟引用模拟普通状态。

常用状态工厂包括：

```kotlin
var count by rememberValueState(0)
var selected by rememberNullState<Item>()
val derived by rememberDerivedState { count * 2 }
```

库还为 `Color`、`Dp`、`Offset`、`Size` 等常见值提供专用可变状态包装，减少装箱和重复样板。它们的语义仍与 Compose 状态一致：在组合中读取会建立观察关系。

## 一次执行标记

`LaunchFlag` 用原子状态保护“只启动一次”的动作。Desktop 主窗口使用它避免窗口重组时重复执行 `initPoolLater`。适用场景是同一对象生命周期内只应成功触发一次的边界事件，而不是代替 `remember`：

```kotlin
private val starter = LaunchFlag()

LaunchedEffect(Unit) {
    starter {
        connectOnce()
    }
}
```

如果对象本身被重建，标记也会重置。需要进程级幂等时，应把状态放进应用服务并让操作自身可重复执行。

## 深链中继

`DeepLink` 解决“链接早于导航容器到达”的竞态：平台入口可以先提交 URI；导航建立监听后，缓存的 URI 会被交付。这样 Android `Intent`、Desktop 的系统 URL handler 和 iOS `onOpenURL` 不必知道页面栈是否已完成组合。

它是单个待处理链接的轻量中继，不是持久队列。需要保证多条外部事件逐条处理时，应在业务层建立队列。

## 生命周期与焦点

核心模块提供跨平台的窗口焦点、应用前后台与离屏状态辅助。典型用途是：

- 页面不可见时暂停动画或轮询。
- 窗口失焦时降低刷新频率。
- 回到前台时重新验证易过期数据。

这些状态描述 UI 宿主，而不是业务页面是否位于导航栈顶。页面级进入/恢复应使用 `Screen.initialize`、`resume`、`finalize`；应用服务级清理应使用 `Startup.destroyBefore` / `destroy`。

## 图形与平台桥接

Compose Core 还集中处理颜色、BlendMode、路径或平台图形对象的转换，并在各目标提供 `actual`。上层组件应依赖公共图形类型，只有把内容交给 Android、Skia、UIKit 等原生 API 的最后一步才做转换。

## 可移动内容

项目包含对 movable content 的封装，用于在布局结构变化时迁移同一段组合而不丢失内部状态。响应式布局中，一个播放器从底部区域移到侧栏时，这比在两个分支各创建一次组件更可靠。

使用时仍要保持稳定键，并避免把平台原生 View 在不支持重挂载的宿主间任意移动。原生视图的创建、更新、重置与释放规则见[平台组件](../2_ui/platform_component.md)。

## 如何选择层级

| 需求 | 放置位置 |
| --- | --- |
| 与 Compose 无关的值、协程、文件、URI | `core` / `foundation` |
| 通用 Compose 状态或平台 UI 桥 | `compose:core` |
| 颜色、排版、尺寸、形状约定 | `compose:theme` |
| 可直接展示和交互的控件 | `compose:ui` |
| 页面生命周期和返回栈 | `compose:screen` |

这种分层让命令行工具和原生服务端不会因为一个通用辅助函数而被迫链接 Compose，同时也让 UI 层能共享一致的状态语义。
