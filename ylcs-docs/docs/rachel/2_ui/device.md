# 设备模型与自适应布局

`Device` 根据当前窗口内容容器的尺寸描述布局空间。其 `size` 表示宽度等级，`type` 表示宽高比例分类，两者相互独立。

```kotlin
@Stable
@Serializable
data class Device(val size: Size, val type: Type)
```

上例概括公共数据结构，完整枚举与构造器以源码为准。

## 宽度分级

| 容器宽度 | `Device.Size` |
| --- | --- |
| `width <= 420.dp` | `SMALL` |
| `420.dp < width <= 900.dp` | `MEDIUM` |
| `width > 900.dp` | `LARGE` |

边界值属于较小等级。分类单位为 `Dp`，不得直接传入未转换的物理像素尺寸。

## 形态分级

`Device(width, height)` 按以下顺序判断：

| 条件 | `Device.Type` |
| --- | --- |
| `height >= width * 1.5f` | `PORTRAIT` |
| `width >= height * 1.3333f` | `LANDSCAPE` |
| 其余 | `SQUARE` |

`SQUARE` 表示中间比例区间，不要求宽高相等。只接收宽度的构造器则将 SMALL、MEDIUM 和 LARGE 分别映射为 PORTRAIT、SQUARE 和 LANDSCAPE。

## 在 Compose 中使用

```kotlin
val device by rememberDevice()
val size by rememberDeviceSize()
val type by rememberDeviceType()
```

这些函数位于 `love.yinlin.compose`，读取 `LocalWindowInfo.current.containerDpSize` 并返回派生 `State`。`rememberDevice` 使用完整宽高，不采用仅宽度构造器的形态推断。

上述分类对应窗口容器，不直接反映任意嵌套组件的局部约束。局部面板需要独立响应可用空间时，应使用其布局约束计算。

## 布局示例

```kotlin
@Composable
fun AccountContent() {
    val device by rememberDevice()

    when (device.type) {
        Device.Type.PORTRAIT -> AccountSingleColumn()
        Device.Type.SQUARE -> AccountCompactLayout()
        Device.Type.LANDSCAPE -> AccountWideLayout(
            showAside = device.size == Device.Size.LARGE
        )
    }
}
```

页面可按内容密度合并中间形态与其他分支。布局分支负责空间组织，业务数据、选择状态和页面模型应由稳定的外部所有者管理。

## 框架中的使用

`BasicSheet` 在 PORTRAIT 下从底部进入，在 SQUARE 与 LANDSCAPE 下从右侧进入。应用页面也使用同一模型选择单列或多栏布局，因此平台类型不能代替空间判断。

窗口跨越断点时，应保留滚动、焦点和业务状态。需要迁移同一段组合内容时可使用 `rememberMovableContent`，相关说明见[Compose 核心](../1_start/compose_core.md)。

## 验证范围

布局验证应覆盖宽度断点两侧、比例边界、窗口连续缩放、字体缩放与输入法出现后的可用空间。具体页面还需检查滚动、空数据、禁用状态和安全区。

## 源码入口

`ylcs-module/compose/theme/src/commonMain/kotlin/love/yinlin/compose/Device.kt`
