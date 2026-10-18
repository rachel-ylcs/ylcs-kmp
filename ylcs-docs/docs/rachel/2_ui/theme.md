# 主题系统

`compose:theme` 提供 Rachel 的颜色、排版、形状、尺寸、动画和界面文案。组件通过 `Theme` 与 CompositionLocal 读取统一配置。

## 根主题

`Application` 提供以下可覆写属性：

| 属性 | 用途 |
| --- | --- |
| `themeMode` | `SYSTEM`、`LIGHT` 或 `DARK` |
| `fontScale` | 根字体缩放 |
| `mainFontResource` | 公共字体资源 |
| `background` | 根背景覆盖值 |
| `colorSystem` | 明暗两套颜色主题 |
| `typographyTheme`、`shapeTheme` | 排版与形状 |
| `geometryTheme` | 尺寸、间距、边框与阴影 |
| `animationTheme` | 动画配置 |
| `toolingTheme` | 气泡提示等交互工具配置 |
| `valueTheme` | 通用界面文案 |

标准平台入口调用 `ComposedLayout()` 安装主题。独立预览或嵌入场景可直接使用 `Theme(...) { ... }`。动态主题属性应通过 Compose 可观察状态驱动。

```kotlin
override val themeMode: ThemeMode get() = config.themeMode
override val fontScale: Float get() = config.fontScale
```

## 读取主题值

```kotlin
Surface(
    modifier = Modifier.padding(Theme.padding.eValue),
    shape = Theme.shape.v1,
    shadowElevation = Theme.shadow.v2,
    contentPadding = Theme.padding.value9
) {
    Text("主题内容", style = Theme.typography.v6.bold)
}
```

当前 `Surface` 从主题读取表面颜色，通过 `tonalLevel` 控制色调层次；它不提供 `color` 参数。自定义背景可使用合适的容器或绘制 Modifier。

| 分组 | 作用 |
| --- | --- |
| `Theme.color`、`darkMode` | 语义颜色与当前明暗模式 |
| `Theme.typography` | 字号、行高、字重与字体 |
| `Theme.shape` | 标准形状与分级圆角 |
| `Theme.size` | 图标、图片、输入、单元格及面板尺寸 |
| `Theme.padding` | 水平、垂直和统一间距 |
| `Theme.border`、`shadow` | 边框与阴影尺度 |
| `Theme.animation` | 动画参数 |
| `Theme.tool`、`value` | 工具开关与通用文案 |

`LocalColor`、`LocalColorVariant` 与 `LocalStyle` 表示当前子树的默认内容颜色和文本样式。容器在自身语义范围内覆盖这些值。

## 局部明暗模式

```kotlin
Theme.ThemeModeWrapper(isDarkMode = true) {
    PreviewContent()
}
```

该包装替换明暗颜色、内容颜色和文本选择色，不重新建立整套几何与排版配置。品牌主题应通过根 `ColorSystem` 等参数统一声明。

## 响应式布局

`rememberDevice()`、`rememberDeviceSize()` 与 `rememberDeviceType()` 根据当前窗口容器的 `Dp` 尺寸派生布局分类。它们不依据平台名称或硬件型号判断设备类型。

宽度断点为 `420.dp` 与 `900.dp`；形态根据宽高比例划分为 `PORTRAIT`、`SQUARE` 与 `LANDSCAPE`。完整规则与示例见[设备模型](device.md)。

## 安全区

导航宿主提供 `LocalImmersivePadding`，表示系统栏或窗口安全区。`Screen` 的标题、内容与底栏分别消费所需边距，浮层也按自身位置处理安全区。

```kotlin
val padding = LocalImmersivePadding.current
Box(Modifier.padding(padding)) {
    Content()
}
```

使用前应确认父容器是否已经消费相应方向的边距，避免重复应用完整安全区。

## 配置约束

主题定制应保持颜色语义、内容对比度和尺寸用途一致。明暗切换、字体缩放和窗口尺寸变化需要共同验证。默认值可通过相应主题的 `Default.copy(...)` 等接口局部调整，具体复制方式以类型定义为准。

## 源码入口

- `ylcs-module/compose/theme/src/commonMain/kotlin/love/yinlin/compose/Theme.kt`
- `ylcs-module/compose/theme/src/commonMain/kotlin/love/yinlin/compose/GeometryTheme.kt`
- `ylcs-module/compose/theme/src/commonMain/kotlin/love/yinlin/compose/ImmersivePadding.kt`
