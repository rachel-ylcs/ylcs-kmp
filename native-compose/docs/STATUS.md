# Skiko 当前状态与下一步

结论：**Core/Skottie 已接齐本阶段约定的三个桌面 Native 目标，当前处于三平台验收与发行收尾阶段。**
源码审计尚未发现新增的 target actual 或 bridge 符号遗漏；Linux/macOS 的完整运行、三目标 Maven 汇总消费
和正式发布尚未完成，因此不能把当前状态宣布为三个平台均已验收完成。
不能将目标已经声明、Kotlin 已编译和宿主实际运行混为同一结果。

本轮将源码准备改为唯一固定 workspace，renderer 改用 val 并保留构造失败清理，
统一编译参数和 adaptation Kotlin 规范。按用户要求未执行准备、构建、测试或运行；
下表为本轮调整前的验证记录，新流程与调整后的代码等待 CI 验收。
最新读取的 Native run 40111028367（提交 c5d1df6ebed9）中，三个宿主的编译、链接、
6 项 Native 回归、软件窗口和 Maven 工件审计均已通过；Linux/macOS 均在 ZIP 权限验收失败。
当前补齐 ZIP 权限、Kotlin 警告修复和禁止远程 Maven 发布，尚未运行修改后的验证。

| 能力 | Windows x64 | Linux x64 | macOS ARM64 |
| --- | --- | --- | --- |
| Kotlin target / Native actual | 已实现并编译 | 最新 CI 已编译 Core/Skottie/APP | 最新 CI 已编译 Core/Skottie/APP |
| Skia/Core C++ 与静态归档 | 已编译、链接 | 最新 CI 已编译、链接并运行 | 最新 CI 已编译、链接并运行 |
| SDL 静态 SDK 与配套 CInterop KLIB | 已构建、嵌入并本地发布 | 最新 CI 已构建、嵌入并通过工件审计 | 最新 CI 已构建、嵌入并通过工件审计 |
| 窗口 GPU | D3D12 实际绘制/resize 通过 | Vulkan 实现已接入，待驱动运行 | Metal 实现已接入，待驱动运行 |
| Core/Skottie Native 回归 | 6 项通过 | 最新 CI 6 项通过 | 最新 CI 6 项通过 |
| 可搬移 ZIP | 最新 CI 通过 | 打包丢失执行权限，修复待 CI | 打包丢失执行权限，修复待 CI |
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

1. **三平台宿主验收**：重跑 ZIP 权限修复及当前源码调整后的工作流，再完成 Maven 汇总与独立消费。
   最新 Native run 为 [40111028367](https://github.com/rachel-ylcs/ylcs-kmp/actions/runs/40111028367)，
   提交 c5d1df6ebed9：Windows 成功；Linux/macOS 的编译、链接、6 项测试和本地 Maven 工件审计成功，
   仅在 ZIP 搬移步骤因执行权限丢失失败。当前修复已在工作区，尚无修复后的 CI 结果。
2. **完整性与稳健性**：扩展 API 覆盖，验证长时间运行、快速 resize、最小化/恢复、DPI 变化、资源重复释放、
   设备丢失/恢复和显存/内存行为。当前 smoke 不能替代这些场景。
3. **完整窗口能力**：多窗口及其生命周期、鼠标/滚轮/焦点、文本输入与 IME、剪贴板、拖放和无障碍。
   当前 NativeWindow 是最小渲染宿主，不承诺完整桌面 UI 输入模型。
4. **跨宿主与发行**：Linux X11/Wayland、不同 Vulkan 驱动、Apple Metal、不同 Windows GPU；
   安装器、macOS app bundle/签名/公证等正式发行形态。
5. **公共 Maven**：按用户要求当前禁止远程发布，只生成本地验证仓库和 Actions 工件。
   上游 ComposeRepo 配置已从准备流程移除，移植库和桥插件发布任务只允许 file: 仓库。
   正式公开发布需后续单独安排。
6. **Compose 层**：随后移植 runtime、UI、窗口与输入接入，再推进 Rachel 的 Compose 模块和业务应用。
   目前独立 APP 展示的是 Skiko 绘图，尚未运行 Compose UI。
