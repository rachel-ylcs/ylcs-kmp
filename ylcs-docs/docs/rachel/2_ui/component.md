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
