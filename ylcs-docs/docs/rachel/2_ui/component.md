# UI 组件

`compose:ui` 提供基于 Compose Foundation 的跨平台组件，并通过 Rachel 主题统一颜色、排版、尺寸与交互配置。可选复合组件位于独立模块。

## 基础组件

| 包 | 代表能力 |
| --- | --- |
| `animation` | `CircleLoading`、`WaveLoading`、`ExpandableContent` |
| `collection` | `TagView`、`TreeView` |
| `container` | `Surface`、`StatefulBox`、`AdaptiveTwoBox`、`Banner`、`ActionScope` |
| `floating` | Dialog、Sheet、Tip、FAB、Flyout、Menus、BalloonTip |
| `image` | `Icon`、`Image`、`NineGrid`、`LoadingIcon` |
| `input` | Button、Switch、CheckBox、Radio、Slider、ComboBox、Filter、ColorPicker |
| `layout` | Divider、Space 与测量辅助 |
| `navigation` | Breadcrumb、TabBar |
| `node` | 条件、语义、绘制、阴影、点击及尺寸 Modifier |
| `status` | `LinearProgress`、`CircleProgress` |
| `text` | Text、FastText、Input、PasswordInput、StrokeText、SelectionBox |

组件应置于 `Application.ComposedLayout()` 或显式 `Theme` 下。主题配置见[主题系统](theme.md)。

## 按钮与挂起操作

```kotlin
PrimaryButton(text = "保存", onClick = ::save)
SecondaryButton(text = "预览", onClick = ::preview)
TertiaryButton(text = "取消", onClick = { pop() })
```

普通 Button 和 TextButton 的 `onClick` 为普通函数，不能直接调用挂起操作。页面中可以通过 `launch` 建立任务：

```kotlin
PrimaryButton("提交") {
    launch {
        submit()
    }
}
```

`PrimaryLoadingButton`、`SecondaryLoadingButton`、`TertiaryLoadingButton` 与 `LoadingTextButton` 提供挂起回调。页面请求状态与控件加载状态应明确所有者，避免重复提交或互相覆盖。

## 输入与状态提升

Switch、CheckBox、TriStateCheckBox、Radio、Slider 和 ComboBox 通过参数与回调传递状态。文本输入使用 `InputState`、`PasswordInputState` 等对象保存编辑状态。

格式、长度和界面提示属于交互校验，提交时仍需进行业务校验。显示文本不应替代实际业务数值或标识。

## 状态容器

`StatefulBox` 使用 `StatefulProvider.status` 选择内容，当前状态为：

- `Content`：正常内容。
- `Custom`：自定义布局。
- `Empty`：空数据。
- `NetworkError`：网络异常。
- `Loading`：加载状态。

`DefaultStatefulProvider` 提供默认布局，也可实现自己的 Provider。`ReplaceableBox` 用于内容替换，`AdderBox` 用于内容与添加入口组合，`AdaptiveTwoBox` 用于自适应区域布局。

`Surface` 统一形状、边框、阴影、内容边距及色调层次。组件实现应优先复用这些能力与主题值。

## 可选模块

| `compose/components` 子模块 | 能力 |
| --- | --- |
| `calendar` | 日历、农历及日期状态 |
| `data-table` | 表格、单元格、排序、编辑及选择 |
| `drag-drop` | 拖放交互 |
| `hexview` | 十六进制数据查看 |
| `image` | 独立实现的异步图片加载、GIF / WebP 动画、缩放、缓存与裁剪选区 |
| `log-output` | 日志输出组件 |
| `lottie` | Lottie 动画 |
| `media` | 音频、视频及播放控制 |
| `pagination-layout` | 分页加载布局 |
| `qrcode` | 二维码相关组件 |
| `reordering` | 列表重排 |
| `rich-text` | 富文本渲染 |
| `url-image` | 网络图片、Sketch 与缓存 |
| `webview` | 原生网页视图与状态 |

这些模块需按使用范围添加项目依赖。日历已位于独立模块，不属于基础 `compose:ui` 的 `widget` 包内容。各模块目标与平台实现以其构建脚本和源集为准。

## 异步图片

`compose/components/image` 提供 `BasicImage`、`WebImage`、`LocalFileImage` 和 `ResourceFileImage`，与基于 Sketch 的 `url-image` 模块分开。使用前调用 `ImageManager.initialize(context, appName, configuration)`；管理器只能成功初始化一次，`shutdown()` 释放资源后也不能重新初始化。管理器未就绪时，图片组件显示空占位。

`BasicImage` 接受网络、本地文件、Compose 资源、字节数组及已注册的自定义 `ImageSource`。网络请求头通过 `ImageSource.Network` 传入。每个显示位置单独持有 `rememberImageState()`；同源、同版本、同解码参数的并发请求共享加载结果，各位置独立控制动画播放和缩放。

