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

`version: String? = null` 是可选缓存版本，不拼入 URL 或文件路径。为 `null` 时只根据输入源识别原始内容；非空值时附加版本键，空字符串也是一个明确版本，与 `null` 区分。解码结果按目标尺寸、填充方式、画质与动图模式分别匹配内存缓存；新请求可以复用同内容、同画质和同动图模式下尺寸足够的已有结果。本地文件内容改变后需设置或更新 `version`。`state.retry()` 允许复用已有缓存；读取失败可重试，解码失败时默认占位不提供重试入口。内存缓存保存解码结果，磁盘缓存保存网络图片原始字节，不按显示尺寸生成磁盘副本；浏览器不启用图片磁盘缓存。`ImageManager.clearCache()` 取消在途加载并清理两级缓存；已经显示的图片不会自动刷新。网络进度按约 100ms 间隔更新，加载成功返回前同步最终字节数；响应没有内容长度时 `progress.total` 为 `null`。

`ImageConfiguration(loadingDelayMillis = 500L)` 设置 loading 占位的显示门槛，默认 500ms，设为 `0L` 可立即显示。门槛覆盖默认和自定义 `ImagePlaceholder.loading`，快速成功的图片不会创建加载圈；失败仍立即显示错误占位。延迟期间继续收集真实字节进度，因此加载圈首次显示时使用当前下载比例。未知总长度时，默认旋转圈从本次加载已经历的时间计算初始相位；自定义动画可调用 `state.loadingElapsedMillis()`，按自身绘制节奏更新。重试、输入或版本变化会重新计时，旧请求的显示任务随取消一起结束。

默认画质为 `ImageQuality.Medium`。`quality` 是统一画质入口，内部选择平台支持的解码重采样和绘制滤波策略，不再按画质固定放大解码尺寸，也不进行尺寸分档。每次加载取首个有效布局的物理像素尺寸，结合 `ContentScale`、原图尺寸和像素预算计算解码尺寸；初始零尺寸会等待有效布局。请求不变时，后续布局变大、变小或变为零都只调整绘制范围，保持原解码结果，也不因加载期间布局变化而追加解码。这与 Coil / Sketch 的 Compose 图片组件在请求不变时的行为一致；大幅放大可能降低清晰度，缩小后仍持有原解码像素。输入、版本、画质、动图模式、`ContentScale` 变化，或调用 `state.retry()` 等重新发起请求时，才解析当时的布局尺寸并尝试缓存。显式启用缩放查看时，静态图仍可按缩放档位请求更多像素。已移除 `ImageQuality.sizeMultiplier` 和 `ImageDecodeOptions.sizeMultiplier`；自定义解码器直接使用 `targetSize`。`ImagePlaceholder` 决定未显式指定尺寸时的保留空间，成功加载后保持容器尺寸。组件不绘制背景，也不提供背景配置；空态图标、加载圈、读取失败和损坏图标均保持透明底色；`ContentScale` 和 `alignment` 决定图片绘制范围，留白不触发图片的 `onClick`。

淡入默认开启，可通过 `ImageOptions(fadeIn = false)` 关闭。当前输入源和版本首次成功加载时使用主题默认时长淡入。重组、滚出后返回、保留状态重挂载、内存缓存命中及已显示后开启淡入均不重播；只有首次结果需要淡入时，绘制节点才启动短期动画协程。静态图直接调整绘制透明度，淡入结束后不保留额外内容层或动画状态；同内容重新请求后的结果替换会结束已有淡入，`alpha` 仍独立控制图片透明度。

GIF / WebP 按文件循环次数逐帧播放，`ImageOptions(animated = false)` 只显示首帧。缩放通过 `ImageOptions(zoom = true)` 开启，支持双击、双指、滚轮与放大后的拖动，也可调用 `state.zoom`。`Modifier.cropImageRegion(imageState, cropImageState, aspectRatio)` 添加裁剪选区，`cropImageState.result` 返回相对于原图的比例坐标。

