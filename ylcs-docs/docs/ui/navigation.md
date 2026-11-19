---
description: 使用 ScreenModel、BasicScreen 与 ScreenManager 管理页面、参数、导航策略与恢复边界。
---

# 导航与页面

<p class="rc-lead"><code>compose:screen</code> 基于 Navigation 3 与 ViewModel 管理页面。导航创建描述与运行时页面实例分离：<code>ScreenKey</code> 保存类型、初始参数和实例 ID，<code>ScreenManager</code> 管理栈变更，<code>ScreenStore</code> 管理页面作用域。</p>

## 页面层级

| 类型 | 职责 |
| --- | --- |
| `ScreenModel : ViewModel` | 生命周期、页面协程、导航、参数唤醒与 `ModelContent()` |
| `BasicScreen : ScreenModel` | 返回处理、页面浮层槽、FAB 与 `BasicContent()` |
| `Screen : BasicScreen` | 标题栏、动作区、第二标题栏、内容区与底栏 |

页面实例由导航管理，工厂必须为新条目返回新的页面对象。同一页面类型可以创建多个相互隔离的实例，不能把同一个 ViewModel 实例重复挂接到多个条目。

## 注册导航

```kotlin title="根导航"
@Composable
override fun Content() {
    ScreenManager.Navigation<ScreenHome>(deeplink = this) {
        screen(::ScreenHome)
        screen(::ScreenDetails)
        screen(::ScreenProfile, key = "profile")
    }
}
```

| 参数 | 含义 |
| --- | --- |
| 泛型 | 根页面类型 |
| `deeplink` | 应用实现 `DeepLink` 时传入 |
| `screen(...)` | 注册页面工厂 |
| `key` | 为页面绑定一个字符串键，可用于受控跳转 |

根页面必须以零参数创建并显式注册。注册器按页面的具体类型保存工厂；同类型重复注册、重复绑定 key 或空白 key 会失败。

未知页面类型与未绑定字符串 key 使用 `Screen404`，可通过 `screen404 { ... }` 定制。已注册工厂的参数数量、解码或构造错误会传播给调用方，不应依赖其自动转为 404。

## 页面生命周期

```kotlin title="页面"
class ScreenDetails(private val initialId: String) : Screen() {
    override val title = "详情"
    private var currentId by mutableStateOf(initialId)

    override fun initialize() {
        launch { load(currentId) }
    }

    override fun resume() = withResume<String> { newId ->
        currentId = newId
        launch { load(newId) }
    }

    override fun uninitialize() {
        // 释放页面持有的非协程资源。
    }

    @Composable
    override fun Content() {
        Text(currentId)
    }
}
```

`initialize()`、`resume()` 与 `uninitialize()` 均为普通函数。挂起工作通过页面 `launch { ... }` 启动，协程绑定 `viewModelScope`。

| 回调 | 触发条件 |
| --- | --- |
| `initialize()` | 新页面挂接并完成栈变更后首次初始化，仅执行一次 |
| `resume()` | 显式 `Resume` 请求匹配已存在的同类型条目，包括目标已在栈顶 |
| `uninitialize()` | 页面 ViewModel 实际清理时执行一次 |
| `onBack()` | `BasicScreen` 接收返回事件，默认调用 `pop()` |

普通 `pop()` 返回旧页面不调用 `resume()`。页面被覆盖不代表已经清理；离栈条目可能因转场仍被持有，最后一个引用释放后才结束 ViewModel 生命周期。

!!! note "monitor 只观察快照状态"
    `monitor(state, action)` 内部使用 `snapshotFlow(state).collectLatest(action)`。普通变量若未参与 Compose 快照，不会被转换为可观察状态。

## 参数与条目标识

内部 `ScreenKey` 包含：

| 字段 | 含义 |
| --- | --- |
| `type` | 注册类型与策略匹配依据 |
| `args` | 首次创建参数，使用 `ScreenArgs` 包装 JSON 数组 |
| `id` | 该条目的独立 `ScreenID` |
| `isUnboundKey` | 记录无法解析的字符串 key |

公开注册、导航与 `withResume` 重载支持零至多个参数。调用方应使用公开 API，不直接依赖内部 JSON 编码。

参数需要可序列化。优先传递稳定 ID 与小型值，避免传入 ViewModel、平台句柄、回调、文件流或大对象。

字符串 key 的跳转标记为 `@LooseTyped`，调用方需显式 opt-in，并在运行时保证参数数量与类型正确。具有静态工厂引用时优先使用类型安全重载。

