# 适配源码与构建归属

`adaptation/` 保存提交到 Git 的维护代码；`vendor/` 保存忽略的源码归档、唯一工作副本、SDK 和输出。
C/C++ 使用 4 空格缩进，保留 include 次序和宏定义体；不单独维护格式配置文件。

```text
native-compose/
  adaptation/
    org.jetbrains.skiko_skiko/
      build.gradle.kts
      src/
        commonMain/kotlin/org/jetbrains/skiko/window/
        nativeMain/kotlin/       # 共享 Native 实现、SDL 窗口循环
        nativeMain/cpp/          # Image、GL interface 等共享 bridge
        windowsMain/kotlin/      # Windows/D3D12，包括窗口 backend
        windowsMain/cpp/
        linuxMain/kotlin/        # Linux/Vulkan，包括窗口 backend
        linuxMain/cpp/           # Vulkan WSI、Linux C++ ABI 兼容
        macosMain/kotlin/        # macOS/Metal，包括窗口 backend
        nativeInterop/cinterop/sdl.def
    org.jetbrains.skiko_skiko-skottie/
      build.gradle.kts
    samples/skiko/              # 当前阶段：公共示例、Native 回归
  vendor/
    skiko/workspace/            # 唯一、固定路径的 Native-only 工作源码
    skiko/*.zip                 # 锁定的上游/参考/共同基线归档，不常驻展开副本
    skiko/skia/                 # 校验过的 Skia 下载归档
    skiko/state.json            # 当前版本、输入指纹和维护源码路径清单
    sdl-source-*/               # 校验过的 SDL 源码
    sdl-static/<konan-target>/  # SDL 静态 SDK、系统链接依赖清单
    build/
      logs/
      samples/
      native-compose/          # 本地验证 Maven 仓库
      maven-consumer/
  scripts/
  upstream/
  docs/
ylcs-app/native-compose/
  build.gradle.kts
  src/commonMain/kotlin/       # 只有业务场景与公共窗口 API 调用
```

## 共享代码统一到 Native

维护代码与准备后的活动源集都使用 `nativeMain/nativeTest`。
同步脚本合并上游历史共享目录，删除多余的 Native/JS hierarchy 节点，并对重名文件检查冲突。
当前只有 macosArm64 一个 Apple target，darwinMain/darwinTest 也统一合并到 macosMain/macosTest，
层级中去掉 Darwin/UIKit/iOS/tvOS 节点；维护的文件位置实现直接放在 macosMain。
platform.darwin 等 Kotlin/Native 系统 API 包名保留其官方名称。
锁定归档和来源映射保留上游路径与提交，便于升级比较；它们不构成本项目的活动源集。

只有 vendor/skiko/workspace 是持久展开的 Skiko 工作副本。
需要重新适配时，上游源码与参考/基线中的选定文件在临时目录中合并，完成后原地同步生成文件，
保留工作副本中已有的 build/.gradle 等输出，以及内容不变文件的时间戳。
正常修改 adaptation 的 Kotlin/C++ 内容由 Gradle 增量处理；新增、删除或移动源码文件时更新遮蔽关系。
指纹仅记录准备输入，不参与目录命名；准备成功后清理带有旧生成标记的 prepared/sources 副本。
其他未识别目录和 Git checkout 不会被当作缓存删除。

维护的 Image/GL C++ 放在 nativeMain；Linux Vulkan WSI 和 ABI 兼容放在 linuxMain。
`upstream/adaptation-origins.json` 将维护文件映射回原始社区路径，同步时按映射移除 vendor 的遮蔽文件。
不将手写代码复制成另一份可编辑的 vendor 副本；编译直接读取 adaptation。

## 窗口与 SDL 是库的实现

公共 API 位于 `org.jetbrains.skiko.window`：NativeWindow、NativeWindowOptions、NativeWindowBackend、NativeWindowKey。
NativeWindow 在 Native 层拥有 SDL 事件循环，内部接 Windows D3D12、Linux Vulkan、macOS Metal；软件基线也由库提供。

SDL CInterop 的定义、平台 adapters 和静态链接全部属于 Skiko Core。
生产者从锁定源码构建 SDL，Windows 与 Skia 共用 MSVC 静态 CRT 兼容方案，并过滤不兼容的 COFF directives。
SDK 静态归档与系统 linkerOpts 进入发布的 SDL CInterop KLIB。
消费方不配置 CInterop、不编译 SDL、不需要 SDL SDK 或平台窗口源码。

APP 只在 commonMain 引用官方 Skiko/Skottie 坐标并调用公共 API。
桥插件做 target 重定向、ICU 收集和启动/打包，不再承担 SDL SDK 下载、链接或 DLL 收集。
许可证保留在源码目录与 sources 工件中，不复制到程序暂存目录或 ZIP。
Skiko 的 LICENSE/NOTICE、SDL 源码中的 LICENSE.txt 直接用于 sources 工件；
来源说明从锁定清单生成到 vendor，不依赖维护目录中的 licenses 副本。

## Gradle 和 IDE

