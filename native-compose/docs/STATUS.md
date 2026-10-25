# Skiko 当前状态与下一步

结论：**Core/Skottie 的主要实现已接入三个桌面 Native 目标，但最新 CI 三个平台均在 Kotlin 编译阶段失败，尚未完成移植验收。**
源码审计尚未发现新增的 target actual 或 bridge 符号遗漏；当前编译修复、三目标 Maven 汇总消费
和正式发布仍未验收，不能把当前状态宣布为三个平台均已完成。
不能将目标已经声明、Kotlin 已编译和宿主实际运行混为同一结果。

最新读取的 Native [run 40147244845](https://github.com/rachel-ylcs/ylcs-kmp/actions/runs/40147244845)
对应提交 b0e2bd0f8d1a。三个宿主的 Python 工具测试及桥插件集成检查已通过，
Core 均因 PaintFilterCanvas.shared.kt 的 @file:OptIn 直接引用内部注解 SymbolNameIsInternal 而编译失败。
当前修复在生成脚本中移除该源码引用，保留原有按名称配置的 source-set 编译器 opt-in；
Native 符号绑定、版本和三个 target 不变。版本目录及调用处同步使用 skiko-native / skiko-native-skottie。
按用户要求，本轮未执行准备、构建、测试或运行，修复等待下一轮 CI。

下表明确区分当前阻塞与此前通过的记录；此前成功不能作为最新提交的验收结果。

| 能力 | Windows x64 | Linux x64 | macOS ARM64 |
| --- | --- | --- | --- |
| Kotlin target / Native actual | 最新 CI 编译失败，修复待验证 | 同一编译错误，修复待验证 | 同一编译错误，修复待验证 |
| Skia/Core C++ 与静态归档 | 此前已编译、链接 | 此前 CI 已编译、链接并运行 | 此前 CI 已编译、链接并运行 |
| SDL 静态 SDK 与配套 CInterop KLIB | 此前已构建、嵌入并本地发布 | 此前 CI 已构建、嵌入并通过工件审计 | 此前 CI 已构建、嵌入并通过工件审计 |
| 窗口 GPU | D3D12 实际绘制/resize 通过 | Vulkan 实现已接入，待驱动运行 | Metal 实现已接入，待驱动运行 |
| Core/Skottie Native 回归 | 此前 6 项通过，本轮未到达 | 此前 CI 6 项通过，本轮未到达 | 此前 CI 6 项通过，本轮未到达 |
| 可搬移 ZIP | 此前 CI 通过，本轮未到达 | ZIP 权限修复待完整验收 | ZIP 权限修复待完整验收 |
| 独立 Maven 窗口消费 | 已运行；不需要 SDL SDK/平台源码 | 待 CI | 待 CI |

## 已完成的实现范围

- Core 与独立 Skottie 的三个桌面 Native 目标、静态 C++/Skia 归档接入及配套发布配置。
- 图形基线：光栅/回读、PNG/JPEG/WebP 编解码与无效输入、SVG、系统字体、双向段落。
- Skottie：真实 seek 后像素位置变化、绘制和无效 JSON。
- 公共 NativeWindow 宿主：窗口循环、resize、基本键盘、软件路径、D3D12/Vulkan/Metal adapters。
- SDL 静态实现、绑定和平台代码均在移植层；APP 与阶段样例使用公共 API。
- nativeMain/nativeTest 统一源集、根版本覆盖、源码 project 引用、普通 Maven 消费、ICU 暂存和 ZIP。
- Skiko 使用固定 vendor/skiko/workspace，原地更新生成文件；不再按输入指纹创建完整源码副本。
- 移植库、测试工具与维护/消费样例统一启用 -Xexpect-actual-classes；Kotlin 源码按仓库规范组织。
- 只有 macosArm64 一个 Apple 目标，Darwin 共享代码与层级已合并到 macosMain/macosTest。
- adaptation 的 5 个 C++ 文件统一为 4 空格缩进，不额外维护 .clang-format。
- 桥插件统一为一个构建模块，保留 Project/Settings 入口和轻量 Settings 产物；nativeBridgeReport 显示 target 规则及 adaptation 位置。

这些是当前功能回归覆盖，不代表 Skiko 的全部 API 都经过逐项运行测试。
Graphite 按路线排除，AWT/JVM runtime 不属于 Native 迁移缺口。

## 完整性审计

范围以锁定的 Skiko 0.156.0 为准：Core 和 Skottie，以及它们在三个桌面 Native 目标所需的
Kotlin/C++、静态归档、CInterop、运行数据和 publication。ci/docs/import-generator/test-utils
是上游构建或测试模块，不作为需要移植发布的 Native 运行时工件。

Windows 静态 bridge 符号审计：当前 commonMain/nativeMain/windowsMain 中 Core 的 976 个、
Skottie 的 31 个 ExternalSymbolName 均在编译的 bridge 归档中找到定义。
这能检查声明与 bridge 的连接，不能证明每个 API 的语义、资源寿命或其他平台行为均已通过测试。

源码还继承了官方已有的边界：

- Native 的 reachabilityBarrier 当前为空实现；GC、回调与资源寿命需要压力验证，不能仅由 smoke 推断安全性。
- Native 的 InteropPointer.fromInteropNativePointerArray 仍为 TODO；当前源码未发现其调用，需明确支持范围。
- 公共源码中的若干迭代器 remove() 仍为 TODO。这是上游共有限制，不应算作新增的桌面 Native 漏移植。

这些点已与官方锁定源码对照，本轮不为清除 TODO 数量而改写既有 API。
完整输入/IME、多窗口及窗口 API 统一继续放在后续 Compose 宿主阶段权衡；
它们不替代本阶段需要先完成的三宿主编译、运行与发布验收。

## 待完成

1. **三平台宿主验收**：先验证内部注解引用的编译修复，再完成 Native 回归、窗口、ZIP 搬移及 Maven 汇总消费。
   最新 Native run 为 [40147244845](https://github.com/rachel-ylcs/ylcs-kmp/actions/runs/40147244845)，
   提交 b0e2bd0f8d1a：三个平台因同一 Kotlin 编译错误失败，汇总及独立消费被跳过。
   ZIP 权限的桥插件检查已通过，但本轮没有到达真实应用 ZIP 的搬移验收。
2. **完整性与稳健性**：扩展 API 覆盖，验证长时间运行、快速 resize、最小化/恢复、DPI 变化、资源重复释放、
   设备丢失/恢复和显存/内存行为。当前 smoke 不能替代这些场景。
3. **完整窗口能力**：多窗口及其生命周期、鼠标/滚轮/焦点、文本输入与 IME、剪贴板、拖放和无障碍。
   当前 NativeWindow 是最小渲染宿主，不承诺完整桌面 UI 输入模型。
4. **跨宿主与发行**：Linux X11/Wayland、不同 Vulkan 驱动、Apple Metal、不同 Windows GPU；
   安装器、macOS app bundle/签名/公证等正式发行形态。
5. **公共 Maven**：按用户要求当前禁止远程发布，只生成本地验证仓库和 Actions 工件。
   上游 ComposeRepo 配置已从准备流程移除，移植库和桥插件发布任务只允许 file: 仓库。
   正式公开发布需后续单独安排。
6. **Compose 层**：先审计官方 runtime 等依赖三个目标的实际实现，再移植 UI、窗口与输入接入，
   推进 Rachel 的 Compose 模块和业务应用。已有 target 或 Maven 变体只证明声明存在；
   还需按 SOURCES.md 的审计规则排除 TODO、空实现及测试 stub，不能据此直接认定可复用。
   目前独立 APP 展示的是 Skiko 绘图，尚未运行 Compose UI。
