# 浮层

Rachel 通过 `Floating`、Dialog、Sheet、Tip 与 FAB 管理覆盖层交互。浮层实例保存打开状态，组合侧调用 `Land()` 渲染对应内容。

## 注册与显示

`BasicScreen` 自动渲染默认 `slot.info`、`slot.confirm`、`slot.loading`、`slot.tip` 和页面 `fab`。自定义对话框与面板通过 `land` 注册：

```kotlin
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

`land` 是页面的中缀函数，需提供接收者。非页面场景应保持浮层实例稳定，并在所属组合中调用相应 `Land()`。仅执行 `open()` 不会自动插入组合节点。

## 对话框结果

```kotlin
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

Dialog 通过 `SyncFuture` 等待结果。带任务的版本将结果等待与任务置于同一 `coroutineScope`，用户结束交互时可取消并行任务。自定义对话框可通过受保护的 `future?.send(value)` 提交结果。

普通关闭可得到空结果，协程取消仍传播 `CancellationException`。当前 `awaitResult` 在正常结果路径后调用 `close()`，不保证任意外部取消时均通过 `finally` 立即关闭；浮层离开组合时另有关闭处理。

普通任务异常可能被 `catchingNull` 折叠，尤其 `DialogLoading.open()` 不返回业务成功标志。操作需要区分成功和失败时，应显式保存结果或使用可表达结果的流程，再决定是否提示成功或导航。

同一浮层实例只保存一份当前参数和等待对象，不应同时启动多个竞争的打开流程。

## Sheet

```kotlin
class DetailsSheet : SheetContent<String>() {
    @Composable
    override fun Content(args: String) {
        Text(args)
    }
}

// 已注册的实例可在事件处理中打开。
detailsSheet.open("详情内容")
```

无参数面板继承 `Sheet`，带参数面板继承 `SheetContent<A>`。PORTRAIT 下从底部进入，SQUARE 与 LANDSCAPE 下从右侧进入。

竖向高度默认范围为父容器的 30% 至 70%；横向宽度默认使用主题面板尺寸。可覆写 `scrollable`、高度比例、横向宽度、圆角与竖向拖拽柄。内容本身管理 Lazy 列表滚动时，应核对与内置滚动容器的协作。

## Tip 与 FAB

```kotlin
slot.tip.info("提示信息")
slot.tip.success("保存完成")
slot.tip.warning("输入不完整")
slot.tip.error(error.message)
```

Tip 默认显示 3 秒，不显示遮罩，也不接管返回键。新提示会取消前一个计时任务，避免旧任务关闭后续提示。持续状态应在页面内展示，不宜频繁转为临时提示。

页面覆写 `fab` 可提供主动作和 `FABAction` 列表，框架自动调用 `Land()`。`FABScrollTop` 提供回到滚动起点的行为。

## 层级与生命周期

| 浮层 | 默认 zIndex |
| --- | --- |
| 通用浮层 | 5 |
| FAB | 8 |
| Sheet | 10 |
| Dialog | 20 |
| Tip | 30 |

`Floating` 统一打开参数、进出动画、遮罩、外部点击、返回键及离开组合时的关闭。自定义实现需明确关闭策略，并验证安全区、键盘、焦点与多浮层叠加行为。

## 源码入口

- `ylcs-module/compose/ui/src/commonMain/kotlin/love/yinlin/compose/ui/floating/`
- `ylcs-module/compose/screen/src/commonMain/kotlin/love/yinlin/compose/screen/BasicScreen.kt`
