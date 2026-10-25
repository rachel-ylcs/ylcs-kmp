# Native Compose 当前状态与下一步

更新时间：2026-10-25（Asia/Shanghai）。

当前进入 Compose UI 主工件与 SDL ComposeScene 宿主阶段，覆盖 ui-util、ui-geometry、ui-unit、ui-graphics、ui-text、ui-skiko、ui-backhandler、ui。
此前 Guide3 中的主 UI WIP 已继续实现，包含真实 Composable APP、库内测试、独立 Maven 消费者及同一份三宿主 CI 配置。
具体源码分层、任务、所有权和支持边界见 [COMPOSE_UI.md](COMPOSE_UI.md)；实际验证结果见 [VALIDATION.md](VALIDATION.md)。

## 当前已接入的能力

- 保留锁定上游、单份 vendor、adaptation 维护源码和自动遮蔽流程。
- 三个桌面 Native 目标的八模块 KLIB/source/publication 配置及按库、目标、metadata 的桥规则。
- SDL 键盘/鼠标/滚轮/焦点/文本/IME/接收 drop，系统文本剪贴板和 URI。
- ComposeScene 布局、绘制、重组、帧时钟、焦点、Dialog 返回及 Lifecycle/SavedState/ViewModelStore 所有权。
- 真实 Composable 预览仅在 ylcs-app/native-compose；适配库保留单元测试。
- 库内测试报告契约 72 项，独立 Maven 消费者 6 项；CI 汇总三宿主工件并验证独立消费。
- 本地 file: Maven 验证仓库；公开 Maven 发布仍关闭。

这些描述实现及检查范围，不表示每项 API、三平台或全部设备均已验收。
本轮本机只具备 Windows 宿主；Linux/macOS 编译、链接和运行交由用户接下来的 CI 流程。

## Skiko 基线

Guide3 记录用户已报告此前 Core/Skottie 三平台 CI 通过；旧文档中 run 40147244845 的失败是历史快照。
本次主 UI 又扩展窗口/输入 API，因此以前的成功不能替代这次 CI。
图形路线仍是静态 Skia/SDL：Windows Ganesh/D3D12、Linux Ganesh/Vulkan、macOS Ganesh/Metal，CPU 路径单独验证。

保留上游边界：reachabilityBarrier 的 GC/回调寿命需压力测试，InteropPointer 数组转换及若干迭代器 remove 的上游 TODO 需按调用路径评估。
这些限制不能机械计为主 UI 漏移植，也不能由代表性回归推断全部安全。

## 下一步

1. 用户传递并执行更新后的 native-core.yml，核对三个宿主构建、72 项库测试、APP 链接及 CPU smoke、Maven 汇总和两种消费者请求。
2. 本机与真实设备验收中文 IME、缩放/多显示器、GPU、剪贴板和长时间关闭/重开。
3. 再推进 Foundation/Animation 等实际依赖闭包，随后接入 Rachel Compose 模块与业务 APP。
4. 多窗口、完整无障碍、本机控件、拖出与正式发行按阶段另行实现。
5. 提交、推送和公开 Maven 发布均等待用户后续安排。