`version: String? = null` 是可选缓存版本，不拼入 URL 或文件路径。为 `null` 时只根据输入源识别原始内容；非空值时附加版本键，空字符串也是一个明确版本，与 `null` 区分。解码结果仍按目标尺寸、画质采样倍率与动图模式分别缓存。本地文件内容改变后需设置或更新 `version`。`state.retry()` 允许复用已有缓存；读取失败可重试，解码失败时默认占位不提供重试入口。内存缓存保存解码结果，磁盘缓存保存网络图片原始字节，浏览器不启用图片磁盘缓存。`ImageManager.clearCache()` 取消在途加载并清理两级缓存；已经显示的图片不会自动刷新。网络进度按约 50ms 间隔更新，加载成功返回前同步最终字节数；响应没有内容长度时 `progress.total` 为 `null`。

默认画质为 `ImageQuality.Medium`。Low、Medium、High、Full 的目标采样倍率依次为 1、2、3、4，实际解码尺寸还受原图尺寸和像素预算限制。`ImagePlaceholder` 决定未显式指定尺寸时的保留空间，成功加载后保持容器尺寸。组件不绘制背景，也不提供背景配置；空态图标、加载圈、读取失败和损坏图标均保持透明底色；`ContentScale` 和 `alignment` 决定图片绘制范围，留白不触发图片的 `onClick`。

淡入默认关闭，可通过 `ImageOptions(fadeIn = true)` 开启。开启后，当前输入源和版本从空内容首次成功加载时使用主题默认时长淡入。重组、滚出后返回、保留状态重挂载、内存缓存命中及已显示后开启淡入均不重播；只有需要首次淡入时才创建动画状态和启动 Effect。尺寸变化或结果替换直接结束已有淡入，`alpha` 仍独立控制图片透明度。

GIF / WebP 按文件循环次数逐帧播放，`ImageOptions(animated = false)` 只显示首帧。缩放通过 `ImageOptions(zoom = true)` 开启，支持双击、双指、滚轮与放大后的拖动，也可调用 `state.zoom`。`Modifier.cropImageRegion(imageState, cropImageState, aspectRatio)` 添加裁剪选区，`cropImageState.result` 返回相对于原图的比例坐标。

请求由布局节点管理，尺寸就绪后立即尝试内存缓存，读取和解码使用框架的 IO / CPU 协程上下文；能否并行执行由平台运行时决定，浏览器默认上下文不等同于独立 Worker。普通静态图在绘制节点观察结果；缩放、淡入和动图使用独立的 Composable。缩放状态在启用缩放或显式访问 `state.zoom` 时创建，此后持续同步边界。静态图不注册动图的可见性监听；未配置点击、缩放或淡入时不创建对应的交互或动画辅助状态。`state.contentRect` 和裁剪使用的几何信息按需计算并缓存，尺寸、内容和显示参数改变后自动失效，不依赖每次重组的 `SideEffect`。

内置字节数组输入复用 `ImageSource.Bytes` 已持有的防御性副本，跳过无 IO 的原始读取任务；自定义输入仍保留读取、进度和共享语义。稳定长度的本地文件与已知长度的网络响应直接填充最终数组；未知长度或读取期间长度改变时使用有界缓冲，始终受原始字节预算限制，不额外缓存整图字节来换取速度。

Skia 实现对 JPEG 缩略图使用原生 1/2、1/4、1/8 预采样，再绘制到目标尺寸，保留 EXIF 方向；非 sRGB ICC 图片沿用完整解码路径。预采样保留目标尺寸约定，像素结果可能与完整解码后缩放不同。

静态 WebP 从容器标志识别后直接进入静态解码，避免额外打开动画解码器。Skia 动图保留可复用的工作帧，只在同步栅格绘制期间借用其像素，绘制结束即关闭借用视图；返回给 Compose 的帧是独立的不可变位图，不会被下一帧解码修改。

## 示例与 API

```powershell
.\gradlew.bat :ylcs-app:gallery:galleryRun
```

Gallery 展示当前组件交互，`GalleryCatalog.kt` 维护分类、搜索与概览信息。精确函数签名可查看模块源码或生成的 Dokka 页面。

网络图片和媒体等组件具有服务初始化与资源释放要求；平台原生组件的状态归属见[平台组件](platform_component.md)，浮层结果与注册方式见[浮层](floating.md)。

## 源码入口

- `ylcs-module/compose/ui/src/commonMain/kotlin/love/yinlin/compose/ui/`
- `ylcs-module/compose/components/`
- `ylcs-app/gallery/src/commonMain/`
