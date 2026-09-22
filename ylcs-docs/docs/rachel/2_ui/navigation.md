# 导航与页面

Rachel 的导航建立在 Navigation 3、可保存字符串栈和 ViewModel 生命周期之上。每个页面实例由一个路由字符串唯一标识；页面对象在对应 NavEntry 存活期间保留，离栈后统一清理。

## 注册根导航

```kotlin
@Composable
override fun Content() {
    ScreenManager.Navigation<ScreenHome>(deeplink = this) {
        screen(::ScreenHome)
        screen(::ScreenProfile)
        screen(::ScreenDetails)

        // 给外部或历史路由一个稳定别名
        screen(::ScreenUserCard, "ScreenUserCard")
    }
}
```

泛型参数是根页面，它在空状态恢复时自动成为栈底。没有注册或无法构造的路由会进入 `Screen404`。注册表在组合中 `remember`，builder 应保持纯粹，不要在其中启动任务。

## 编写页面

```kotlin
class ScreenDetails(
    private val id: String
) : Screen() {
    override val title = "详情"

    override suspend fun initialize() {
        load(id)
    }

    override suspend fun resume() = withResume<String?> { changedId ->
        if (changedId != null) load(changedId)
    }

    override fun finalize() {
        // 释放非协程资源；viewModelScope 会随页面取消
    }

    @Composable
    override fun Content() {
        DetailsContent()
    }
}
```

`Screen` 自动提供标题栏、返回按钮、左右动作、第二标题栏、内容区和底栏。只需要原始画布时可以直接继承 `BasicScreen`。

## 页面生命周期

| 回调 | 语义 |
| --- | --- |
| `initialize()` | 新实例第一次进入时执行一次 |
| `resume()` | `Resume` 策略找到旧实例并把它移到栈顶时执行 |
| `finalize()` | ViewModel 真正离开导航并被清理时执行 |
| `onBack()` | 系统/框架返回事件，默认 `pop()` |

页面暂时被另一个页面覆盖不会触发 `finalize`。需要“当前可见”语义时，应结合导航策略或组合生命周期，不要把 `initialize`/`finalize` 当成每次进入/离开回调。

页面通过 `launch {}` 在自己的 `viewModelScope` 中运行任务；`monitor({ value }) { ... }` 使用 `snapshotFlow.collectLatest` 观察普通读取表达式。

## 路由编码

内部路由由三部分构成：

```text
screenKey|uniqueId?[JSON 参数数组]
```

- `screenKey` 默认是完整类名，也可以是注册别名。
- `uniqueId` 让同一种页面可以有多个独立实例。
- 参数以 JSON 数组保存，当前构造器支持 0 到 3 个参数。

路由栈本身使用 `rememberSaveable` 保存，因此构造参数必须可序列化和可稳定恢复。不要传 Activity、文件流、回调或巨大对象；传 ID，再从 DataSource/服务重新取得实体。

## 导航策略

```kotlin
navigate(::ScreenDetails, id, policy = NavigationPolicy.New)
navigate(::ScreenHome, policy = NavigationPolicy.Move + ClearPolicy.Clear)
```

| 策略 | 已有同类页面时 | 不存在时 |
| --- | --- | --- |
| `New` | 仍创建全新实例 | 创建 |
| `Replace` | 删除最近的同类实例，再创建新实例 | 创建 |
| `Move` | 把最近的同类实例移到栈顶，不刷新 | 创建 |
| `Resume` | 移到栈顶，注入新参数并调用 `resume()` | 创建 |

加上 `ClearPolicy.Clear` 后，会清除目标与栈顶之间的页面。`pop()` 永远保留根页面。连续导航有 300 ms 防抖，按钮逻辑不要依赖在这一窗口内成功压入多级页面。

`Replace` 总会产生新的页面作用域；`Resume` 才是向旧实例传递更新的方式。在 `resume()` 中用类型匹配的 `withResume(...)` 消费参数，参数只交付一次。

## 深链

应用实现 `DeepLink<ScreenManager>`，在 `onDeepLink` 中把 URI 映射为类型安全导航：

```kotlin
override fun onDeepLink(manager: ScreenManager, uri: Uri) {
    if (uri.scheme == Scheme.Rachel && uri.path == "/openProfile") {
        uri.params["uid"]?.toIntOrNull()?.let { uid ->
            manager.navigate(::ScreenProfile, uid)
        }
    }
}
```

平台只负责把外部事件转成公共 `Uri`。`DeepLink` 会暂存导航尚未注册时到达的一条链接，避免冷启动竞态；多事件可靠队列应由业务层实现。

## 共享数据而不是查找页面

`ScreenManager` 明确不鼓励从栈中寻找任意页面并改它的字段。跨页面状态放入 `DataSource`；页面被清理时框架会调用 `onDataSourceClean()`。需要多个数据域时使用 `MultiDataSource`。

这种规则避免导航栈成为隐式服务定位器，也让 `Replace`、状态恢复和多实例页面保持可预测。

## 页面浮层槽

每个 `BasicScreen` 都有 `slot`：

```kotlin
slot.tip.success("保存成功")

launch {
    if (slot.confirm.open(content = "确定删除？")) {
        delete()
    }
}
```

默认 `info`、`confirm`、`loading` 和 `tip` 已由页面自动 Land。自定义 Dialog/Sheet 需用页面的 `land` 注册，详见[浮层](floating.md)。
