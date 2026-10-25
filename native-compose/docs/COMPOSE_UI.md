# Compose UI 第一阶段

当前选择根版本目录中的 CMP 1.13.0-beta01、Kotlin 2.5.0-Beta2、JDK 25 和根 Gradle Wrapper。
官方源码固定在 JetBrains/compose-multiplatform-core 的 v1.13.0-beta01，
commit bb05011eb10784ada1ec00494f1a710ba6ab6252。移植工件使用相同 CMP 版本，group 为 love.yinlin.native.compose.ui。

本阶段只建立 ui-util → ui-geometry → ui-unit 的依赖链。ui 主工件、graphics/text/ui-skiko、
backhandler、Foundation、ComposeScene 窗口宿主尚未移植，不能用整包 libs.compose.ui 替代这三个细分依赖。

## 实际实现审计

| 模块/依赖 | 实际源码与边界 | 当前处理 |
| --- | --- | --- |
| ui-util | commonMain 的位打包、数学和集合工具；nonJvmMain 的真实位转换/舍入 actual | 复用上游公共及非 JVM 源码；不加载 commonStubsMain、linuxx64StubsMain |
| ui-util tracing | 官方 macosMain 的 trace 执行 block，traceValue 不采集事件 | Windows/Linux actual 保持同一 profiling-disabled 语义；测试返回值、次数和异常传播；没有宣称 ETW/perf 功能 |
| ui-geometry | Offset、Rect、Size、CornerRadius 等都是公共实现，无缺失 actual | 复用 commonMain，添加几何边界回归，不伪造平台实现 |
| ui-unit | FontScaling 使用真实 FontScalingLinear；非 JVM 的 JvmDefaultWithCompatibility 是注解兼容 typealias | 复用 nonAndroidMain/nonJvmMain 到 nativeMain；NoOp 仅对应 JVM 专属注解，不是字体缩放空实现 |
| 官方 Runtime | 已核对 Native ThreadLocal/atomic 线程标识、atomicfu 锁和 Kotlin/Native WeakReference 实现 | 复用官方发布工件；增加 Snapshot 隔离及 Composition/effect/disposal 回归，尚待运行验证 |
| annotation-internal / collection-internal | KLIB 名称解析兼容占位，实际 API 传递依赖 AndroidX | 使用根 cmp-internal，当前 1.12.0-alpha02；占位源码 EmptyFile.kt 是上游明确用途 |

Runtime 的核对是关键实际实现审计，不表示其所有 API、线程和 GC 压力场景均已验收。
internal 1.12.0-alpha02 的三个目标发布变体已核实，Windows 源码包及 KLIB manifest 也已检查：
唯一名称延续原兼容库，compiler_version 为 2.3.20，依赖实际 AndroidX annotation/collection。
项目直接声明的 AndroidX 版本仍由根目录选定；本轮不把上游依赖的最低版本当作必须降级的要求。

官方构建语义按各模块 build-fork.gradle 核对；实际 Native 构建使用维护脚本，避免载入整个 AndroidX 构建工具链。
原始源码和构建脚本保留在同一 sparse checkout，修改的构建规则、actual 和回归测试提交到 adaptation/scripts/build-logic。
上游测试源码仍保留在 checkout，本阶段执行维护的库内回归，尚未接入需要 Kruth 等额外组件的上游完整测试集。

## 源码与 IDE

```text
adaptation/org.jetbrains.compose.ui_ui-util/
adaptation/org.jetbrains.compose.ui_ui-geometry/
adaptation/org.jetbrains.compose.ui_ui-unit/
vendor/compose/workspace/                  # 唯一稀疏 Git checkout
vendor/compose/workspace/native-desktop/   # 生成的 Native Gradle 入口
vendor/build/compose-ui/                  # 输出、本地验证 Maven 文件和测试消费者
ylcs-app/native-compose/                  # 唯一窗口预览
```

