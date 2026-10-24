# Core 渲染后端与社区实现

审查日期：2026-10-24。项目版本仍以根 `libs.versions.toml` 为准；本阶段为 Skiko 0.156.0。
本文区分 API 存在、C++/KLIB 构建、离屏运行以及 GPU 窗口运行，不把其中一项替代另一项。

## 本项目路线

| 层次 | Windows x64 | Linux x64 | macOS ARM64 |
| --- | --- | --- | --- |
| Kotlin 目标 | `mingwX64` | `linuxX64` | `macosArm64` |
| 窗口、事件 | SDL3 | SDL3 | SDL3 |
| 后续首选 Skia GPU 后端 | Ganesh / D3D12 | Ganesh / Vulkan | Ganesh / Metal |
| 当前自动化窗口基线 | Skia CPU 光栅 → SDL texture | 同左 | 同左 |

Vulkan 是 Khronos 的跨平台 GPU API，和 Microsoft D3D12、Apple Metal 属于同一层。
Ganesh、Graphite 是 Skia 的两套 GPU 渲染架构；Skiko 在 Kotlin 层封装其接口。
SDL3 提供窗口及事件，不能因为 SDL renderer 使用某个 GPU API，就认为 Skia 已经使用同一个 GPU 后端。

Core/Ganesh 已有 Direct3D、Vulkan、Metal 的 Native 桥接入口。当前社区基线的
`DirectContext.cc` 包含真正的 `GrDirectContexts::MakeD3D/MakeVulkan/MakeMetal` 调用，
并非 Graphite 的 Vulkan 占位入口；但 API 存在仍需要对应设备、队列、交换链和同步的运行验证。
Windows 的 `DIRECT3D` 枚举在当前实现中指 D3D12。

上轮 Graphite 审计中的 Windows/Linux Vulkan 建议只适用于该扩展，不能据此要求 Core/Windows 改用 Vulkan。
Graphite 当前直接支持 Metal/Vulkan 等后端，没有可直接复用的原生 D3D12 后端。
本项目无需以 Graphite 为前提才能实现 Windows D3D12；Graphite 的后续移植单独评估。
Dawn 的其他后端也不等于现有 Skiko Native 绑定已经提供完整路径。

Core 基线完成后，GPU 窗口阶段仍需：

- Windows：SDL HWND 与现有 Ganesh D3D12 renderer 对接，验证资源重建、缩放、透明及同步。
- Linux：虽然已有 Vulkan Context/RenderTarget 桥接，当前 SkiaLayer/社区 SDL 窗口主要为 OpenGL，
  仍需 Vulkan instance/device、surface、swapchain、队列、同步、resize 和销毁路径。
- macOS：复用 Metal Context/renderer 基础，对接 SDL Metal view/CAMetalLayer 并验证绘制和生命周期。

CPU 基线和硬件后端测试分别报告结果；显式要求某个 GPU 后端的测试应核对实际后端，不能静默回退后报告成功。
Hosted runner 的 GPU 能力需单独确认，必要时使用具备对应驱动的宿主。

## 社区对照

以下是当前默认分支的源码审查结果，不是本项目对这些仓库做过的构建或性能验证。
它们自己的 Kotlin/Compose/Skiko 版本不能替代本仓库的版本锁。

| 项目 | Windows | Linux | macOS | 对本项目的参考价值 |
| --- | --- | --- | --- | --- |
| bitsycore/compose-desktop-native | SDL + Skia OpenGL，可退 CPU；Windows Skiko/Skia DLL | 官方 Skiko + SDL OpenGL，可退 CPU | 官方 Skiko + Metal，可退 CPU | SDL 平台层、vendor、依赖桥接和消费体验 |
| brahmkshatriya/compose-native + skiko-native | SDL 提取 HWND → SkiaLayer；默认 D3D12，可退 OpenGL/CPU | SDL/SkiaLayer OpenGL，可退 CPU | Native fork 的 Metal，可选 OpenGL | Windows Native D3D12、SkiaLayer、Native 发布与平台打包 |

两者都不提供可以直接搬来的 Linux Vulkan 窗口完整实现。
本项目已锁定使用的 `brahmkshatriya/skiko-native` 差异继续通过 `skiko/adapt.py` 重现，
不会在普通构建时自动追踪其默认分支。当前静态 Skia/桥接进入 KLIB 的方案也不改为 Windows Skiko DLL。

源码依据：

- [Skia Ganesh 后端枚举](https://api.skia.org/GrTypes_8h.html)、[Graphite 后端枚举及 Direct3D 限制](https://api.skia.org/namespaceskgpu.html)。
- [bitsycore GpuMode](https://github.com/bitsycore/compose-desktop-native/blob/main/compose/ui/ui/src/nativeMain/kotlin/com/compose/sdl/GpuMode.kt)、[其 Skiko/SDL 打包说明](https://github.com/bitsycore/compose-desktop-native/blob/main/README.md)。
- [brahmkshatriya Windows SDL HWND 接入](https://github.com/brahmkshatriya/compose-native/blob/main/compose/desktop/desktop-native/src/mingwX64Main/kotlin/androidx/compose/ui/window/NativeDesktopIntegration.windows.kt)、[Linux 平台接入](https://github.com/brahmkshatriya/compose-native/blob/main/compose/desktop/desktop-native/src/linuxMain/kotlin/androidx/compose/ui/window/NativeDesktopIntegration.linux.kt)。
- [已锁定的 Native Ganesh Context 桥接](https://github.com/brahmkshatriya/skiko-native/blob/85a7cf78045555f885fa774ac6007c6258d2bfb7/skiko/src/nativeJsMain/cpp/DirectContext.cc)、[Windows 默认 D3D12 与回退顺序](https://github.com/brahmkshatriya/skiko-native/blob/85a7cf78045555f885fa774ac6007c6258d2bfb7/skiko/src/windowsMain/kotlin/org/jetbrains/skiko/SkiaLayerProperties.windows.kt)。
