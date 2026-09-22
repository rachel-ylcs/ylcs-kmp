# UI 组件

`love.yinlin.compose:ui` 是一套直接基于 Compose Foundation 构建的跨平台组件，不要求 Material。组件默认读取 Rachel 主题令牌，因此接在 `Application.ComposedLayout` 或 `Theme` 下即可获得一致的颜色、尺寸、动画和文案。

可运行用法以 `ylcs-app/gallery` 为准：

```powershell
.\gradlew.bat :ylcs-app:gallery:galleryRun
```

## 基础组件地图

| 包 | 代表组件 | 用途 |
| --- | --- | --- |
| `animation` | `CircleLoading`、`WaveLoading`、`ExpandableContent` | 加载与内容切换 |
| `collection` | `TagView`、`TreeView` | 标签流与树结构 |
| `container` | `Surface`、`StatefulBox`、`AdaptiveTwoBox`、`Banner` | 表面、状态容器与响应式布局 |
| `floating` | Dialog、Sheet、Tip、FAB、Flyout、Menu | 覆盖层与临时交互 |
| `image` | `Icon`、`Image`、`NineGrid`、`LoadingIcon` | 本地图形与图片布局 |
| `input` | Button、Switch、CheckBox、Radio、Slider、ComboBox、Filter、ColorPicker | 输入控件 |
| `layout` | Divider、Space 与测量辅助 | 基础排版 |
| `navigation` | Breadcrumb、TabBar | 区域内导航 |
| `node` | condition、点击、阴影、动画等 Modifier | 行为与绘制修饰 |
| `status` | Progress 等 | 进度与状态呈现 |
| `text` | Text、FastText、Input、PasswordInput、StrokeText | 文本与编辑 |
| `widget` | Calendar | 复合业务控件 |

## 按钮与输入

按钮分为主、次、三级语义，并有 loading/text 变体。选择样式时按操作优先级，而不是仅凭颜色：

```kotlin
PrimaryButton(text = "保存", onClick = ::save)
SecondaryButton(text = "预览", onClick = ::preview)
TertiaryButton(text = "取消", onClick = ::pop)
```

事件回调通常支持挂起调用，页面中可直接执行请求或对话框流程。仍要在组件提供的 loading 语义和页面任务之间保持单一状态源，避免按钮自身 loading 与外层 `isLoading` 互相覆盖。

`Switch`、`CheckBox`、`TriStateCheckBox`、RadioGroup、`Slider` 和 `ComboBox` 都采用状态提升。复杂值通过 `SliderConverter` 等适配为显示/滑动域；不要把格式化字符串当真实业务值保存。

## 容器与状态

`Surface` 统一背景、内容色、边框、形状、阴影和 tonal level，是构建自定义控件的起点。

`StatefulBox` 适合 Loading/Empty/Error/Content 四类内容状态；`ReplaceableBox` 用于带动画替换内容；`AdderBox` 表达“现有内容 + 添加入口”；`ActionScope` 统一左右动作布局。

`AdaptiveTwoBox` 会根据设备形态在单列和双区间切换。把同一状态和回调传给两个布局位置，避免横竖屏分支各自维护一份选择状态。

## 图片与文本

基础 `Image` 处理 Compose 资源；网络图片由可选的 `url-image` 模块提供 `WebImage` 并依赖 `StartupUrlImage`。两者生命周期和缓存语义不同，不应只按函数名替换。

`Text` 继承 `LocalStyle` / `LocalColor`，支持字符串和 `AnnotatedString`；`FastText` 针对更轻的绘制路径；`SimpleEllipsisText` 是常用单行截断。可编辑内容使用 `InputState` 管理长度和选择等规则，业务校验仍在提交边界完成。

## 可选组件模块

这些能力不由 `compose:ui` 全部导出，应按需添加依赖：

| 模块 | 能力 |
| --- | --- |
| `compose.components:drag-drop` | 文件/内容拖放 |
| `compose.components:lottie` | Lottie 动画 |
| `compose.components:media` | 音频、视频、播放器服务 |
| `compose.components:pagination-layout` | 分页加载布局 |
| `compose.components:qrcode` | 二维码生成/扫描相关 UI |
| `compose.components:rich-text` | 富文本渲染 |
| `compose.components:url-image` | Sketch 网络图片与缓存 |
| `compose.components:webview` | 跨平台 WebView 状态与视图 |

模块坐标遵循 `love.yinlin.compose.components:<artifact>:3.7.0`。平台支持以各模块源集为准；例如某个目标可能提供退化实现，而不是与 Android 完全相同的原生能力。

## 组合组件的原则

1. 从 `Surface`、主题令牌和已有 Modifier 开始，不复制颜色/圆角常量。
2. 数据状态向下传、事件向上传；需要跨页面的数据放 DataSource/Startup。
3. 组件负责交互状态，页面负责业务状态和错误策略。
4. 响应式变化移动同一内容或共享状态，不创建互不一致的两套页面。
5. 原生视图、网络图片、媒体等有资源所有权的能力，遵守各自释放契约。

组件 API 会随仓库演进。Gallery 展示的是当前组合方式，Dokka 则适合核对精确参数；本页负责说明选择和组合思路。
