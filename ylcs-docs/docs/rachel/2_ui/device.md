# 设备模型与自适应布局

Rachel 的 `Device` 描述的是**当前内容容器**，不是手机、平板或电脑的硬件标签。窗口缩放、桌面分屏、浏览器尺寸变化以及移动设备旋转都会重新分类，因此同一平台可以在运行期间经过多种设备状态。

```kotlin
@Stable
@Serializable
data class Device(
    val size: Device.Size,
    val type: Device.Type
)
```

`size` 回答“可用宽度有多大”，`type` 回答“当前宽高比例适合怎样排版”。二者正交：例如一个宽度属于 `MEDIUM` 的窗口，既可能是 `PORTRAIT`，也可能是 `SQUARE`。

## 尺寸分级

尺寸只看容器宽度，边界值包含在较小一级中：

| 容器宽度 | `Device.Size` | 常见用途 |
| --- | --- | --- |
| `<= 420.dp` | `SMALL` | 单列、紧凑操作区 |
| `420.dp < width <= 900.dp` | `MEDIUM` | 宽单列或有限双栏 |
| `> 900.dp` | `LARGE` | 多栏、常驻侧边区域 |

这些是项目自己的布局断点，不应与某个平台的设备类别或 Material Window Size Class 混用。

## 形态分级

`Device(width, height)` 使用宽高比例判断形态：

| 条件 | `Device.Type` |
| --- | --- |
| `height >= width * 1.5` | `PORTRAIT` |
| `width >= height * 1.3333` | `LANDSCAPE` |
| 其余比例 | `SQUARE` |

`SQUARE` 表示中间比例区间，并不要求宽高相等。这个缓冲区避免接近方形的平板或桌面小窗口在横竖两套极端布局间过早切换。

另有一个只接收 `width` 的构造函数，主要适合只有宽度信息的场景：它把 `SMALL`、`MEDIUM`、`LARGE` 分别映射为 `PORTRAIT`、`SQUARE`、`LANDSCAPE`。Compose 的窗口监听 API 使用完整宽高，不走这套简化推断。

## 在 Compose 中读取

三个入口都读取 `LocalWindowInfo.current.containerDpSize`，并用派生状态跟随容器变化：

```kotlin
val device by rememberDevice()
val size by rememberDeviceSize()
val type by rememberDeviceType()
```

- `rememberDevice()`：布局同时关心宽度级别和形态时使用。
- `rememberDeviceSize()`：只因宽度断点改变时使用。
- `rememberDeviceType()`：只在竖向、横向和中间形态之间分支时使用。

按最小所需信息订阅，能让代码表达更清楚；这些函数返回 Compose `State`，不要在组合外长期缓存它们的当前值。

## 页面分支示例

```kotlin
@Composable
fun AccountPage() {
    val device by rememberDevice()

    when (device.type) {
        Device.Type.PORTRAIT -> AccountSingleColumn()
        Device.Type.SQUARE -> AccountCompactTwoPane()
        Device.Type.LANDSCAPE -> AccountWideTwoPane(
            showAside = device.size == Device.Size.LARGE
        )
    }
}
```

实际页面可以合并分支，不必为三个枚举值制造三份重复 UI：

```kotlin
when (val type = rememberDeviceType().value) {
    Device.Type.PORTRAIT,
    Device.Type.SQUARE -> CompactContent()

    Device.Type.LANDSCAPE -> WideContent()
}
```

应尽量让分支只决定骨架，把业务状态和可复用内容留在外层。这样窗口跨过断点时只是重新布置同一份状态，不会重建页面模型或重复发起网络请求。

## 框架中的实际策略

浮层系统也消费同一个 `Device`。`BasicSheet` 在 `PORTRAIT` 下从底部进入、纵向拖拽；在 `SQUARE` 和 `LANDSCAPE` 下贴右侧并横向进出。这让页面布局与浮层交互共享同一套空间判断，而不是分别猜测当前是不是手机。

项目中的图片预览、登录、设置、社区详情和游戏页面也按 `rememberDeviceType()` 选择单列或宽屏布局。各页面对 `SQUARE` 的归组不完全相同，这是有意的：中间比例只是一个可供决策的信号，最终仍应由内容密度决定它更接近紧凑布局还是宽屏布局。

## 设计与测试建议

- 不要用 `Platform.Android`、`Platform.Desktop` 代替布局判断；桌面窗口可能很窄，手机横屏也可能很宽。
- 不要把像素尺寸直接传给 `Device`；断点单位是 `Dp`，平台密度已由 `containerDpSize` 处理。
- 页面只需要比例时优先读取 `rememberDeviceType()`，需要断点时再读取 `size`。
- 在 `420.dp`、`900.dp`、`1.5` 和 `1.3333` 附近各测试边界，并实际拖动桌面窗口观察状态连续性。
- 自适应切换时保留焦点、滚动位置和页面模型；不要把这些状态放进某个布局分支内部后又期待跨分支存活。

主题令牌和安全区的用法见[主题与响应式布局](theme.md)，Sheet 的具体行为见[浮层系统](floating.md)。
