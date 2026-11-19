---
description: 使用 Rachel 的 Dialog、Sheet、Tip、FAB 与 Android 系统悬浮窗管理覆盖层交互。
---

# 浮层与窗口

<p class="rc-lead">Rachel 通过 <code>Floating</code>、Dialog、Sheet、Tip 与 FAB 管理覆盖层交互。浮层实例保存打开状态，组合侧调用 <code>Land()</code> 渲染对应内容。</p>

## 注册与显示

`BasicScreen` 自动渲染默认的 `slot.info`、`slot.confirm`、`slot.loading`、`slot.tip` 与页面 `fab`。自定义对话框与面板通过 `land` 注册：

```kotlin title="页面浮层"
class ScreenEditor : Screen() {
    private val renameDialog = this land DialogInput(hint = "名称")
    private val detailsSheet = this land DetailsSheet()

    private fun rename() = launch {
        val value = renameDialog.open(initText = currentName) ?: return@launch
        saveName(value)
    }

    @Composable
    override fun Content() {
        PrimaryButton("重命名", onClick = { rename() })
    }
}
```

`land` 是页面的中缀函数，需要提供接收者。非页面场景应保持浮层实例稳定，并在所属组合中调用相应 `Land()`。仅执行 `open()` 不会自动插入组合节点。

## 对话框结果

```kotlin title="确认与加载"
launch {
    if (!slot.confirm.open(content = "确认删除该条目？")) return@launch

    var deleted = false
    slot.loading.open(content = "正在删除") {
        repository.delete(id)
        deleted = true
    }

    if (deleted) slot.tip.success("删除完成")
}
```

| 类型 | 常用结果 |
| --- | --- |
| `DialogInfo` | 等待确认或关闭，无返回值 |
| `DialogConfirm` | `Boolean` |
| `DialogInput` | `String?` |
| `DialogPairInput` | `Pair<String, String>?` |
| `DialogChoice` | `Int?` |
| `DialogProgress<R>` | `R?` |
| `DialogLoading` | 等待任务完成，无返回值 |

Dialog 通过 `SyncFuture` 等待结果。带任务的版本将结果等待与任务置于同一 `coroutineScope`，用户结束交互时可以取消并行任务。自定义对话框可通过受保护的 `future?.send(value)` 提交结果。

!!! warning "普通异常可能被折叠"
    `DialogLoading.open()` 不返回业务成功标志。操作需要区分成功与失败时，应显式保存结果或使用可表达结果的流程，再决定是否提示成功或导航。

同一浮层实例只保存一份当前参数和等待对象，不应同时启动多个竞争的打开流程。取消异常仍会传播。

## Sheet

```kotlin title="带参数面板"
class DetailsSheet : SheetContent<String>() {
    @Composable
    override fun Content(args: String) {
        Text(args)
    }
}

detailsSheet.open("详情内容")
```

无参数面板继承 `Sheet`，带参数面板继承 `SheetContent<A>`。布局方向由窗口形态决定：纵向形态从底部进入，方形与横向形态从右侧进入。

| 配置 | 默认行为 |
| --- | --- |
| 纵向高度 | 父容器高度的一定比例范围 |
| 横向宽度 | 主题面板尺寸 |
| 滚动 | 内置滚动容器 |
| 圆角与拖拽柄 | 可覆写 |

内容本身管理 Lazy 列表滚动时，应核对与内置滚动容器的协作。

## Tip 与 FAB

```kotlin title="Tip"
slot.tip.info("提示信息")
slot.tip.success("保存完成")
slot.tip.warning("输入不完整")
slot.tip.error(error.message)
```

Tip 默认短暂显示，不显示遮罩，也不接管返回键。新提示会取消前一个计时任务，避免旧任务关闭后续提示。持续状态应在页面内展示，不宜频繁转为临时提示。

页面覆写 `fab` 可提供主动作与 `FABAction` 列表，框架自动调用 `Land()`。`FABScrollTop` 提供回到滚动起点的行为。

## 层级与生命周期

| 浮层 | 默认 zIndex |
| --- | --- |
| 通用浮层 | 低 |
| FAB | 中 |
| Sheet | 较高 |
| Dialog | 高 |
| Tip | 最高 |

`Floating` 统一打开参数、进出动画、遮罩、外部点击、返回键及离开组合时的关闭。自定义实现需明确关闭策略，并验证安全区、键盘、焦点与多浮层叠加行为。

## Android 系统悬浮窗

页面 Dialog、Sheet 等由当前组合树绘制。Android 系统悬浮窗使用 `compose:app` 中的 `ApplicationWindow.OverlayWindow`，底层为 `love.yinlin.compose.window.FloatingView`。

应用调用 `openWindow(window, owner: LifecycleOwner)` 注册系统悬浮窗。Owner 销毁时该宿主关闭其窗口；使用服务 Owner 可以让歌词窗的存续范围与服务一致。每次打开仍需创建新的 `ApplicationWindow`，不能重新打开已关闭的实例。

```kotlin title="不可触摸歌词窗"
@Composable
override fun Window(content: @Composable () -> Unit) {
    OverlayWindow(
        touchable = false,
        onPermissionFailed = {
            if (currentWindow === this) app.config.enabledFloatingLyrics = false
        },
        onOpen = {
            view = it
            if (currentWindow === this) isAttached = true
        },
        onClose = { view = null },
        content = content,
    )
}
```

Manifest 需要声明 `android.permission.SYSTEM_ALERT_WINDOW`。`OverlayWindow` 检查权限，缺少权限时等待前台 Activity 可用并发起设置页请求；Activity 重建后会重建未完成的请求，明确拒绝时关闭窗口并调用 `onPermissionFailed`。

!!! warning "窗口已激活不等于已挂载"
    实际挂载以 `onOpen` 为准。权限、Activity 重建与系统限制都可能让窗口未按预期显示。

`layoutInScreen` 与 `touchable` 是创建参数。`FloatingView` 使用基于 Application 的窗口 Context，并为 ComposeView 安装独立的 Lifecycle、SavedStateRegistry 与 ViewModelStore；释放时解除引用，不要求把 Activity 保存到长生命周期服务中。

## 浮层设计检查

- [ ] 关闭、取消与外部点击语义明确；
- [ ] 结果类型与业务失败可以区分；
- [ ] 同一实例不会并发打开多个流程；
- [ ] 安全区、键盘与焦点处理正确；
- [ ] 多浮层叠加时层级与返回键行为一致；
- [ ] 系统悬浮窗权限被拒绝时有降级路径。

## 下一步

继续阅读[自适应与平台视图](adaptive.md)，了解布局分类与原生视图宿主。
