---
description: 配置 Rachel 的目标平台、源集、平台入口、资源与权限边界。
---

# 多平台配置

<p class="rc-lead">跨平台不是把所有代码塞进一个源集，而是让共享部分足够大、平台部分足够薄。Rachel 通过模板开关、显式源集与平台入口把这三件事分开。</p>

## 目标平台

构建模板按模块选项启用目标。客户端常见目标包括 Android、iOS、JVM Desktop、JS、WasmJS，以及 Windows、Linux、macOS 的 Kotlin/Native 桌面目标。

| 目标 | 典型宿主 | 入口形态 |
| --- | --- | --- |
| Android | Android SDK | `ComposeApplication` + `ComposeActivity` |
| iOS | macOS + Xcode | Swift 宿主 + `buildUIViewController()` |
| JVM Desktop | 任意 JVM 宿主 | `PlatformApplication.run()` |
| Web | 浏览器 | `ComposeViewport` |
| Windows Native | Windows + MSVC | Native Bridge + `NativeWindow` |
| Linux Native | Linux 桌面工具链 | Native Bridge + `NativeWindow` |
| macOS Native | macOS + Xcode | Native Bridge + `NativeWindow` |
| Native Server | 目标服务器宿主 | `ServerEngine` 可执行程序 |

!!! warning "声明目标不等于具备能力"
    模块可能声明了某个平台，但某个组件仍可能没有该平台的实现。以模块构建脚本、源集和实际运行结果为准。

## 目标开关

在 `rachel { multiplatform { } }` 中按需关闭或开启目标：

```kotlin title="目标开关"
rachel {
    multiplatform {
        iosTarget = true
        desktopTarget = true
        webTarget = true
        nativeDesktopTarget = false
    }
}
```

`nativeDesktopTarget` 同时控制三个桌面 Native 目标；也可以分别使用 `windowsTarget`、`linuxTarget`、`macosTarget` 覆盖默认值。iOS 模拟器目标是否启用取决于宿主与模板配置。

## 源集划分

推荐的源集形态：

```text title="源集结构"
commonMain
├─ clientMain
│  ├─ jvmMain
│  │  ├─ androidMain
│  │  └─ desktopMain
│  ├─ appleMain
│  │  └─ iosMain
│  └─ webMain
│     ├─ jsMain
│     └─ wasmJsMain
└─ nativeMain
   └─ posixMain
```

| 内容 | 应放置的位置 |
| --- | --- |
| 业务模型、状态与纯逻辑 | `commonMain` |
| 客户端共享、不含服务端 | `clientMain` |
| Android 与 JVM Desktop 共享 | `jvmMain` |
| 浏览器共享 | `webMain` |
| 原生共享 | `nativeMain` |
| 平台入口与系统 API | 对应平台源集 |

!!! tip "优先下沉，而不是复制"
    当一个实现同时适用于多个平台时，把它提升到最近的上层源集。只有在 API 确实不同的时候，才使用 `expect/actual` 或平台分支。

## `expect` / `actual`

共享层声明能力，平台层提供实现：

```kotlin title="commonMain"
expect fun platformGreeting(): String
```

```kotlin title="androidMain"
actual fun platformGreeting(): String = "Hello from Android"
```

```kotlin title="desktopMain"
actual fun platformGreeting(): String = "Hello from Desktop"
```

修改 `expect` 声明时，必须同步所有已启用的 `actual` 实现。平台专属类型不应出现在 `commonMain` 的公开签名中。

## 平台入口与生命周期

### Android

`ComposeApplication` 创建共享实例并执行第一阶段初始化；`ComposeActivity` 设置 Activity、启用 edge-to-edge、调度第二阶段初始化并安装 Compose 内容。

系统返回、Intent 与深链需要在平台源集中转交给共享导航。页面销毁与服务清理的生命周期边界见[入口与生命周期](../ui/lifecycle.md)。

### iOS

Swift 宿主持有 Kotlin 应用实例。Kotlin 侧提供 `run()` 与 `buildUIViewController()`；Swift 侧通过 `UIViewControllerRepresentable` 挂载控制器。

```kotlin title="iosMain"
class IOSApplication : HelloApplication(PlatformContext.Instance)
```

需要窗口的能力应处理 `viewIfLoaded?.window` 为空的情况。不要假设视图加载时已经加入窗口层级。

### Web

JS 与 WasmJS 共享 `webMain`。浏览器没有进程工作目录，文件系统、剪贴板与网络能力受浏览器安全策略约束。

```kotlin title="webMain"
fun main() {
    HelloApplication(PlatformContext.Instance).run()
}
```

### 桌面 Native

桌面 Native 使用 SDL 事件循环与移植的 `NativeWindow`，不依赖 JVM。它需要：

1. 启用至少一个桌面 Native 目标；
2. 安装 Native Bridge 插件；
3. 准备对应宿主的 C/C++ 工具链；
4. 通过 `composeDesktopPackage { }` 声明分发信息。

具体配置见 [Native Compose](../native/bridge.md)。

## 资源与权限

| 平台 | 资源 | 权限 |
| --- | --- | --- |
| Android | `androidMain/res`、Compose 资源 | Manifest 与运行时权限 |
| iOS | Xcode 资源目录、Compose 资源 | Info.plist 与系统授权 |
| Desktop | Compose 资源与本地文件 | 文件系统访问由应用控制 |
| Web | 静态资源与浏览器缓存 | CORS、Cookie 与混合内容策略 |
| Native | `composeResources` 与分发目录 | 平台原生授权与系统能力 |

资源路径、缓存目录与用户数据目录是不同概念。使用 `PlatformFileSystem` 时按用途选择 `appPath`、`dataPath` 或 `cachePath`，不要假设目录已经创建。

!!! example "目录选择"
    - 需要长期保留的用户数据：数据目录；
    - 可重建的下载与缩略图：缓存目录；
    - 安装目录或可执行文件位置：不是默认可写数据目录。

## 平台差异的收敛方式

优先顺序：

1. 把平台无关逻辑提升到 `commonMain`；
2. 用接口或能力提供者隔离系统调用；
3. 用 `expect/actual` 提供最小实现；
4. 最后才使用平台分支或独立的平台入口。

这样做的目的不是追求形式统一，而是让每个平台的差异都有明确归属，便于测试和后续增加新目标。

## 下一步

配置完成后，阅读[运行与打包](run-package.md)，选择正确的任务并把应用交付到目标平台。