请求由布局节点管理，尺寸就绪后立即尝试内存缓存，读取和解码使用框架的 IO / CPU 协程上下文；能否并行执行由平台运行时决定，浏览器默认上下文不等同于独立 Worker。普通静态图在绘制节点观察结果和执行首次淡入；缩放与动图使用独立的 Composable，动图只在 Painter 绘制时读取帧状态，逐帧更新不重新组合图片或创建 Painter。缩放状态在启用缩放或显式访问 `state.zoom` 时创建，此后持续同步边界。静态图不注册动图的可见性监听；未配置点击、缩放时不创建对应的交互状态。`state.contentRect` 和裁剪使用的几何信息按需计算并缓存，尺寸、内容和显示参数改变后自动失效，不依赖每次重组的 `SideEffect`。

内置字节数组输入复用 `ImageSource.Bytes` 已持有的防御性副本，跳过无 IO 的原始读取任务；自定义输入仍保留读取、进度和共享语义。Android / Desktop 的标准本地文件通过 JVM 文件流直接填充最终数组，减少通用分段缓冲的拷贝；调用方提供的自定义 `File` 仍使用其自身读取实现。稳定长度的本地文件与已知长度的网络响应直接填充最终数组；未知长度或读取期间长度改变时使用有界缓冲，始终受原始字节预算限制，不额外缓存整图字节来换取速度。

Skia 实现对 JPEG 缩略图使用原生 1/2、1/4、1/8 预采样，再绘制到目标尺寸，保留 EXIF 方向；非 sRGB ICC 图片沿用完整解码路径。预采样保留目标尺寸约定，像素结果可能与完整解码后缩放不同。

静态 WebP 从容器标志识别后直接进入静态解码，避免读取动画帧表。Skia 的普通 sRGB 静态图片通过 Codec 解码：同尺寸 PNG / BMP 直接移交不可变像素，WebP 直接在目标尺寸解码。PNG 缩略图沿用 Image 栅格缩放路径，避免在原生解码耗时占主导的场景增加工作位图包装；非 sRGB ICC 图片也保留原来的色彩转换路径。Skia 动图保留可复用的工作帧，WebP 工作帧按目标尺寸分配，通过 Pixmap 复制或缩放后发布独立的不可变位图，移除逐帧的 Data / Image / Canvas 中间对象。Android WebP 复用帧编码缓冲和可复用的解码位图，已发布的动画帧仍独立持有像素；静态图片和 GIF / WebP 帧在移交显示前调用 `prepareToDraw()`，提前发起系统支持的绘制准备。

Android 静态图片在兼容的硬件加速显示目标上自动请求硬件位图，不增加画质参数。API 28+ 使用 `ImageDecoder` 的硬件分配；API 26/27 使用 `BitmapFactory`，并根据问题设备、尺寸与进程级文件描述符预算回退软件位图，需要 EXIF 变换时也保留软件路径。旧系统通过原生 density 缩放尽量直接产生目标尺寸，避免额外创建缩放位图。预览、已知的软件绘制目标及软件图层使用软件请求，硬件与软件请求分别匹配解码缓存；Android 内存预算优先读取实际位图分配量，包含高精度像素和行对齐。硬件位图不可直接读写像素，也不能绘制到软件 Canvas；将已显示界面额外截取到软件画布时仍需注意这一平台限制。硬件位图优化像素存储与绘制准备，不等于 PNG / WebP 的压缩解码由 GPU 完成。

Android API 28+ 的动态 WebP 使用系统 `AnimatedImageDrawable` 播放，避免逐帧经过 Kotlin 编码、合成和发布位图。加载阶段取得首帧与系统播放器，首个显示位置可接管播放器，其他显示位置各自建立播放状态；缓存只保留可选的弱引用接管入口，不强持有播放器。暂停、组件可见性、前后台与卸载共同管理播放，停止与再次开始采用系统播放器的语义。API 26/27、系统不支持该输入及注册自定义图片格式时保留通用逐帧路径；GIF 继续使用原有实现，避免改变短帧延迟策略。公开解码器的 `frame(index)` 仍支持访问 WebP 任意帧，需要非首帧时才建立逐帧解码器。普通布局变化只缩放现有结果，不重建播放器。

内置支持 JPEG、PNG、BMP、GIF 和 WebP。BMP 使用系统 / Skia 原生解码器，进入原生解码前校验 CORE / INFO 头部、像素偏移和尺寸预算，支持正常行序与自顶向下的行序；调色板和 bitfields 的具体变体由平台解码器决定。

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
