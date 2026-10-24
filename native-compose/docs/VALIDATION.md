# 当前 Skiko 阶段的验证

日期：2026-10-24（Asia/Shanghai）。环境：Windows x64、JDK 25、根 Wrapper Gradle 9.8.0、
Kotlin 2.5.0-Beta2、Skiko 0.156.0、Skia m156-24300fbbcb、MSVC 14.50.35717、SDL 3.4.18。
Gradle 经 IDEA MCP 终端执行；日志统一位于 Git 忽略的 native-compose/vendor/build/logs/。
本页只记录当前阶段及仍有意义的回归，不累积已替换的临时样例。

## 当前实际通过

| 验证 | 结果 / 日志 |
| --- | --- |
| 静态 SDL 生产者 | 从锁定源码构建 SDL3-static.lib；归档嵌入 Core 的 cinterop-sdl；绑定和系统链接依赖随 publication 导出 |
| Core / Skottie 回归 | Core 4 项、Skottie 2 项 Native 测试，零失败/错误/跳过；覆盖光栅、图片编解码及无效输入、SVG、字体/双向段落、实际时间轴像素变化和无效 JSON |
| 独立 Rachel APP | 只有 commonMain 业务源码；commonMain 声明官方 Skiko/Skottie；不引用 samples，无 SDL SDK/CInterop/平台 adapter 配置 |
| Windows 发布与运行 | merged-native-final.log：1m17s、123 tasks；6 项 Native 测试、阶段 Debug ZIP、APP Release D3D12/resize/退出、Core/Skottie 发布和 dependencyInsight 通过 |
| 实际 GPU | Skia Ganesh/Direct3D12，NVIDIA RTX 4060；APP resize 1000×660→1160×720，绘制 12 帧 |
| 桥插件合并 | 同一 native-bridge 模块包含 Project/Settings 入口；16 项 TestKit、validatePlugins 和两个 marker 的发布通过。bridge-merged-report.log |
| 合并后的最终复核 | merged-bridge-debug-final.log：57s、67 tasks；再次通过 16 项 TestKit 与插件校验/发布，APP Debug ZIP 也更新为仅 EXE 和 ICU |
| 独立 Settings 消费 | 外部最小工程仅从验证 Maven 安装 Settings 插件；断言加载 settings classifier，且其类加载器不能加载 KGP；实际配置通过。merged-settings-published.log |
| 重定向可见性 | nativeBridgeReport 输出 target、configuration、上游坐标、included project 和 adaptation 路径；dependencyInsight 实际选择 skiko / skiko-skottie 源码项目 |
| Maven 审计 | Core 27 个静态归档、Skottie 4 个，另有 SDL 静态归档的配套 CInterop；校验 metadata、target、Kotlin 版本、传递 Core、源码来源及许可、ICU 和无生产者绝对链接路径 |
| 独立 Maven 窗口消费 | merged-maven-final.log：32s、6 tasks；只声明 Skottie，再加旧官方 Core 0.150.1 请求，使用当前移植版本；PNG 像素/编解码及公共窗口 CPU resize 通过 |
| 可搬移 ZIP | 阶段 Debug 与 APP Release 解压到新的带空格目录；软件和 D3D12 运行、缩放、退出通过，PE imports 无动态 SDL。merged-relocated-software.log / merged-relocated-gpu.log |
| 运行文件 | APP Release ZIP 只有 native_compose.exe 和 icudtl.dat；不复制许可证，不需要 SDL3.dll/skiko.dll/JVM；源码和 sources 工件保留许可证 |
| 禁用 target 警告 | 根 ignoreDisabledTargets 属性自动传给生成的 included build，独立消费者也设置；merged-native-final.log / merged-maven-final.log 无 Native task ... is disabled 警告 |
| CInterop 宿主提示 | cinterop-warning-and-skottie.log：1m42s、56 tasks；定向抑制 CrossCompilationWithCinterops 后，该提示未再出现；报告仍列出三个 target，Core 4 项/Skottie 2 项 Native 回归通过 |
| 源码与工作流工具 | Python 14 项测试和 actionlint 通过 |

APP Debug EXE 为 24,276,480 bytes，Release 为 20,896,256 bytes；这是磁盘文件大小，不是内存或性能基准。
维护模式直接依赖源码项目；上述文件 Maven 仓库专用于验证 publication/独立消费，
不要求 ~/.m2 或 mavenLocal()，最终程序运行也不访问 Maven。

## IDE 与普通工程边界

