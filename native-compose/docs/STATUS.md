# Native Compose 当前状态与下一步

更新时间：2026-10-25（Asia/Shanghai）。

当前已在八个 Compose UI 工件与 SDL ComposeScene 宿主上，继续接入 foundation-layout、foundation、animation-core、animation、animation-graphics 五个运行时工件。
预览已使用真实 Foundation 文本编辑、选择菜单、布局、列表及 Animation；应用只声明 libs.compose.foundation。
本轮按用户要求只做构建，不执行测试、运行或 CI，也不扩展上一阶段的 CI 测试契约。
当前实现、任务和支持边界见 [FOUNDATION_ANIMATION.md](FOUNDATION_ANIMATION.md)，UI 阶段见 [COMPOSE_UI.md](COMPOSE_UI.md)；实际验证见 [VALIDATION.md](VALIDATION.md)。

## 当前已接入的能力

- 保留锁定上游、单份 vendor、adaptation 维护源码和自动遮蔽流程。
- 三个桌面 Native 目标的十三模块 KLIB/source/publication 配置及按库、目标、metadata 的桥规则，保留官方模块边界与 KLIB 身份。
- SDL 键盘/鼠标/滚轮/焦点/文本/IME/接收 drop，系统文本剪贴板和 URI。
- ComposeScene 布局、绘制、重组、帧时钟、焦点、Dialog 返回及 Lifecycle/SavedState/ViewModelStore 所有权。
- 真实 Composable 预览仅在 ylcs-app/native-compose；适配库保留单元测试。
- 上一 UI 阶段的库内测试报告契约 72 项、独立 Maven 消费者 6 项保持原范围；本轮没有为新增五模块运行测试或发布消费验证。
- 本地 file: Maven 验证仓库；公开 Maven 发布仍关闭。

这些描述实现及检查范围，不表示每项 API、三平台或全部设备均已验收。
本轮本机只具备 Windows 宿主；Foundation/Animation 的 Linux/macOS 编译、链接及三平台行为交由用户后续验证。

## Skiko 基线

Guide3 记录用户已报告此前 Core/Skottie 三平台 CI 通过；旧文档中 run 40147244845 的失败是历史快照。
本次主 UI 又扩展窗口/输入 API，因此以前的成功不能替代这次 CI。
图形路线仍是静态 Skia/SDL：Windows Ganesh/D3D12、Linux Ganesh/Vulkan、macOS Ganesh/Metal，CPU 路径单独验证。

保留上游边界：reachabilityBarrier 的 GC/回调寿命需压力测试，InteropPointer 数组转换及若干迭代器 remove 的上游 TODO 需按调用路径评估。
这些限制不能机械计为主 UI 漏移植，也不能由代表性回归推断全部安全。

## 下一步

1. 用户继续上一 UI 阶段 CI；本轮没有修改 workflow 或 ciModules。Foundation/Animation 的扩展检查由用户安排。
2. 验证新增五模块在对应宿主的编译、发布和独立消费，再用最新预览验收文本编辑/IME、菜单、列表滚动、动画及已有窗口行为。
3. 基础层验收后，接入 Rachel Compose 模块与业务 APP。
4. 多窗口、完整无障碍、本机控件、拖出与正式发行按阶段另行实现。
5. 提交、推送和公开 Maven 发布均等待用户后续安排。
