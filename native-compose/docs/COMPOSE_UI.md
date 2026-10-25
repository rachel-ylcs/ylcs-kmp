# Compose UI 与 SDL 场景宿主

本阶段固定 CMP 1.13.0-beta01、Kotlin 2.5.0-Beta2、Skiko 0.156.0、SDL 3.4.18、JDK 25 和根 Gradle Wrapper。
Compose 来源为 JetBrains/compose-multiplatform-core v1.13.0-beta01，commit bb05011eb10784ada1ec00494f1a710ba6ab6252。
Maven group 为 love.yinlin.native.compose.ui；KLIB unique_name 保持 org.jetbrains.compose.ui:<artifact>，版本与官方一致。

## 本阶段范围

| 模块 | 实现与验证范围 |
| --- | --- |
| ui-util / ui-geometry / ui-unit | 工具、几何、密度、单位、Native actual 和官方 Runtime 行为 |
| ui-graphics / ui-text / ui-skiko | 图形与文本 API、Skia backend、字体、区域查询、像素与颜色矩阵 |
| ui-backhandler | 复用上游有效 jbMain 实现，桥接 Google NavigationEvent；验证 handler 注册、禁用与释放 |
| ui | 复用 commonMain、skikoMain、nonJvmMain/nativeMain，补齐 Native 键盘、光标、剪贴板、URI、IME 和同步 actual |
| SDL ComposeScene 宿主 | 真实 Layout/绘制/重组、帧时钟、鼠标与滚轮、键盘焦点、Dialog、生命周期和 ViewModelStore 清理 |

UI 阶段先以 UI 原语独立完成构建与消费验证；后续 Foundation/Animation 已单独接入，最新预览和本轮构建范围见 [FOUNDATION_ANIMATION.md](FOUNDATION_ANIMATION.md)。
官方 Runtime、Google annotation/collection、Lifecycle、SavedState 和 NavigationEvent 继续复用真实 Native 工件。
Navigation3 UI 保留 JetBrains 坐标，当前没有纳入本轮移植。

## 源码与桥接

- adaptation/org.jetbrains.compose.ui_<artifact>/ 保存提交的构建脚本、自有实现与单元测试。
- vendor/compose/workspace/ 为唯一锁定稀疏 checkout；不手工修改。
- vendor/compose/workspace/native-desktop/ 为生成的 Compose Gradle included build；当前已从八个 UI 模块扩展至十三模块。
- scripts/compose_adapt.py 记录可重现的源码改动并检测漂移；compose-origins.json 记录派生来源。
- 不编译 StubsMain；替代 actual 对应的原文件从准备工作区移除，让 Gradle 和 IDEA 看到同一份定义。
- skikoMain 位于 commonMain 与 nativeMain 之间，保留 expect/actual fragment 边界。

native.compose=true 时通过 Settings/composite 直接消费源码项目；false 时不准备 vendor，使用 Maven 工件。
桥插件精确重定向八个 UI 工件在 mingw_x64、linux_x64、macos_arm64 的依赖，并处理对应共享 metadata。
其他平台保持官方依赖。Maven 发布保留可消费的 Native 坐标，不输出生产者源码路径。

## 窗口、调度与资源所有权

commonMain 可通过 androidx.compose.ui.window.NativeWindow(options) { ... } 打开一个真实 Composable 窗口。
这是此移植库提供的 Native-only 桌面入口；混合 JVM/Android 工程应把入口放到相应 Native 源集。
调用阻塞到窗口关闭；onCloseRequest 返回 true 才接受 OS 关闭。Escape 交给 Compose 焦点/弹层/返回处理。

宿主复用 CanvasLayersComposeScene、FrameRecomposer 和 SingleComposeSceneRenderingScope。
宿主在首帧前读取实际像素尺寸，并在派发输入前同步尺寸，避免启动事件触发无限约束测量。
窗口线程拥有 scene、composition、Lifecycle/SavedState/ViewModelStore 与 UI 队列；后台计时只入队和唤醒 SDL。
关闭先释放 composition/effects 和 recomposer，再停止输入、销毁生命周期/清空 ViewModelStore，最后释放调度器与原生窗口资源。
重复关闭不重复释放；构造及释放失败会继续执行其余清理并保留异常。

org.jetbrains.skiko.window.NativeWindow.open 暴露拥有自身数据的键盘、指针、文本/IME、drop 与焦点事件。
原 run 入口继续委托 open，保持既有 Skia 消费者兼容。借用 canvas 和窗口 scope 不得在调用结束后使用。
SDL 文本/drop 指针被复制为 Kotlin 数据；剪贴板分配缓冲按 SDL 所有权释放。
Compose 使用物理像素，displayScale 与 pixelDensity 分开转换；IME codepoint 偏移转成 UTF-16。

## 预览与验收

ylcs-app/native-compose 是唯一可视预览，安装 composeCompiler，保持 Rachel install/rachel/lib DSL 和 commonMain 官方依赖。
本页记录的 UI 阶段预览使用 UI 原语；最新预览已换为 Foundation BasicTextField、选择容器、列表和 Animation，仍保留同一公共窗口入口。
移植层不维护 samples，不在 APP 复制 SDL SDK、cinterop 或平台窗口代码。

```powershell
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:linkDebugExecutableWindows
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:runWindowsDebugExecutable
.\gradlew.bat "-Pnative.compose=true" :compose-ui:ui:windowsTest :compose-ui:ui-backhandler:windowsTest
.\gradlew.bat "-Pnative.compose=true" :native-compose:publishComposeUiToVerificationRepository
```

Linux/macOS 使用各自宿主与 Linux/linuxTest、Macos/macosTest。
库内报告契约共 72 项：原有基础 46 项，加 NativeInputTest 10、NativeDispatcherTest 5、NativeSceneTest 9、NativeBackHandlerTest 2。
报告验证器拒绝缺失、失败、跳过或重复报告；独立 Maven 消费者另有 6 项，包含真实 ComposeScene 重组和像素回读。

唯一工作流 .github/workflows/native-core.yml 同步执行三宿主库内测试、APP 编译链接及 dummy/software smoke、本地 Maven 发布/汇总和独立消费者。
消费者只直接声明主 ui，分别测试 commonMain 官方请求与显式 Native 坐标，并审计传递的八模块闭包；CI 不向公开 Maven 发布。
本轮实际执行的命令、结果和未验证范围见 VALIDATION.md。Windows 通过不能推断 Linux/macOS 通过。

## 保留的边界

- SDL 接收文件/文本 drop；完整 outgoing drag、跨进程格式协商尚未实现。
- InteropView 只提供当前 UI backend 的类型契约；没有宣称系统控件嵌入。
- 单窗口入口；多窗口调度、完整 OS 无障碍、系统菜单/托盘及正式安装发行属于后续阶段。
- IME 适配有 codepoint、候选区域与取消回归；中文候选窗、多显示器缩放、平台输入法仍须真机交互验收。
- CPU/离屏测试不能替代 D3D12/Vulkan/Metal 的设备、驱动和长期资源压力验证。
