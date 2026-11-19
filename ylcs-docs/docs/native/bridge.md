---
description: 安装 Native Bridge 插件，配置桌面 Native 目标、Compose 依赖、窗口与入口。
---

# 桥接与窗口

<p class="rc-lead">桥插件的职责很具体：在 Native 目标上把官方 Compose 依赖重定向到 Rachel 移植工件，并提供窗口、事件循环与分发运行时。应用侧只需要声明目标和依赖。</p>

## 安装插件

```kotlin title="build.gradle.kts"
plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
    )
}

rachel {
    multiplatform {
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        sources {
            commonMain {
                lib(libs.compose.material3)
            }
        }
    }
}
```

| 插件 | 作用 |
| --- | --- |
| `nativeBridge` | 替换 Native Compose 依赖、窗口运行时与分发 |
| `nativeBridgeSettings` | Settings 级配套，通常由仓库配置 |

JVM 与 Android 目标继续使用官方 Compose；桥插件只影响 Native target。

## Compose 依赖

在 `commonMain` 中按普通方式声明官方 Compose 依赖：

```kotlin title="常用依赖"
commonMain {
    lib(
        libs.compose.ui,
        libs.compose.foundation,
        libs.compose.material3,
    )
}
```

| 能力 | 依赖 |
| --- | --- |
| 基础 UI 与 Foundation | `libs.compose.ui`、`libs.compose.foundation` |
| Material3 | `libs.compose.material3` |
| 窗口尺寸类 | `libs.compose.material3.windowSizeClass` |
| Adaptive | `libs.compose.material3.adaptive.*` |
| 资源 | 官方 Compose Resources 插件与 `Res` |

Material3 的基础 UI、Foundation 与 Ripple 会随依赖图引入。不要手动替换为 Rachel 内部坐标，桥插件会完成重定向。

## 入口

```kotlin title="nativeMain/Main.kt"
private val appReference = LazyReference<DesktopApplication>()

class DesktopApplication(
    override val commandArgs: Array<String>,
) : PlatformApplication<DesktopApplication>(appReference) {

    override val initPosition = WindowPosition.Aligned(Alignment.Center)
    override val initPlacement = WindowPlacement.Floating

    @Composable
    override fun Content() {
        // 应用内容
    }
}

fun main(args: Array<String>) = DesktopApplication(args).run()
```

Native 入口不自动读取或解析进程参数，`commandArgs` 由入口显式传入。

## NativeWindow

桌面入口位于 Native 源集，底层使用 `androidx.compose.ui.window.NativeWindow`。`NativeWindowOptions` 支持创建时的 `transparent` 与 `undecorated`，默认均为 `false`。

| 能力 | 说明 |
| --- | --- |
| 透明 | 贯通 SDL 与 D3D12 / Vulkan / Metal 的 alpha 缓冲 |
| 无边框 | 使用 Compose 自绘标题栏 |
| 焦点 | `focusable`，默认 true |
| 缩放 | `resizable`，默认 true |
| 鼠标穿透 | `clickThrough`，默认 false |

```kotlin title="窗口选项"
NativeWindow(
    onCloseRequest = ::close,
    title = "示例应用",
    options = NativeWindowOptions(
        transparent = true,
        undecorated = true,
    ),
) {
    App()
}
```

透明窗口需要 Compose 内容使用透明背景，Linux 还需要支持透明合成的窗口管理器与 Vulkan surface。Wayland 普通顶层窗口的 `focusable` 配置只保存赋值，不修改焦点能力。

## 窗口控制器

`WindowController` 提供运行期窗口状态：

| 属性 | 说明 |
| --- | --- |
| `icon` | 窗口图标 |
| `undecorated` | 系统装饰开关 |
| `roundedCorner` | 圆角 |
| `position`、`placement` | 位置与最大化/全屏 |
| `focusable`、`resizable`、`clickThrough` | 输入与缩放 |
| `tray` | 托盘图标与提示 |

`transparent` 是创建参数，不能作为普通运行期状态修改。Wayland 不开放全局窗口定位，`isPositionSupported` 为 false。

## 拖动区域

`DragArea` 创建跟随组合生命周期的拖动区域：

```kotlin title="标题栏拖动"
DragArea(
    enabled = !controller.maximize,
    onDoubleClick = { controller.toggleMaximize() },
    modifier = Modifier.fillMaxWidth(),
) {
    TopBar(controller, onMainWindowClose)
}
```

区域离开组合时注销注册，窗口或拖动配置改变时更新绑定。需要额外拖动区域的组件应传入当前窗口作用域，避免持有其他窗口的 scope。

## 托盘

```kotlin title="托盘"
controller.tray.visible = true
controller.tray.tooltip = "示例应用"
```

| 状态 | 含义 |
| --- | --- |
| `isActive` | 图标是否已创建 |
| `isAvailable` | 最近一次创建结果，尚未尝试时为 null |

桌面未提供托盘服务时跳过创建。Linux 需要可用的 GTK/AppIndicator 与桌面托盘服务。当前只开放图标、提示文字与显隐，点击、菜单与通知尚未接入。

## 主线程调度

Windows 与 Linux 的 UI 工件在应用启动时自动注册 `Dispatchers.Main`，窗口创建后将任务投递到同一个 Native UI 事件队列。`MainScope()`、`Dispatchers.Main.immediate` 与 `Coroutines.main` 无需额外初始化。

窗口启动前可以创建作用域并提交任务，任务会等待事件循环开始。关闭窗口后新的 Main 任务会被取消；重新打开窗口会接入新的 UI 队列。应用自行创建的作用域仍由应用负责取消。

macOS 保留 coroutines 的系统主队列。

## 资源

- Compose 资源沿用 `src/commonMain/composeResources` 与生成的 `Res`；
- 应用分发目录需要包含 Compose 资源与 ICU 数据；
- 图标支持位图、XML Vector 与 SVG，由 Painter 栅格化；
- `NativeWindow` 创建场景前会自动注册 Skiko 图形与文本后端。

如果在窗口创建前直接使用 `Path()`、`ImageBitmap()` 或文本排版 API，需要自行完成 `registerSkikoComposeImplementation()`。

## 平台差异

| 平台 | 注意点 |
| --- | --- |
| Windows | 需要 MSVC、Windows SDK；透明与穿透使用 layered/transparent 扩展样式 |
| Linux | X11 与 Wayland 的句柄不可互换；位置与装饰能力不同 |
| macOS | 使用 NSWindow；Dock 图标与 `.app` 分发 |
| Wayland | 不支持全局定位；`focusable` 配置不生效 |

## 下一步

继续阅读[分发与维护](distribution.md)，完成 Native 应用的资源、符号与发布。
