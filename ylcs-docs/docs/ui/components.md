---
description: 使用 Rachel 基础组件、图片加载、调色板与可选复合组件构建一致的跨平台界面。
---

# 组件体系

<p class="rc-lead"><code>compose:ui</code> 基于 Compose Foundation 提供跨平台组件，并通过 Rachel 主题统一颜色、排版、尺寸与交互。可选复合组件位于独立模块，按需引入。</p>

## 基础组件

| 包 | 代表能力 |
| --- | --- |
| `animation` | `CircleLoading`、`WaveLoading`、`ExpandableContent` |
| `collection` | `TagView`、`TreeView` |
| `container` | `Surface`、`StatefulBox`、`Banner`、`ActionScope` |
| `floating` | Dialog、Sheet、Tip、FAB、Flyout、Menus、BalloonTip |
| `image` | `Icon`、`Image`、`NineGrid`、`LoadingIcon` |
| `input` | Button、Switch、CheckBox、Radio、Slider、ComboBox、Filter、ColorPicker |
| `layout` | Divider、Space 与测量辅助 |
| `navigation` | Breadcrumb、TabBar |
| `node` | 条件、语义、绘制、阴影、点击及尺寸 Modifier |
| `status` | `LinearProgress`、`CircleProgress` |
| `text` | Text、FastText、Input、PasswordInput、StrokeText、SelectionBox |

组件应置于 `Application.ComposedLayout()` 或显式 `Theme` 下。

## 按钮与挂起操作

```kotlin title="按钮"
PrimaryButton(text = "保存", onClick = ::save)
SecondaryButton(text = "预览", onClick = ::preview)
TertiaryButton(text = "取消", onClick = { pop() })
```

普通 Button 与 TextButton 的 `onClick` 是普通函数，不能直接调用挂起操作。页面中通过 `launch` 建立任务：

```kotlin title="在页面中提交"
PrimaryButton("提交") {
    launch {
        submit()
    }
}
```

`PrimaryLoadingButton`、`SecondaryLoadingButton`、`TertiaryLoadingButton` 与 `LoadingTextButton` 提供挂起回调。请求状态与控件加载状态应明确所有者，避免重复提交或互相覆盖。

## 输入与状态提升

Switch、CheckBox、TriStateCheckBox、Radio、Slider 与 ComboBox 通过参数与回调传递状态。文本输入使用 `InputState`、`PasswordInputState` 等对象保存编辑状态。

```kotlin title="输入"
val state = remember { InputState() }

Input(
    state = state,
    hint = "请输入名称",
    modifier = Modifier.fillMaxWidth(),
)
```

格式、长度与界面提示属于交互校验；提交时仍需进行业务校验。显示文本不应替代实际业务数值或标识。

## 状态容器

`StatefulBox` 使用 `StatefulProvider.status` 选择内容：

| 状态 | 内容 |
| --- | --- |
| `Content` | 正常内容 |
| `Custom` | 自定义布局 |
| `Empty` | 空数据 |
| `Loading` | 加载状态 |
| `NetworkError` | 网络异常 |

`DefaultStatefulProvider` 提供默认布局，也可以实现自己的 Provider。`ReplaceableBox` 用于内容替换，`AdderBox` 用于内容与添加入口组合。

```kotlin title="状态容器"
StatefulBox(
    provider = rememberDefaultStatefulProvider(status),
) {
    Content()
}
```

`Surface` 统一形状、边框、阴影、内容边距与色调层次。组件实现应优先复用这些能力与主题值。

## 图片模块

`compose/components/image` 提供 `BasicImage`、`WebImage`、`LocalFileImage` 与 `ResourceFileImage`。使用前需要初始化图片管理器：

```kotlin title="初始化"
ImageManager.initialize(context, appName, ImageConfiguration())
// 应用退出时：
ImageManager.shutdown()
```

业务模块通常把这两个调用封装为应用级服务。管理器只能成功初始化一次，关闭后不能重新初始化；未就绪时图片组件显示空占位。

| 入口 | 输入 |
| --- | --- |
| `WebImage` | 可空网络地址；空白地址显示空占位 |
| `LocalFileImage` | 框架 `File` |
| `ResourceFileImage` | Compose 资源 URI，例如 `Res.getUri(...)` |
| `BasicImage` | `ImageSource`：网络、文件、资源、字节数组或自定义源 |

