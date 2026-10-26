# Rachel Desktop Native Compose

## 概况

本目录维护 Windows x64、Linux x64、macOS arm64 的 Kotlin/Native 桌面 Compose 移植，三个目标均使用 Rachel 移植工件。

共 26 个工件：Skiko Core/Skottie、UI 八工件、Foundation 两工件、Animation 三工件、Resources、Navigation3 UI、Material3 主族四工件、Material Ripple、Adaptive 四工件。Runtime 等已具备三个目标支持的依赖使用官方工件。

基线由根 [libs.versions.toml](../libs.versions.toml) 和 [upstream](upstream) 锁定：CMP/Material3 `1.13.0-beta01`、Adaptive `1.4.0-alpha01`、Navigation3 `1.2.0`、Skiko `0.156.0`、SDL `3.4.18`。桥插件使用 Rachel 发布版本，移植库使用对应的上游版本。

## 目标与源码边界

提供标准 Compose API、桌面 Native 窗口、资源读取与部署、Maven 发布/消费及应用目录分发。桥插件按 Native target 将官方请求重定向为 `love.yinlin.native.*` 工件；JVM、Android 等目标使用官方 Compose。KLIB 保留上游模块身份，以兼容官方 Runtime、AndroidX 等预编译依赖。

- `adaptation/<上游 group>_<artifact>/`：维护的 Gradle buildFile、Kotlin/C++ 实现、cinterop 和单元测试。`src` 统一使用 `nativeMain`、`windowsMain`、`linuxMain`、`macosMain` 及对应 `Test`；cinterop 位于 `nativeMain/cinterop`。
- `upstream/*.json`：源版本、仓库、提交、归档 SHA-256、工件及来源清单。`upstream/patches` 保存受版本、提交和哈希约束的上游差异，保留原始许可证和版权。
- `gradle/templates`：Compose included build 模板。上游 `commonMain`、`skikoMain` 等编译片段由生成配置组织，保留元数据和 expect/actual 边界，不要求适配目录增加相同层次。
- `vendor`：忽略的上游源码、归档、SDK 和构建输出，由 Kotlin 同步器管理。维护差异放入适配源码或补丁，不直接修改生成源码。
- `verification`：独立 Maven 发布/消费工程，验证脱离源码复合构建后的使用方式。

源码同步位于 `build-logic/repository-conventions`，SDL 构建及验证工具位于 `build-logic/native-bridge`。维护流程使用 Gradle/Kotlin，不依赖 Python。

## 维护端使用

需要 JDK 25、根 Gradle Wrapper、Git。Windows 需要 MSVC/Windows SDK；Linux 需要桌面开发包；macOS arm64 需要 Xcode。IDEA 的 Gradle JVM 使用 JDK 25。Gradle Settings 自动准备和登记本地移植源码。

```text
./gradlew -p build-logic :repository-conventions:prepareNativeSources
./gradlew -p build-logic --offline :repository-conventions:prepareNativeSources
./gradlew -p build-logic :repository-conventions:checkNativeUpstreamUpdates
./gradlew -p build-logic :repository-conventions:test :native-bridge:test :native-bridge:validatePlugins
./gradlew :native-compose:testHost :native-compose:publishHost
./gradlew :native-compose:verifyHost
```

`prepareNativeSources` 获取锁定归档和稀疏 Git checkout、应用补丁并生成 included builds，不执行 CMake、库编译或应用运行。离线模式要求所需归档、Git 对象及源码选择已存在。同步保留构建输出和未变化文件的时间戳，遇到本地修改、未知文件或上游漂移时保留现场并失败。

Material3、Adaptive、Navigation3 使用官方 macOS sources JAR 中的真实共享实现；缺失 actual 由适配源码提供。日期和数字本地化共用 Skia ICU，macOS 的 `CalendarLocale` 保留 `NSLocale` 类型身份。

`testHost` 执行当前宿主库测试；`publishHost` 输出到 `native-compose/vendor/build/repository`；`verifyHost` 校验 Maven 文件、校验和、目标、KLIB 身份、静态库、许可、依赖闭包及维护测试报告。缺失、失败或跳过的测试不能作为通过结果。