IDE 通过 included build 关联 adaptation 和准备后的上游源码。
准备规则变更后应 Reload All Gradle Projects，更新带指纹的 included build 路径。
最近一次通过 IDEA MCP 对独立 APP 的 Main.kt、ShowcaseScene.kt，以及 Core 的公共 NativeWindow.kt、
NativeWindow.native.kt 批量检查，返回零错误。APP 不再有自己的 SDL CInterop，绑定由 Core 提供。
正式维护脚本仍直接使用 adaptation/build.gradle.kts，生成的上游基础脚本与 buildSrc 保留在 vendor。

MCP 当前拒绝整文件分析 Gradle 脚本；根工程普通脚本也同样受限，不能把“未分析”当成通过。
实际脚本编译、KMP 源码关联和消费运行分别按上面的结果报告。

普通模式的回归使用外部 init 脚本断言没有 Skiko 源码 build、阶段样例、showcase，
且没有给 ylcs-module:core 自动安装桥插件；随后编译原有 Core Desktop。
合并后此项验证通过：merged-ordinary-final.log，18s、20 tasks。
不会以普通模式运行结果代替 Native 三宿主验收。

macOS CInterop 在 Windows 上无法交叉处理的提示属于独立 KGP 检查，不受 ignoreDisabledTargets 控制。
生成的 Skiko 构建现通过 kotlin.suppressGradlePluginWarnings=CrossCompilationWithCinterops
定向抑制这条预期的宿主限制提示。三个目标和 KLIB 交叉编译配置保留，
C++/CInterop 和最终链接仍由匹配宿主执行；该设置不证明跨宿主 CInterop 已被实现。

## 三平台结果边界

当前 Windows 的 C++/KLIB、原生测试、软件/D3D12 窗口、ZIP 和 Maven 消费实际通过。
Core/Skottie/APP 的 Linux Kotlin 交叉编译已通过（skiko-static-window-final.log）；
Linux 和 macOS 的静态 SDK、C++ 最终链接与运行仍需要各自宿主 CI。

已读取远程 [run 40072504340](https://github.com/rachel-ylcs/ylcs-kmp/actions/runs/40072504340)，
对应旧提交 44091c05d198b8523000187c84d32fac6dcb4c22：Windows 成功，Linux/macOS 最终链接失败。
当前修复包括：

- Linux Fc*/glX*：通过库的 CInterop linkerOpts 导出 fontconfig/GL/dl，不只修某个样例。
- macOS Image 三个 bridge 符号：在上游任务全部注册后配置 adaptation C++ sourceRoots，防止后注册配置覆盖。
- SDL 和 bridge 使用 macOS 12.0 deployment target。
- 当前静态 SDL、阶段样例、独立 APP、ZIP 和独立 Maven 窗口消费均已接入三宿主工作流。

这些修改尚未推送，没有新的远程 CI 结果；Windows 成功不能证明 Linux/macOS 已修复。
CI 默认软件窗口基线，手动 gpu-smoke 另行要求 D3D12/Vulkan/Metal 实际后端。

## 当前复现命令

从 ylcs-kmp 根目录执行，使用 JDK 25，Windows 生产者已配置 VS_PATH：

```powershell
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:nativeBridgeReport
.\gradlew.bat "-Pnative.compose=true" :native-compose:adaptation:samples:skiko:windowsTest
.\gradlew.bat "-Pnative.compose=true" "-Pnative.smoke=true" :ylcs-app:native-compose:runWindowsReleaseExecutable :ylcs-app:native-compose:packageWindowsReleaseExecutable
.\gradlew.bat "-Pnative.compose=true" :native-compose:publishSkikoToVerificationRepository
.\gradlew.bat :build-logic:native-bridge:publishAllPublicationsToLocalVerificationRepository
python native-compose/scripts/verify_repository.py --target mingw_x64
.\gradlew.bat -p native-compose/verification/maven-consumer --refresh-dependencies "-Pnative.window=true" "-Pnative.legacyRequest=true" runWindowsDebugExecutable
python native-compose/scripts/verify_distributions.py --target mingw_x64 --renderer gpu --include-showcase
```

证据位于 vendor/build/samples/skiko/test-results/ 和 reports/tests/；阶段 ZIP 位于
vendor/build/samples/skiko/distributions/，独立 APP ZIP 位于 ../ylcs-app/native-compose/build/distributions/。
独立消费者输出位于 vendor/build/maven-consumer/。

本阶段尚无 Compose UI、完整输入/IME、多窗口、Wayland、性能基准、公共 Maven 发布或三宿主全部成功结果。
