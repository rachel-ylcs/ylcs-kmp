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
| `webview` | 原生网页视图与状态 |

这些模块需按使用范围添加项目依赖。日历已位于独立模块，不属于基础 `compose:ui` 的 `widget` 包内容。各模块目标与平台实现以其构建脚本和源集为准。

## 可选组件的平台边界

可选组件不具有完全相同的平台覆盖，完整目标矩阵见[仓库首页](https://github.com/rachel-ylcs/ylcs-kmp#全平台模块支持)。当前实现还存在以下功能差异：

| 模块 | 当前边界 |
| --- | --- |
| `media` | 配置 Android、iOS、Desktop JVM、JS、WasmJS，没有桌面 Native target；JVM 的 Linux/macOS 视频后端为空实现 |
| `webview` | Android、iOS、Web 有视图实现；JVM 只显示不支持占位，桌面 Native 未启用 |
| `qrcode` | 所有已声明目标可生成二维码；扫码界面仅 Android/iOS 有实现，其余为占位 |
| `drag-drop` | iOS 的 `dragDrop` 当前直接返回原 Modifier，不接收拖放；桌面 Native 使用 SDL 与 Compose 的目标命中检查 |
| `platform-view` | 公共状态管理层包含桌面 Native target，但尚无对应原生控件嵌入 Host |

平台声明不保证权限、外部服务或运行资源已经就绪。媒体与系统视图的原生资源由对应所有者释放，不能把 `remember` 当作全部平台资源的清理机制。

## 图片初始化与输入

`compose/components/image` 提供 `BasicImage`、`WebImage`、`LocalFileImage` 和 `ResourceFileImage`。当前工程已经移除 `url-image` 与 Sketch 接入。

使用方需管理 `ImageManager.initialize(context, appName, configuration)` 与 `shutdown()` 的生命周期。业务模块 `app/global` 的 `StartupImage` 以同步服务封装这两个调用；它不是基础库提供的 Startup API。Gallery 在共享应用中用 `sync` 执行初始化，见[应用入口](entry.md)。管理器只能成功初始化一次，关闭后不能重新初始化。未就绪时图片组件显示空占位，初始化完成后开始加载。

| 入口 | 输入 |
| --- | --- |
| `WebImage` | 可空网络地址；空白地址显示空占位 |
| `LocalFileImage` | 框架 `File` |
| `ResourceFileImage` | Compose 资源 URI，例如 `Res.getUri(...)` |
| `BasicImage` | `ImageSource`，包括 Network、文件、资源、字节数组及已注册的自定义源 |

自定义请求头使用 `ImageSource.Network` 并交给 `BasicImage`。每个显示位置持有独立的 `rememberImageState()`；同源、同版本及同解码选项的并发加载可共享结果，各位置分别管理动画和缩放。

## 图片预算与加载状态

`ImageConfiguration` 的当前默认配置为：

| 配置 | 默认值 | 含义 |
| --- | --- | --- |
| `maxMemoryCacheBytes` | 64 MiB | 解码结果的内存缓存预算 |
| `maxDiskCacheBytes` | 256 MiB | 网络原始字节的磁盘缓存预算 |
| `maxSourceBytes` | 64 MiB | 单个输入的原始字节上限 |
| `maxDecodePixels` | 64 × 1024 × 1024 像素 | 解码像素预算，允许范围为 1 至该上限 |
| `loadingDelayMillis` | 500 ms | loading 占位的显示门槛，0 表示立即显示 |
| `cacheDirectory` | `null` | 非浏览器平台使用默认应用缓存目录；可显式覆盖 |

浏览器不启用图片磁盘缓存。预算描述不同层面的资源，缓存预算不等于应用全部图片内存的硬上限，已显示图片也会持有解码结果。

加载进度通过 state 读取；响应没有 Content-Length 时 `progress.total` 为 `null`。进度按约 100 ms 更新，成功前同步最终字节数。loading 门槛只控制占位显示，延迟期间仍下载并收集进度；失败时立即显示错误占位。

`state.retry()` 重新发起请求并允许复用缓存。默认读取失败占位提供重试，解码失败占位没有默认重试入口；自定义交互可以自行决定是否再次加载。输入、版本或重试变化会重新计算加载显示时间。

## 图片缓存与版本

`version: String?` 默认 `null`，参与缓存身份，不拼入 URL 或文件路径。`null` 表示不附加显式版本，空字符串是一个独立版本。本地文件内容发生变化时，应更新版本；可变网络资源也应采用业务可观察的版本来源。

内存缓存保存解码结果，磁盘缓存保存网络图片的原始字节，不按显示尺寸创建多份磁盘文件。内存结果还按尺寸、画质、动图模式和平台解码选项匹配，尺寸足够的兼容结果可以复用。

`ImageManager.clearCache()` 取消在途加载并清理两级缓存，已经显示的结果不会自动刷新。需要刷新某个显示位置时，更新输入、版本或显式触发该 state 的重试，而不是只清空磁盘目录。

## 图片尺寸、画质与绘制

默认 `ImageQuality.Medium`；`ImageOptions.quality` 统一选择平台重采样和绘制滤波策略。每次请求取首个有效布局的物理像素尺寸，结合 `ContentScale`、原图尺寸和像素预算确定解码大小，零尺寸会等待有效布局。

请求不变时，后续布局缩放只改变绘制范围，不自动重新解码。大幅放大可能降低清晰度，缩小后也仍持有原解码结果。输入、版本、画质、动图模式、`ContentScale` 变化或显式重试会使用当时的布局重新请求。开启缩放查看后，静态图还可按缩放档位请求更多像素。

`ImagePlaceholder` 决定没有显式尺寸时的保留空间，成功后保持容器尺寸。组件不绘制背景，`ContentScale` 与 `alignment` 决定内容区域，留白不触发图片 `onClick`。`alpha` 和 `colorFilter` 控制绘制外观，圆形裁剪使用 `ImageOptions.Circular` 或 `circular = true`。

## 图片动画、缩放与裁剪

淡入默认开启，可用 `ImageOptions(fadeIn = false)` 关闭。当前输入和版本首次成功加载时使用主题动画时长；重组、保留 state 重新挂载和内存缓存命中不重播。淡入不改变 `alpha` 的公开语义。

GIF / WebP 按文件循环配置播放，`ImageOptions(animated = false)` 仅显示首帧。`ImageOptions(zoom = true)` 开启双击、双指、滚轮和放大后拖动，也可通过 `state.zoom` 控制。`Modifier.cropImageRegion(imageState, cropImageState, aspectRatio)` 显示裁剪选区，结果为相对于原图的比例坐标，并非已经生成裁剪后的文件。

Android 动态 WebP 默认使用系统 `AnimatedImageDrawable`；系统停止后保留画面，再次启动从头播放。前后台、可见性和卸载会影响播放，不能假设动图在离屏期间持续推进到同一帧。布局改变只缩放现有结果，不因此重建播放器。

## 图片格式与平台约束

内置格式包括 JPEG、PNG、BMP、GIF 和 WebP。BMP 会先检查头部、像素偏移与尺寸预算，具体调色板和 bitfields 变体仍由平台解码器决定。可通过 `ImageConfiguration.inputs` 与 `formats` 注册自定义输入和格式处理器。

Android 最低 API 28，静态解码使用 `ImageDecoder`。兼容的硬件加速显示目标自动使用硬件位图，预览和已知软件绘制目标使用软件请求。硬件位图不能直接读取像素，也不能直接绘制到软件 Canvas；额外执行软件截图时需考虑这一限制。不同平台的色彩处理、缩略采样与动图后端可能产生不同像素结果。

读取和解码使用框架的 IO / CPU 上下文，浏览器上下文不等同于独立 Worker。自定义解码器应遵循 `targetSize`、原始字节与像素预算，并明确释放原生资源；旧版 `sizeMultiplier` 不是当前 API。

## Gallery 中的资源图片

以下片段来自 `ImagePage.ResourceExamples` 的每个图片显示位置；资源、state 与是否播放动图分别配置：

示例来源：[ImagePage.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/gallery/src/commonMain/kotlin/love/yinlin/page/ImagePage.kt#L90)。

```kotlin
val state = rememberImageState()
ResourceFileImage(
    Res.getUri(uri),
    contentDescription = description,
    modifier = Modifier.size(Theme.size.image2),
    state = state,
    options = ImageOptions(animated = false),
)
```

`uri`、`description` 来自外层样本列表，`Res` 由 Gallery 模块生成。每个显示位置创建自己的 state；同一 state 不应被多个同时显示的位置共享。图片管理器需先初始化。

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
