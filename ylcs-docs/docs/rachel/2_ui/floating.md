# 浮层

Rachel 把 Dialog、Sheet、Tip 和 FAB 放在统一的 `Floating` 模型中。浮层对象持有“是否打开”和本次参数，Composable 侧通过 `Land()` 把它安放在页面树中；打开动作只改变对象状态，不会临时创建新的窗口树。

## Land 后再打开

`BasicScreen` 已自动 Land 默认 `slot.info`、`slot.confirm`、`slot.loading`、`slot.tip` 和页面的 `fab`。自定义 Dialog/Sheet 必须注册：

```kotlin
class ScreenEditor : Screen() {
    private val renameDialog = land DialogInput()
    private val detailsSheet = land DetailsSheet()

    private fun rename() = launch {
        val value = renameDialog.open(initText = currentName) ?: return@launch
        saveName(value)
    }

    @Composable
    override fun Content() { /* ... */ }
}
```

没有 Land 的实例可以被标记为 open，却没有任何 UI 消费它。不要在 Composable 每次重组时 `DialogInput()`；让页面/ViewModel持有稳定实例。

## Dialog 的挂起结果

```kotlin
launch {
    if (!slot.confirm.open(content = "删除后无法恢复，继续？")) return@launch

    slot.loading.open(content = "正在删除") {
        repository.delete(id)
    }

    slot.tip.success("已删除")
    pop()
}
```

常用返回形式：

- `DialogConfirm.open(...) : Boolean`
- `DialogInput.open(...) : String?`
- `DialogPairInput.open(...) : Pair<String, String>?`
- `DialogChoice.open() : Int?`
- `DialogProgress<R>.open { ... } : R?`

内部用 `SyncFuture` 与结构化并发同时等待用户结果和自定义任务。用户关闭、页面离开或调用协程取消时，另一侧任务会结束；返回 `null` 表示没有结果。不要把这类取消显示为业务错误。

自定义 Dialog 继承 `Dialog<R>` 或 `DialogTemplate<R>`，按钮通过 `future?.send(value)` 返回结果；关闭会发送空结果并清理 future。

## Sheet

无参数面板继承 `Sheet`，带打开参数的面板继承 `SheetContent<A>`：

```kotlin
class DetailsSheet : SheetContent<Item>() {
    @Composable
    override fun Content(args: Item) {
        Details(args)
    }
}

detailsSheet.open(item)
```

布局会随窗口形态变化：竖屏从底部弹出，横屏/方形从右侧弹出。竖屏高度默认限制在父容器的 30% 到 70%；横屏使用主题 sheet 宽度。拖动超过一半关闭，内容滚动通过 nested scroll 与拖动协作。

可覆写 `scrollable`、尺寸、圆角和竖屏拖拽柄。若内容本身有复杂 Lazy 列表，通常关闭内置纵向滚动，让内容自己承担滚动，避免双重滚动容器。

## Tip

```kotlin
slot.tip.info("提示")
slot.tip.success("保存成功")
slot.tip.warning("输入不完整")
slot.tip.error(error.message)
```

Tip 没有遮罩、不拦截返回键，默认 3 秒关闭。新 Tip 会取消旧定时任务，并检查任务身份，防止旧任务稍后把新消息关掉。频繁状态流不适合逐条 Tip；应在页面内展示稳定状态或合并消息。

## FAB

`FAB` 提供主动作和可展开的 `FABAction` 列表，动作可使用挂起回调，并通过 provider 动态取得图标、文案、颜色和 enabled。页面覆写 `fab` 后会自动 Land；滚动回顶可使用 `FABScrollTop`。

FAB 是当前页面动作，不应承担全局导航栏角色。在小窗口中还要检查它与底部安全区、Sheet 和输入法的重叠。

## 通用浮层行为

`Floating<A>` 提供：

- 响应式对齐和进出动画；
- 可选遮罩、点击外部关闭和返回键关闭；
- 每次打开一次的 `initialize(args)`；
- 离开组合时自动 `close()`；
- 统一层级：普通 5、FAB 8、Sheet 10、Dialog 20、Tip 30。

只有确实需要新的交互形态时才直接继承 `Floating`。覆写 `useBack`、`showScrim` 或 dismiss 行为时，要同时考虑系统返回、可访问性和页面销毁。层级常量保证默认组件互不遮错；自定义 zIndex 应基于这些语义层，而不是随意给一个巨大数字。