```kotlin title="资源图片"
val state = rememberImageState()

ResourceFileImage(
    Res.getUri(uri),
    contentDescription = description,
    modifier = Modifier.size(Theme.size.image2),
    state = state,
    options = ImageOptions(animated = false),
)
```

每个显示位置持有独立的 `rememberImageState()`。同源、同版本与同解码选项的并发加载可以共享结果，各位置分别管理动画与缩放。

### 缓存与版本

| 概念 | 说明 |
| --- | --- |
| 内存缓存 | 保存解码结果，按尺寸、画质与平台选项匹配 |
| 磁盘缓存 | 保存网络图片原始字节，浏览器不启用 |
| `version` | 参与缓存身份，不拼入 URL |
| `clearCache()` | 取消在途加载并清理缓存，已显示结果不会自动刷新 |

输入、版本或重试变化会重新计算加载显示时间。本地文件内容变化时应更新版本；可变网络资源也应使用业务可观察的版本来源。

### 尺寸、画质与绘制

默认使用主题约定的画质。每次请求取首个有效布局的物理像素尺寸，结合 `ContentScale`、原图尺寸与像素预算确定解码大小。请求不变时，后续布局缩放只改变绘制范围，不自动重新解码。

| 选项 | 行为 |
| --- | --- |
| `fadeIn` | 首次成功加载淡入，缓存命中与重组不重播 |
| `animated` | 控制 GIF / WebP 是否播放 |
| `zoom` | 双击、双指、滚轮与拖动 |
| `Circular` | 圆形裁剪 |
| `alpha`、`colorFilter` | 绘制外观 |

`Modifier.cropImageRegion(...)` 显示裁剪选区，结果是相对于原图的比例坐标，不是已经生成的文件。

### 平台约束

- Android 静态解码使用系统解码器，硬件位图不能直接读取像素；
- 浏览器不启用图片磁盘缓存；
- 不同平台的色彩处理、缩略采样与动图后端可能产生不同像素结果；
- 自定义解码器应遵循 `targetSize`、原始字节与像素预算，并释放原生资源。

## 调色板

```kotlin title="取色"
val palette = rememberPalette(bitmap)
```

`PaletteState` 提供主色、标准目标色、Swatch 集合（含 HSL 与文字对比色）以及加载和错误状态。读取这些属性会订阅快照状态。图像实例变化时旧任务自动取消并重新分析，刷新期间保留最近一次成功结果。

独立使用时可配置 `Palette.Builder`：

```kotlin title="Builder"
Palette.from(bitmap) {
    maximumColorCount = 12
    // 采样区域、过滤器与目标色
}.generate()
```

模块仅依赖 `commonMain`，不依赖 `androidx.palette`。自定义目标色可通过 `Target { ... }` 创建。

## 可选组件

| 模块 | 能力 |
| --- | --- |
| `calendar` | 日历、农历与日期状态 |
| `data-table` | 表格、单元格、排序、编辑与选择 |
| `drag-drop` | 拖放交互 |
| `hexview` | 十六进制数据查看 |
| `image` | 异步图片、GIF / WebP、缩放、缓存与裁剪 |
| `log-output` | 日志输出 |
| `lottie` | Lottie 动画 |
| `media` | 音频、视频与播放控制 |
| `pagination-layout` | 分页加载 |
| `palette` | ImageBitmap 取色与文字对比色 |
| `qrcode` | 二维码生成与扫码 |
| `reordering` | 列表重排 |
| `rich-text` | 富文本渲染 |
| `webview` | 原生网页视图 |

这些模块需按使用范围添加项目依赖，平台覆盖不完全相同。

### 平台边界

| 模块 | 当前边界 |
| --- | --- |
| `media` | 未启用桌面 Native；JVM 的 Linux/macOS 视频后端可能为空实现 |
| `webview` | Android、iOS、Web 有视图实现；JVM 显示不支持占位；桌面 Native 未启用 |
| `qrcode` | 所有目标可生成二维码。Android、iOS 和 Web 支持相机扫码与选图识别。Web 相机需要安全上下文和用户授权，选图识别在浏览器本地完成。 |
| `drag-drop` | iOS 当前返回原 Modifier；桌面 Native 使用 SDL 与 Compose 命中检查 |
| `platform-view` | 公共状态层包含桌面 Native，但尚无原生控件嵌入 Host |

平台声明不保证权限、外部服务或运行资源已经就绪。媒体与系统视图的原生资源由对应所有者释放。

## 下一步

继续阅读[导航与页面](navigation.md)，了解如何组织页面、参数与状态。
