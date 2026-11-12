# 构建插件与模块配置

Rachel 的构建逻辑由通用插件、本仓库约定和桌面 Native Compose 桥接三部分组成。模块应使用与项目版本目录一致的插件版本，避免将业务仓库配置当作通用插件 API。

## 构建逻辑分层

| 位置 | 职责 |
| --- | --- |
| `build-logic/public-plugin` | `love.yinlin.rachel`，提供 `install` 启动入口、`rachel {}` 与 Kotlin/Android/CMake 模板 |
| `build-logic/repository-conventions` | `ylcs.repository`，管理本仓库模块注册、常量、版本、路径与 Native 上游源码准备 |
| `build-logic/native-bridge` | `love.yinlin.native-bridge` 及其 Settings 配套插件，管理 Compose Native 依赖替换、窗口运行时和应用分发 |
| `build-logic/native-binaries` | Release 符号分离等共享实现；源码由前述插件编译使用，不是单独登记的 Gradle 子项目 |

根 Settings 通过 `pluginManagement.includeBuild("build-logic")` 引入插件构建。`repository {}` 顶层声明仓库身份，`rachel {}` 提供构建默认值，`ylcs {}` 提供应用配置，`modules {}` 注册项目。

## 模板与目标

| `rachel` 模板 | 用途 | 目标选择 |
| --- | --- | --- |
| `multiplatform` | 共享库与 Compose 应用 | iOS、Desktop JVM、Web 默认开启；Android 需要相应插件；桌面 Native 默认关闭 |
| `native` | Kotlin/Native 共享模块 | 桌面 Native 目标按开关启用 |
| `nativeExecutable` | 原生可执行程序，如服务端 | 同上，并配置可执行二进制和运行参数 |
| `nativeLibrary` | 原生库，如服务端引擎与 Win32 库 | 同上，并配置库二进制 |
| `jvm` | JVM 专用库，如 Foreign Function & Memory API 封装 | JVM |
| `js` | 浏览器 JavaScript 可执行模块与 TypeScript 声明 | JS，不包含 WasmJS |
| `androidApplication` | Android 安装包入口 | Android |
| `androidLibrary` | Android 专用库与 NDK 配置 | Android；区别于 KMP Android library target |

