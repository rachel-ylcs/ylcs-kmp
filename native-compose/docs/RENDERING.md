# Core 渲染后端与社区实现

审查日期：2026-10-24。项目版本仍以根 `libs.versions.toml` 为准；本阶段为 Skiko 0.156.0。
本文区分 API 存在、C++/KLIB 构建、离屏运行以及 GPU 窗口运行，不把其中一项替代另一项。

## 本项目路线

| 层次 | Windows x64 | Linux x64 | macOS ARM64 |
| --- | --- | --- | --- |
| Kotlin 目标 | `mingwX64` | `linuxX64` | `macosArm64` |
| 窗口、事件 | SDL3 | SDL3 | SDL3 |
| 默认窗口示例 Skia GPU 后端 | Ganesh / D3D12 | Ganesh / Vulkan | Ganesh / Metal |
| 当前自动化窗口基线 | Skia CPU 光栅 → SDL texture | 同左 | 同左 |
| 本机运行验证 | D3D12 + resize 已通过 | Kotlin 编译、C++ Linux ABI 语法检查通过，待宿主运行 | 待 macOS CI 编译和运行 |

Vulkan 是 Khronos 的跨平台 GPU API，和 Microsoft D3D12、Apple Metal 属于同一层。
Ganesh、Graphite 是 Skia 的两套 GPU 渲染架构；Skiko 在 Kotlin 层封装其接口。
SDL3 提供窗口及事件，不能因为 SDL renderer 使用某个 GPU API，就认为 Skia 已经使用同一个 GPU 后端。

Core/Ganesh 已有 Direct3D、Vulkan、Metal 的 Native 桥接入口。当前社区基线的
`DirectContext.cc` 包含真正的 `GrDirectContexts::MakeD3D/MakeVulkan/MakeMetal` 调用，
并非 Graphite 的 Vulkan 占位入口；但 API 存在仍需要对应设备、队列、交换链和同步的运行验证。
Windows 的 `DIRECT3D` 枚举在当前实现中指 D3D12。

上轮 Graphite 审计中的 Windows/Linux Vulkan 建议只适用于该扩展，不能据此要求 Core/Windows 改用 Vulkan。
Graphite 当前直接支持 Metal/Vulkan 等后端，没有可直接复用的原生 D3D12 后端。
本项目选择 Ganesh；按当前任务范围不迁移 Graphite，也不生成 Graphite publication。
Dawn 的其他后端也不等于现有 Skiko Native 绑定已经提供完整路径。

Skiko Core 的 Native 窗口实现显式对接了三个后端，样例只调用公共入口：

- Windows：SDL HWND 交给现有 Native `SkiaLayer` 和 D3D12 renderer；本机 RTX 4060 的绘制和 resize 已通过。
- Linux：新增 toolkit 无关的 Vulkan window/renderer；SDL 提供 instance extensions 和 surface factory。
  Skiko 管理设备、同一 graphics/present queue、swapchain、图像取得、呈现和 resize。每个交换链图像
  拥有自己的呈现完成 semaphore，图像取得使用有界 CPU fence 等待；resize/销毁前等待设备空闲。
  场景保存在 GPU surface，以免 screenshot 读取已交还窗口系统的图像。
  本机已编译 Linux Kotlin 源码，并通过 Linux ABI 的 C++ 语法检查；尚未在 Linux 驱动上运行。
- macOS：Core 中的 SDL host 用 Metal view 提供 `CAMetalLayer`，复用现有 `MacosMetalRenderer`。
  此接入尚需 macOS 宿主编译和运行，不能用 Windows 结果替代。

Linux 原有 host 的 OpenGL 默认值保留；示例明确要求 `VULKAN`。
Vulkan 初始化或运行失败后 layer 可以尝试既有回退，但这个示例会检测并拒绝把回退当成 GPU 测试通过。
新实现及既有平台源码位置见 [SOURCES.md](SOURCES.md)。

CPU 基线和硬件后端测试分别报告结果；显式要求某个 GPU 后端的测试应核对实际后端，不能静默回退后报告成功。
Hosted runner 的 GPU 能力需单独确认，必要时使用具备对应驱动的宿主。
默认 CI 显式使用 CPU 基线；手动触发的 `gpu-smoke` 输入另行要求三种 GPU API 的窗口和 resize。

## 社区对照

以下是当前默认分支的源码审查结果，不是本项目对这些仓库做过的构建或性能验证。
它们自己的 Kotlin/Compose/Skiko 版本不能替代本仓库的版本锁。

| 项目 | Windows | Linux | macOS | 对本项目的参考价值 |
| --- | --- | --- | --- | --- |
| bitsycore/compose-desktop-native | SDL + Skia OpenGL，可退 CPU；Windows Skiko/Skia DLL | 官方 Skiko + SDL OpenGL，可退 CPU | 官方 Skiko + Metal，可退 CPU | SDL 平台层、vendor、依赖桥接和消费体验 |
| brahmkshatriya/compose-native + skiko-native | SDL 提取 HWND → SkiaLayer；默认 D3D12，可退 OpenGL/CPU | SDL/SkiaLayer OpenGL，可退 CPU | Native fork 的 Metal，可选 OpenGL | Windows Native D3D12、SkiaLayer、Native 发布与平台打包 |

两者都不提供可以直接搬来的 Linux Vulkan 窗口完整实现，因此本项目新增该层。
本项目已锁定使用的 `brahmkshatriya/skiko-native` 差异继续通过 `scripts/skiko_adapt.py` 重现，
不会在普通构建时自动追踪其默认分支。当前静态 Skia/桥接进入 KLIB 的方案也不改为 Windows Skiko DLL。

源码依据：

- [Skia Ganesh 后端枚举](https://api.skia.org/GrTypes_8h.html)、[Graphite 后端枚举及 Direct3D 限制](https://api.skia.org/namespaceskgpu.html)。
- [Khronos 的交换链呈现 semaphore 复用规则](https://docs.vulkan.org/guide/latest/swapchain_semaphore_reuse.html)。
- [bitsycore GpuMode](https://github.com/bitsycore/compose-desktop-native/blob/main/compose/ui/ui/src/nativeMain/kotlin/com/compose/sdl/GpuMode.kt)、[其 Skiko/SDL 打包说明](https://github.com/bitsycore/compose-desktop-native/blob/main/README.md)。
- [brahmkshatriya Windows SDL HWND 接入](https://github.com/brahmkshatriya/compose-native/blob/main/compose/desktop/desktop-native/src/mingwX64Main/kotlin/androidx/compose/ui/window/NativeDesktopIntegration.windows.kt)、[Linux 平台接入](https://github.com/brahmkshatriya/compose-native/blob/main/compose/desktop/desktop-native/src/linuxMain/kotlin/androidx/compose/ui/window/NativeDesktopIntegration.linux.kt)。
- [已锁定的 Native Ganesh Context 桥接](https://github.com/brahmkshatriya/skiko-native/blob/85a7cf78045555f885fa774ac6007c6258d2bfb7/skiko/src/nativeJsMain/cpp/DirectContext.cc)、[Windows 默认 D3D12 与回退顺序](https://github.com/brahmkshatriya/skiko-native/blob/85a7cf78045555f885fa774ac6007c6258d2bfb7/skiko/src/windowsMain/kotlin/org/jetbrains/skiko/SkiaLayerProperties.windows.kt)。