## 导航策略

```kotlin title="策略"
navigate(::ScreenDetails, "demo")

navigate(
    ::ScreenDetails,
    "updated",
    policy = CreatePolicy.Resume + ClearPolicy.None,
)

navigate(
    ::ScreenHome,
    policy = CreatePolicy.Move + ClearPolicy.Clear,
)
```

默认策略为 `CreatePolicy.New + ClearPolicy.None`。除 `New` 外，策略查找原栈中最靠近栈顶的同类型条目，匹配时忽略参数。

| 创建策略 | 匹配已有条目 | 未匹配 |
| --- | --- | --- |
| `New` | 创建独立新条目 | 创建 |
| `Replace` | 移除匹配条目并创建新条目 | 创建 |
| `Move` | 保留原实例并移至栈顶，不更新参数或调用 `resume` | 创建 |
| `Resume` | 保留原实例并移至栈顶，同步调用 `resume` | 创建并仅执行初始化 |

`ClearPolicy.None` 保留目标上方的其他页面；`ClearPolicy.Clear` 清理目标与原栈顶之间的页面。`New` 或未匹配目标时不额外执行沿途清理。`pop()` 保留栈中最后一个条目。

!!! warning "导航防抖"
    每个 `ScreenManager` 的所有 `navigate` 重载与 `pop()` 共享一个短防抖窗口。窗口内的请求会被忽略，不改变栈、不创建页面、不触发回调，也不会延迟执行。栈变更应在界面线程串行执行。

## Resume 参数

`Resume` 的新参数仅用于当前回调，不覆盖 `ScreenKey.args` 中的初始创建参数：

```kotlin title="withResume"
override fun resume() = withResume<String, Int> { id, version ->
    launch { reload(id, version) }
}
```

`withResume` 仅允许在 `resume()` 的同步调用范围内使用，不能放入稍后执行的 `launch` 中。它不会在第一次读取后消费参数；同一回调内可以多次读取。

## 宿主、嵌套与恢复边界

每个 `Navigation` 从当前 `LocalViewModelStoreOwner` 获取管理器，以 `rememberSaveable` 保存管理器标识。在同一个 Owner 保留期间，管理器、栈与页面实例可以随界面重建继续使用。

根导航通常归属 Activity 或窗口；页面内容中的子导航归属父页面 Owner。不同导航标识具有独立的管理器与页面存储。

!!! danger "当前不等同于完整进程恢复"
    仅保存管理器标识，没有把完整 `backStack` 或管理器写入 SavedState。进程死亡或 Owner 被真正销毁后，新建管理器从根页面开始。`ScreenKey` 可序列化不代表公共宿主已经实现完整进程恢复。

可持久化内容应限制为标识、可序列化参数和小型界面状态；平台句柄、回调、文件流与 ViewModel 由运行时所有者管理。

## 深链

```kotlin title="DeepLink"
override fun onDeepLink(manager: ScreenManager, uri: Uri) {
    if (uri.scheme != Scheme.Rachel || uri.path != "/openProfile") return

    val uid = uri.params["uid"]?.toIntOrNull() ?: return
    manager.navigate(::ScreenProfile, uid)
}
```

平台收到地址后调用 `DeepLink.navigate(uri)`。中继最多缓存一条尚未交付的链接，全局仅维护一个监听器，适合绑定根导航。多个独立宿主不应同时把它当作可靠事件通道。

## 页面浮层与共享状态

`BasicScreen.slot` 提供信息、确认、加载与提示浮层，自定义浮层通过 `this land ...` 注册，见[浮层与窗口](floating.md)。

跨页面状态通过应用服务或显式注入的业务状态所有者共享。不要依赖全局页面查找修改其他页面。

## 导航示例工程

```kotlin title="App.kt"
@Composable
fun App() {
    ScreenManager.Navigation<OverviewScreen> {
        registerGallery(GalleryRoutes())
    }
}
```

`registerGallery` 是示例工程自己的注册扩展，不是框架内置 API。参数、复用与清理规则由 `ScreenArgsTest`、`ScreenResumeTest`、`ScreenNavigationTest` 与 `ScreenStoreTest` 覆盖。

```powershell title="运行导航示例"
.\gradlew.bat :ylcs-app:modern-screen:modernScreenRun
```

## 下一步

继续阅读[浮层与窗口](floating.md)，了解页面内的覆盖层与 Android 系统悬浮窗。
