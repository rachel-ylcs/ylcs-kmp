# Skiko 当前状态与下一步

以当前源码和已执行验证为准。Windows 基线可用，三平台完整验收尚未完成；
不能将目标已经声明、Kotlin 已编译和宿主实际运行混为同一结果。

| 能力 | Windows x64 | Linux x64 | macOS ARM64 |
| --- | --- | --- | --- |
| Kotlin target / Native actual | 已实现并编译 | 已实现，Kotlin 交叉编译通过 | 实现已接入，待对应宿主完整验收 |
| Skia/Core C++ 与静态归档 | 已编译、链接 | 配置/实现已接入，待宿主 CI | 配置/实现已接入，待宿主 CI |
| SDL 静态 SDK 与配套 CInterop KLIB | 已构建、嵌入并本地发布 | 静态构建路径已接入，待宿主 CI | 静态构建路径已接入，待宿主 CI |
| 窗口 GPU | D3D12 实际绘制/resize 通过 | Vulkan 实现已接入，待驱动运行 | Metal 实现已接入，待驱动运行 |
| Core/Skottie Native 回归 | 6 项通过 | 待宿主执行 | 待宿主执行 |
| 独立 Maven 窗口消费 | 已运行；不需要 SDL SDK/平台源码 | 待 CI | 待 CI |

## 已完成的实现范围

- Core 与独立 Skottie 的三个桌面 Native 目标、静态 C++/Skia 归档接入及配套发布配置。
- 图形基线：光栅/回读、PNG/JPEG/WebP 编解码与无效输入、SVG、系统字体、双向段落。
- Skottie：真实 seek 后像素位置变化、绘制和无效 JSON。
- 公共 NativeWindow 宿主：窗口循环、resize、基本键盘、软件路径、D3D12/Vulkan/Metal adapters。
- SDL 静态实现、绑定和平台代码均在移植层；APP 与阶段样例使用公共 API。
- nativeMain/nativeTest 统一源集、根版本覆盖、源码 project 引用、普通 Maven 消费、ICU 暂存和 ZIP。
- 桥插件统一为一个构建模块，保留 Project/Settings 入口和轻量 Settings 产物；nativeBridgeReport 显示 target 规则及 adaptation 位置。

这些是当前功能回归覆盖，不代表 Skiko 的全部 API 都经过逐项运行测试。
Graphite 按路线排除，AWT/JVM runtime 不属于 Native 迁移缺口。

## 待完成

1. **三平台宿主验收**：重跑修复后的 Linux/macOS C++、最终链接、测试、窗口、静态 SDK、ZIP、Maven 消费。
   最新远程 run 仍是 40072504340 的旧提交，修复尚未推送；实际结果记录在 VALIDATION.md。
2. **完整性与稳健性**：扩展 API 覆盖，验证长时间运行、快速 resize、最小化/恢复、DPI 变化、资源重复释放、
   设备丢失/恢复和显存/内存行为。当前 smoke 不能替代这些场景。
3. **完整窗口能力**：多窗口及其生命周期、鼠标/滚轮/焦点、文本输入与 IME、剪贴板、拖放和无障碍。
   当前 NativeWindow 是最小渲染宿主，不承诺完整桌面 UI 输入模型。
4. **跨宿主与发行**：Linux X11/Wayland、不同 Vulkan 驱动、Apple Metal、不同 Windows GPU；
   安装器、macOS app bundle/签名/公证等正式发行形态。
5. **公共 Maven**：三目标 publication 汇总与独立消费全部通过后，完成正式仓库发布流程。
   目前只发布到本地验证仓库/配置 CI 工件。
6. **Compose 层**：随后移植 runtime、UI、窗口与输入接入，再推进 Rachel 的 Compose 模块和业务应用。
   目前独立 APP 展示的是 Skiko 绘图，尚未运行 Compose UI。
