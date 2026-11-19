---
description: 使用 Rachel 主题系统统一颜色、排版、几何、形状、动画与安全区。
---

# 主题系统

<p class="rc-lead"><code>compose:theme</code> 提供 Rachel 的颜色、排版、形状、尺寸、动画与界面文案。组件通过 <code>Theme</code> 与 CompositionLocal 读取统一配置，而不是在各处写入字面量。</p>

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

标准平台入口调用 `ComposedLayout()` 安装主题。独立预览或嵌入场景可以直接使用 `Theme(...) { ... }`。

```kotlin title="动态主题"
override val themeMode: ThemeMode get() = config.themeMode
override val fontScale: Float get() = config.fontScale
```

动态属性应通过 Compose 可观察状态驱动，而不是在主题安装后修改静态变量。

## 读取主题值

```kotlin title="组件中使用主题"
Surface(
    modifier = Modifier.padding(Theme.padding.eValue),
    shape = Theme.shape.v1,
    shadowElevation = Theme.shadow.v2,
    contentPadding = Theme.padding.value9,
) {
    Text("主题内容", style = Theme.typography.v6.bold)
}
```

| 分组 | 内容 |
| --- | --- |
| `Theme.color`、`darkMode` | 语义颜色与当前明暗模式 |
| `Theme.typography` | 字号、行高、字重与字体 |
| `Theme.shape` | 标准形状与分级圆角 |
| `Theme.size` | 图标、图片、输入、单元格与面板尺寸 |
| `Theme.padding` | 水平、垂直与统一间距 |
| `Theme.border`、`shadow` | 边框与阴影尺度 |
| `Theme.animation` | 动画参数 |
| `Theme.tool`、`value` | 工具开关与通用文案 |

`LocalColor`、`LocalColorVariant` 与 `LocalStyle` 表示当前子树的默认内容颜色与文本样式。容器在自身语义范围内覆盖这些值。

!!! warning "Surface 不提供 color 参数"
    当前 `Surface` 从主题读取表面颜色，通过 `tonalLevel` 控制色调层次。自定义背景应使用合适的容器或绘制 Modifier。

## 颜色系统

`ColorSystem` 同时包含明暗两套语义颜色。品牌主题应通过根 `ColorSystem` 统一声明，而不是在组件内散落颜色。

```kotlin title="颜色系统覆写"
override val colorSystem: ColorSystem
    get() = if (galleryPalette == GalleryPalette.Framework) {
        ColorSystem.Default
    } else {
        GalleryColors
    }
```

固定配色可以使用 `Default.copy(light = ..., dark = ...)` 局部调整。调整时保持语义用途一致，并同时验证明暗模式下的对比度。

## 局部明暗模式

```kotlin title="局部暗色"
Theme.ThemeModeWrapper(isDarkMode = true) {
    PreviewContent()
}
```

该包装替换明暗颜色、内容颜色与文本选择色，不重新建立整套几何与排版配置。品牌主题仍应通过根 `ColorSystem` 声明。

## 排版与几何

| 配置 | 关注点 |
| --- | --- |
| `typography` | 字号、行高、字重、字体 |
| `shape` | 标准形状与分级圆角 |
| `geometry` | 尺寸、间距、边框、阴影 |
| `animation` | 时长、缓动与转场参数 |

使用 `Theme.padding`、`Theme.size`、`Theme.border` 等语义值，而不是 `12.dp`、`16.sp` 等字面量。这样在密度、字体缩放与主题切换时保持一致。

## 响应式布局

`rememberDevice()`、`rememberDeviceSize()` 与 `rememberDeviceType()` 根据当前窗口容器的 `Dp` 尺寸派生布局分类。它们不依据平台名称或硬件型号判断设备类型。

```kotlin title="设备分类"
val device by rememberDevice()

when (device.type) {
    Device.Type.PORTRAIT -> AccountSingleColumn()
    Device.Type.SQUARE -> AccountCompactLayout()
    Device.Type.LANDSCAPE -> AccountWideLayout(
        showAside = device.size == Device.Size.LARGE,
    )
}
```

宽度断点与形态规则见[自适应与平台视图](adaptive.md)。

## 安全区

导航宿主提供 `LocalImmersivePadding`，表示系统栏或窗口安全区。`Screen` 的标题、内容与底栏分别消费所需边距，浮层也按自身位置处理安全区。

```kotlin title="安全区"
val padding = LocalImmersivePadding.current
Box(Modifier.padding(padding)) {
    Content()
}
```

使用前确认父容器是否已经消费相应方向的边距，避免重复应用完整安全区。

## 主题配置约束

- 颜色保持语义用途，不把“品牌色”当作“错误色”；
- 明暗切换、字体缩放与窗口尺寸变化需要共同验证；
- 组件尺寸使用主题值，不在业务代码中硬编码；
- 文案通过 `valueTheme` 统一，避免同一概念出现多种称呼；
- 动画遵循系统“减少动态效果”偏好。

## 实际应用的主题覆写

Gallery 在共享应用的属性 getter 中读取可观察状态，切换外观时整棵主题子树随之更新：

```kotlin title="GalleryApplication.kt"
override val themeMode: ThemeMode
    get() = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT

override val mainFontResource = Res.font.font

override val colorSystem: ColorSystem
    get() = if (galleryPalette == GalleryPalette.Framework) ColorSystem.Default else GalleryColors

override val toolingTheme = ToolingTheme(enableBallonTip = true)
```

`mainFontResource` 使用当前模块生成的 `Res`。`colorSystem` 可以在默认与自定义配色之间切换。

## 下一步

继续阅读[组件体系](components.md)，了解主题如何落实到输入、容器、图像与浮层组件。