两个适配库属于同一个 Skiko included build。Settings 的 buildFileName 将 adaptation 的 build.gradle.kts
指定为正式项目脚本，项目目录仍在 vendor，以复用上游 buildSrc 和相对路径。
上游基础配置自动生成为 vendor 内的 rachel-upstream.gradle.kts。
不需要将库重复 include 为 ylcs-kmp 的普通子项目。

窗口绑定只在 Core 中 commonize；APP 没有额外的 CInterop 源集需求。
首次切换后 Reload All Gradle Projects，让 IDE 导入 vendor/skiko/workspace/skiko。
此后改变准备规则也继续使用同一路径；准备过程使用进程锁，避免同时改写工作副本。
维护开关仍默认 false；普通模式不准备或关联这些源码构建。

Core、Skottie、上游测试工具、维护样例、独立 Maven 消费样例及生成的 buildSrc
统一启用 -Xexpect-actual-classes 和 -Xcollection-literals。
独立 Rachel APP 已通过根 repository 的 languageFeatures 继承这两个参数。
adaptation 的 Kotlin 源码使用集合字面量、4 空格和独立分支排版，并按类组织文件；
Gradle Kotlin DSL 脚本继续使用 Gradle 内置 Kotlin 能解析的语法。
Skiko 位于框架依赖链底层，不为语法辅助函数反向依赖 Rachel 或 Compose。

桥插件的两个生命周期入口统一维护在 build-logic/native-bridge：src/main 是 Project 实现，
src/settings 是不依赖 KGP 的 Settings 实现。发布时 Settings marker 选择同一 GAV 的 settings classifier。
规则及 adaptation 路径可由消费模块的 nativeBridgeReport 查看；实际依赖选择使用 dependencyInsight。

## 依赖与目标的实际支持审计

审计以根版本目录选定的版本为准，按“依赖坐标 + target”记录结果；上游宣称支持或发布了 KLIB
只表示存在声明，不能直接标记为可复用。后续 Compose 和 AndroidX 依赖也采用相同规则：

1. 检查发布的 Gradle Module Metadata 中该 target 的变体、实际 KLIB 和传递依赖闭包，
   排除只发布 common metadata、只配置空 target 或依赖落到其他平台的情况。
2. 对照锁定版本的实际构建脚本，追踪叶子源集到共享源集的依赖及所有生效的 expect/actual。
   Compose fork 优先核对实际使用的 build-fork.gradle，不能把 AndroidX 原始脚本中的 Stubs 源集当成正式实现。
3. 阅读平台实现及调用方，排查 TODO()、NotImplementedError、error("Not implemented")、
   空函数、固定返回值、stub/mock，以及缺失的 CInterop/C++/系统服务。
   没有平台源码不一定缺实现，共享实现也可能完整；存在空函数也不一定是缺陷，需按 API 语义判断。
4. 分别记录“可复用”“有限支持”“缺失实现”“待运行验证”，写明影响的 API、源码位置和所需平台能力。
   仅对缺失的库/target 增加适配与桥插件规则；未检查的目标保持待审计，不能计为已支持。
5. 在对应宿主执行编译、链接和有实际行为断言的回归后，再更新运行结论。
   用户要求只改源码时，保持运行状态未验证，不以编译配置或 TODO 数量代替验收。

例如，Skiko 已记录 Native 空 reachabilityBarrier 和未实现的指针数组入口边界；
这些限制需要结合调用和 GC/FFI 语义评估。窗口、输入法、剪贴板和资源释放同样需要实际实现审计。
Runtime、Lifecycle、SavedState、Annotation、Collection 即使已有三个 Native 发布变体，也要逐项完成以上检查。

移植库的版本目录别名使用连字符：skiko-native、skiko-native-skottie，后续新增别名沿用同一形式。
对应 Kotlin accessor 为 libs.skiko.native 和 libs.skiko.native.skottie；
前者同时拥有子节点，需要 Provider 值时使用 libs.skiko.native.asProvider()。

## Skottie 为什么只有构建脚本

adaptation 只保存本项目维护的差异，不保存完整上游副本。
Skottie 的公共 API、共享 Native Kotlin 实现和 C++ bridge 已可复用，当前没有新增平台 Kotlin/C++ 文件。
adaptation/org.jetbrains.skiko_skiko-skottie/build.gradle.kts 注册 Windows target、
关联 Core 的 Native 符号输入并以 API 依赖 Core，继续应用生成的上游构建脚本。

实际源码位置以 vendor/skiko/state.json 的 build 字段为准：

```text
vendor/skiko/workspace/skiko/skiko-skottie/src/
  commonMain/kotlin/org/jetbrains/skia/skottie/  # Animation、AnimationBuilder 等 API
  nativeMain/kotlin/org/jetbrains/skia/skottie/  # Native 文件入口、静态加载实现
  nativeMain/cpp/skottie/                      # Animation/AnimationBuilder/Logger bridge
  nativeMain/cpp/sksg/                         # InvalidationController bridge
```

本项目的功能回归位于 adaptation/samples/skiko/src/nativeTest，涵盖实际时间轴像素变化及无效输入。
如果后续发现平台实现缺口，手写修复再加入此 adaptation 模块的对应源集；现有上游代码不重复复制。

Graphite 按确定的路线排除；AWT/JVM runtime 不迁移。
