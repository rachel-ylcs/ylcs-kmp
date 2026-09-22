# 主题与响应式布局

Rachel 的 UI 不依赖 Material 组件体系。`Theme` 从颜色系统、排版、形状、几何尺寸、动画和文案工具等多个小主题组装出一套 CompositionLocal，组件直接读取这些令牌。

## 应用级配置

继承 `Application` 时可以覆写：

```kotlin
override val themeMode: ThemeMode
    get() = config.themeMode

override val fontScale: Float
    get() = config.fontScale.value

override val colorSystem: ColorSystem = MyColorSystem
override val typographyTheme: TypographyTheme = MyTypography
override val shapeTheme: ShapeTheme = MyShapes
override val geometryTheme: GeometryTheme = MyGeometry
override val animationTheme: AnimationTheme = MyAnimations
override val toolingTheme: ToolingTheme = MyTooling
override val valueTheme: ValueTheme = MyWords
```

`ThemeMode` 有 `SYSTEM`、`LIGHT`、`DARK`。`ColorSystem` 同时包含明暗两套 `ColorTheme`，切换模式不会改变组件使用的令牌名。

可选的 `mainFontResource` 会注入排版主题；`fontScale` 会调整 Compose density 的字体缩放；`background` 可覆盖根背景。所有覆写值都应稳定，或由 Compose 可观察状态驱动。

## 读取令牌

```kotlin
Surface(
    color = Theme.color.surface,
    shape = Theme.shape.v1,
    shadowElevation = Theme.shadow.v2,
    modifier = Modifier.padding(Theme.padding.eValue)
) {
    Text(
        text = "当前主题",
        style = Theme.typography.v6.bold
    )
}
```

`Theme` 当前暴露：

| 分组 | 用途 |
| --- | --- |
| `color` / `darkMode` | 表面、内容、强调、轮廓、遮罩和当前明暗模式 |
| `typography` | 字号、行高、字重与默认字体 |
| `shape` | 矩形、圆形和分级圆角 |
| `size` / `padding` | 控件、图标、单元格与间距尺度 |
| `border` / `shadow` | 边框宽度与阴影高度 |
| `animation` | 统一动画时长 |
| `tool` | 气泡提示等交互工具开关 |
| `value` | 返回、确认等可替换的运行时文案 |

组件外不要复制令牌的具体数值。语义令牌使同一控件能随设备、品牌主题和可访问性配置一起变化。

## 局部颜色和明暗模式

`LocalColor`、`LocalColorVariant` 与 `LocalStyle` 提供当前内容默认值。容器组件会在需要时建立局部内容颜色。只想让一个子树以另一明暗模式预览时，可使用：

```kotlin
Theme.ThemeModeWrapper(isDarkMode = true) {
    PreviewCard()
}
```

它替换颜色相关 Local，但不会创建一套全新的尺寸、排版和动画主题。完整品牌切换应在应用根的 `Theme` 参数上完成。

## 响应式设备模型

`rememberDevice()` 根据当前窗口容器大小返回 `Device(size, type)`，不是根据操作系统名称猜测：

| 宽度 | `Device.Size` |
| --- | --- |
| `<= 420.dp` | `SMALL` |
| `<= 900.dp` | `MEDIUM` |
| `> 900.dp` | `LARGE` |

方向判断使用比例：高度至少为宽度的 1.5 倍是 `PORTRAIT`；宽度至少为高度的约 1.3333 倍是 `LANDSCAPE`；中间区域是 `SQUARE`。

```kotlin
val device by rememberDevice()

when (device.type) {
    Device.Type.PORTRAIT -> PortraitContent()
    Device.Type.LANDSCAPE -> WideContent()
    Device.Type.SQUARE -> CompactTwoPaneContent()
}
```

这套模型能正确处理可调整大小的桌面窗口、平板分屏和浏览器，不要把 `Platform.Phone` 等同于竖屏。

## 沉浸式安全区域

根导航提供 `LocalImmersivePadding`，统一表示状态栏、导航栏或窗口安全区。页面标题栏、底栏、Sheet 和 Dialog 会按自身位置消费一部分 padding。

```kotlin
val safe = LocalImmersivePadding.current
Box(Modifier.padding(safe)) { /* ... */ }
```

已经位于 `Screen.Content` 或框架浮层内部时，Local 往往已被裁剪，不要再次无条件应用完整安全区。检查父容器的契约，避免顶部或底部出现双倍留白。

## 自定义主题的原则

- 从 `Default` 复制并替换少量语义值，缺少字段时仍有一致默认。
- 颜色必须同时检查 light/dark 下的内容对比度。
- 动画速度为 0 或极小时，也要确保浮层和导航状态能完成。
- 尺寸令牌表达用途，而不是某个页面的偶然像素值。
- 用户可配置值通过状态 getter 返回，让根主题重组，不要在每个组件分别读取配置。