唯一 Native 验证工作流为 [native-compose.yml](../.github/workflows/native-compose.yml)。三个宿主分别构建、测试、本地发布，再由 Kotlin 汇总一份 macOS KMP root 和三个 target publication。源码缓存任务为 `:repository-conventions:restoreNativeSourceCache`、`:repository-conventions:createNativeSourceCache`；快照排除构建输出，恢复后仍校验来源和输入。静态 SDK 使用单独缓存。

升级维护流程：

1. 确定 CMP、Skiko 及独立版本库的目标版本，核对官方 tag/commit、Maven source archive、Skia 资产和社区参考变化，更新根版本目录及锁定项。
2. 在临时目录取得对应原始源码，审核和重做 `upstream/patches`。先执行 `git apply --check`，保留真实功能、平台实现和许可证。更新补丁清单的 SHA-256、提交、版本及参考身份，不能只修改版本标签。
3. 核查适配实现、源集、依赖导出、KLIB 身份、资源所有权及测试；Kotlin 位置解构等兼容性修改保留为受版本约束的补丁。
4. 准备源码并验证对应宿主和独立 Maven 消费，刷新 IDEA Gradle 模型。

## 消费端使用

应用与 CMP/Skiko 兼容的桥插件，在普通 `commonMain` 中声明官方 Compose 依赖。消费工程不需要检出本目录或构建 Skia/SDL。

```kotlin
plugins {
    install(libs.plugins.rachel, libs.plugins.kotlinMultiplatform, libs.plugins.composeCompiler, libs.plugins.nativeBridge)
}

rachel {
    multiplatform {
        windowsTarget = true
        linuxTarget = true
        macosTarget = true
        sources {
            commonMain { lib(libs.compose.material3) }
        }
    }
}
```

按需使用 `libs.compose.ui`、`libs.compose.foundation`、`libs.compose.material3`、`libs.compose.material3.windowSizeClass`、`libs.compose.material3.adaptive.navigation.suite`、`libs.compose.adaptive.layout`、`libs.compose.adaptive.navigation`、`libs.compose.adaptive.navigation3`。Material3 的基础 UI、Foundation 和 Ripple 随依赖图引入。Resources 同时应用官方 Compose Multiplatform 插件，沿用 `src/commonMain/composeResources` 和生成的 `Res`。

桌面入口位于 Native 源集，调用 `androidx.compose.ui.window.NativeWindow`。应用安装桥插件后，可在 `rachel.multiplatform` 配置：

```kotlin
composeDesktopPackage {
    entryPoint = "example.main"
    packageName = "ExampleApp"
    displayName = "示例应用"
    packageVersion = "1.0.0"
    vendor = "Example"
    author = "Example Team"
    copyright = "Copyright 2026 Example"
    windows { iconFile = file("icons/app.ico") }
    macos { iconFile = file("icons/app.icns") }
}
```

该配置与 JVM 的 `desktopPackage {}` 独立，同一模块可同时配置。`composeRun` 运行宿主 Debug，`composeReleaseRun` 运行 Release，`composeDistribution` 输出 `build/nativeApplication/<target>/releaseExecutable` 普通目录，包含 Compose 资源和平台运行数据。Windows 图标/版本信息嵌入 EXE；macOS 使用 `.app`、Info.plist 和 ICNS。自定义 `composePublish` 可消费此目录进行复制或 ZIP 打包。

显式验证已生成应用目录的搬移与启动：

```text
./gradlew :ylcs-app:native-compose:composeDistribution
./gradlew :native-compose:verifyDistributions
```

`verifyDistributions` 需要已有 Debug 和 Release 分发目录，并会运行其中的程序。CI 在验证前准备这两个目录。

框架库沿用自身 Maven group/GAV，发布 KMP root 及启用的 Native target publication。JVM、Android 等用户按普通方式依赖；Native 用户应用匹配版本的桥插件。公开 Compose 类型按模块 API 导出规则声明，保持消费者可见的依赖完整。