native.compose 默认 false，普通业务/消费者不下载源码、不关联 adaptation，也不准备 SDK。
维护模式 true 自动准备 Skiko 和 Compose 两个固定工作区，并关联名为 skiko / compose-ui 的 included builds。
Compose 的三个项目使用 adaptation 的 build.gradle.kts；sourceSets 显式关联上游真实源码与维护源码。
更新后在 IDE Reload All Gradle Projects；本轮没有执行 Gradle 同步或构建，新增源集的 IDE 导入结果尚未验证。

sync_compose.py 只稀疏获取所选 UI 模块，锁定 commit，检查根 CMP 与源码版本一致。
不创建指纹源码副本；重复准备更新同一固定入口，保留构建缓存。遇到 tracked 本地编辑或未知目录则停止，
不会通过 reset/force 覆盖。原始源码不手改版本，维护构建直接读取根 catalog，所有 Native 编译启用 -Xexpect-actual-classes。

## 桥接与 common 依赖

业务代码只声明 libs.compose.ui.util / geometry / unit，APP 不配置任务、CInterop 或各平台源码。
Windows/Linux 的官方请求重定向到这三个移植工件，维护模式使用 included project，普通模式使用 Maven。
macOS 的具体目标编译保留官方依赖；本地源码构建仍编译 macosArm64，作为官方真实实现的对照和发布基线。

nativeMain 跨三个目标时，桥插件使用能覆盖三个目标的移植共享元数据：metadataTargets 描述该覆盖范围，
targets 描述实际目标工件的重定向。默认 metadataTargets 跟随 targets，既有自定义规则保持原行为。
仅含 Native 的公共图可选这份元数据；混合 JVM/Android 的 common 图继续使用官方元数据。
独立消费测试会编译该三目标公共源集，覆盖共享元数据及叶子目标选择；结果等待 CI。

底部别名为 native-skiko / native-skiko-skottie / native-compose-ui-util / geometry / unit；
版本始终来自 skiko / cmp / cmp-internal。不要为 Native 另设版本或附加 native 版本后缀。

## 当前任务与 CI

可从主工程指定这些配置的任务，本轮没有执行：

```powershell
.\gradlew.bat "-Pnative.compose=true" :compose-ui:ui-util:windowsTest :compose-ui:ui-geometry:windowsTest :compose-ui:ui-unit:windowsTest
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:nativeBridgeReport
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:runWindowsDebugExecutable
.\gradlew.bat "-Pnative.compose=true" :native-compose:publishComposeUiToVerificationRepository
```

Linux/macOS 在对应宿主使用 linuxTest / macosTest，窗口命令使用 runLinuxDebugExecutable / runMacosDebugExecutable。
库内测试为 Util 5 项、Runtime 2 项、Geometry 3 项、Unit 4 项，共 14 项；验证器拒绝缺失、失败或跳过的报告。
Skiko 的原 6 项测试留在 Core/Skottie 各自 src/nativeTest，通过 mingwX64RegressionTest 等专用 test run 调用。

现有 .github/workflows/native-core.yml 保留为唯一 Native 工作流，显示名改为 Native Compose UI：

1. 三宿主只准备 Compose，执行源码/发布工具测试、桥插件集成测试、14 项 UI/Runtime 回归并生成宿主 Maven 验证文件。
2. 合并三个目标工件，选择一份 KMP root，核对校验和、KLIB target/编译器、源码来源与 license、Unit 导出的 Geometry 依赖。
3. 全新消费者分别用官方 common 请求和显式 Native 别名运行 Maven 测试，确认不准备 vendor/SDK；macOS 也验证官方消费路径。

所有 Maven 发布任务仅允许 file: 仓库，Actions 附件不是公共 Maven 发布。当前 CI 不构建 Skiko、不启动窗口、
不执行 GPU 或 APP 打包；这些能力按后续 UI 渲染/宿主阶段更新同一工作流。