`nativeDesktopTarget = true` 同时开启 Windows、Linux、macOS。`windowsTarget`、`linuxTarget`、`macosTarget` 可分别覆盖该默认值。iOS 是否配置模拟器仍取决于宿主，见[编译环境](environment.md)。平台已声明不等同于每项功能均有实现；模块支持矩阵见[仓库首页](https://github.com/rachel-ylcs/ylcs-kmp#全平台模块支持)。

每个模块只能选择一种 Rachel 模板，重复声明会由 `RachelExtension` 拒绝。需要同时构建 JVM 与 Native Compose 的模块应使用一个 `multiplatform` 模板开启相应目标，并在其中分别声明两种桌面打包配置。

## 插件与依赖

根构建脚本预加载插件；子模块通过 `plugins { install(...) }` 声明所需插件。`sources` 中显式建立源集关系，`lib` 使用版本目录和 `projects.*` 添加依赖。

下列片段来自 [MOD Manager 构建脚本](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/mod-manager/build.gradle.kts)，其余依赖与资源设置仍需保留：

```kotlin
multiplatform {
    iosTarget = false
    desktopTarget = false
    webTarget = false
    nativeDesktopTarget = true
}
```

这表示该应用使用三个桌面 Native 目标，不启用 JVM、Web 或 iOS。该模块还安装 Compose、Compose Compiler、Serialization 与 Native Bridge 插件。示例仅展示模板开关，完整模块不能省略源码依赖配置。

`lib` 中 `ExportLib` 之前为实现依赖，之后为 API 导出依赖。公开签名出现的外部类型应具有相应导出关系。共享源集关系由 `sourceSet(parent...)` 显式设置，不能仅根据目录名称推断继承。

## JVM 与 Native 桌面分发

| 配置 | 运行时 | 说明 |
| --- | --- | --- |
| `desktopPackage {}` | JVM / JRE | 官方 Compose Desktop 打包、JVM modules、ProGuard 与可选 AOT |
| `composeDesktopPackage {}` | Kotlin/Native | 桥插件分发可执行程序、Compose 资源、ICU 与平台运行数据 |

两者可以在同一模块中配置，不能互相代替。`composeDesktopPackage` 要求应用 Native Bridge 插件并启用至少一个桌面 Native 目标。

MOD Manager 实际使用：

```kotlin
composeDesktopPackage {
    entryPoint = "main"
    packageName = "ModManager"
    displayName = "Rachel MOD Manager"
    versionedArchive = false
}
```

`versionedArchive = false` 省略桥插件生成的 Release ZIP 文件名中的 Gradle 版本段。MOD Manager 的工作目录在下文所述的 `runs {}` 中配置。

Native `composeRun` 和 `composeReleaseRun` 分别运行宿主 Debug/Release；`composeDistribution` 生成宿主 Release 完整目录。输出位于 `build/nativeApplication/<target>/<binary>/`。`build/bin/` 的单个链接产物不包含全部运行资源。Release 符号位于独立的 `build/nativeSymbols/`，需要随构建另行归档，详细规则见 [Native Compose](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/native-compose/README.md)。

## 发布应用产物

在模板内用 `artifacts {}` 注册发布任务。模板提供带生产任务依赖的文件或目录，模块声明发布任务名、目的地和文件名。

| 产物来源 | 模板提供的输出 |
| --- | --- |
| Android Release APK | `androidReleaseApk` |
| JVM Desktop Release 应用目录 | `desktopReleaseDistribution` |
| 当前宿主的 Native Desktop Release ZIP | `nativeDesktopReleaseArchive` |
| 浏览器分发目录 | `browserDistribution(BrowserArtifactTarget.Js)` 或 `BrowserArtifactTarget.Wasm`，单 JS 模板提供 `jsDistribution` |
| 指定平台的 Native Release 可执行文件 | `releaseExecutable(BuildPlatform.Linux)` 等 |

`copy` 复制单个文件，可指定新文件名。`zip` 将目录归档，可声明归档内的顶层目录和排除规则。`sync` 将分发目录同步到指定位置。`aggregate` 将多个发布任务组合为一个入口。示例中的 `C` 属于本仓库的 `repository-conventions`，通用工程可以传入自己的目标目录。

```kotlin
artifacts {
    copy("androidArtifact", androidReleaseApk, C.root.outputs, "ylcs-android.apk", "安卓端Release打包")
}
```

## 配置开发运行

`runs {}` 配置 Gradle 启动的进程，与应用打包配置分开。使用 `jvm`、`nativeDesktop` 或 `nativeExecutable` 设置工作目录、命令行参数、环境变量和动态库目录。JVM 还支持 `jvmArgs`，其动态库目录写入 `java.library.path`。Native 将动态库目录加入宿主平台的进程搜索路径。

`debug(...)` 和 `release(...)` 选择构建模式并可注册运行别名。两个模式需要不同配置时，可以分别声明。浏览器使用 `browser(...)` 为 JS 或 Wasm 注册运行入口，开发服务器继续由 `webpack {}` 配置。

```kotlin
runs {
    jvm {
        debug("desktopRunDebug", "运行 Debug 桌面端")
        release("desktopRunRelease", "运行 Release 桌面端")
        workingDirectory = C.root.work.desktop.asFile
        jvmArgs = listOf("-Dnative.library.resource.disabled=true")
        libraryDirectories = listOf(C.root.artifacts.desktopNative.asFile)
        buildDesktopNativeLibraries = true
    }
}
```

## CInterop 与原生依赖

`native { cpp = listOf("shared") }` 为启用的桌面 Native 目标注册共享 CInterop，`windows { cpp = listOf("win32") }` 等块添加平台条目。定义文件位于 `src/nativeInterop/cinterop/<name>.def`。同名条目去重，多次设置采用累积配置。

CInterop 绑定生成、外部 C/C++ 库构建和最终应用分发属于不同环节。桌面 JNI 的 MMKV、picker、miniaudio 等依赖仍需按对应模块与 CI 动作准备，不能将 Gradle 编译成功视为动态库已正确部署。

## 测试与文档工具模块

`ylcs-module/build-tool` 聚合现有测试任务，不提供应用运行 target。聚合任务包括 `androidTest`、`iosTest`、`desktopTest`、`jsTest`、`wasmJsTest`、`windowsTest`、`linuxTest`、`macosTest`。iOS 聚合的是 `iosSimulatorArm64Test`，Desktop 同时收集 KMP 的 `desktopTest` 与 JVM 模块的 `test`。没有对应目标任务的模块不会自动获得测试覆盖。

`ylcs-module/dokka` 汇总模块 API；输出目录由本仓库常量指向 `ylcs-docs/docs/overrides/dokka`。手写指南使用 MkDocs，与 Gallery、ModernScreen、Dokka 的生成页面分开维护。

## 源码入口

- [KotlinMultiplatformTemplate](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/build-logic/public-plugin/src/main/kotlin/love/yinlin/gradle/KotlinMultiplatformTemplate.kt)
- [KotlinNativeTemplate](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/build-logic/public-plugin/src/main/kotlin/love/yinlin/gradle/KotlinNativeTemplate.kt)
- [RepositorySettings](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/build-logic/repository-conventions/src/main/kotlin/love/yinlin/repository/RepositorySettings.kt)
- [测试聚合脚本](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/build-tool/build.gradle.kts)
