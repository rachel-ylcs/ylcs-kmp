---
description: 使用 Rachel 的运行任务、打包配置与产物注册完成多平台交付。
---

# 运行与打包

<p class="rc-lead">运行与打包是两条不同的路径：运行任务关注工作目录、参数与调试库，打包配置关注可分发产物、资源与签名。Rachel 将二者分开声明，避免用同一条配置承担两种责任。</p>

## 常用运行任务

```powershell title="开发运行"
# UI 组件展厅
.\gradlew.bat :ylcs-app:gallery:galleryRun

# 导航示例
.\gradlew.bat :ylcs-app:modern-screen:modernScreenRun

# 完整桌面应用
.\gradlew.bat :ylcs-app:desktop-app:desktopRunDebug

# Web 开发服务
.\gradlew.bat :ylcs-app:web-app:webRun

# Android 调试包
.\gradlew.bat :ylcs-app:android-app:androidPackage

# 原生服务端
.\gradlew.bat :ylcs-app:server:serverRunDebug
```

Linux 与 macOS 使用 `./gradlew`。任务名称以模块的 `runs { }` 配置为准；若任务不存在，先执行 `tasks --all` 查询，而不是猜测名称。

## 运行配置

`runs { }` 配置 Gradle 启动的进程，与应用打包配置分开：

```kotlin title="runs"
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

| 配置 | 作用 |
| --- | --- |
| `workingDirectory` | 进程当前目录，不影响应用数据目录 |
| `jvmArgs` | JVM 参数，仅 JVM 运行生效 |
| `libraryDirectories` | 动态库搜索路径 |
| `debug` / `release` | 选择构建模式并注册运行别名 |
| `browser` | 注册 JS 或 Wasm 运行入口 |

Native 运行任务使用 `nativeDesktop` 或 `nativeExecutable` 配置。动态库目录会被加入宿主平台的进程搜索路径。

## 两种桌面分发

| 配置 | 运行时 | 产物 |
| --- | --- | --- |
| `desktopPackage { }` | JVM / JRE | 官方 Compose Desktop 打包、ProGuard 与可选 AOT |
| `composeDesktopPackage { }` | Kotlin/Native | 桥插件分发可执行程序、Compose 资源、ICU 与平台运行数据 |

两者可以在同一模块中配置，但不能互相替代。`composeDesktopPackage` 要求应用 Native Bridge 插件并启用至少一个桌面 Native 目标。

```kotlin title="Native 桌面分发"
composeDesktopPackage {
    entryPoint = "main"
    packageName = "HelloApp"
    displayName = "Hello Rachel"
    versionedArchive = false
}
```

| 任务 | 结果 |
| --- | --- |
| `composeRun` | 运行宿主 Debug 版本 |
| `composeReleaseRun` | 运行宿主 Release 版本 |
| `composeDistribution` | 生成宿主 Release 完整目录 |

!!! warning "不要直接复制链接产物"
    `build/bin/` 中的单个链接产物不包含全部运行资源。应使用 `composeDistribution` 生成的完整目录，或由桥插件生成的分发包。

## 注册发布产物

在模板内用 `artifacts { }` 注册发布任务。模板提供带生产任务依赖的文件或目录，模块只声明发布任务名、目的地和文件名。

| 产物来源 | 模板输出 |
| --- | --- |
| Android Release APK | `androidReleaseApk` |
| JVM Desktop 分发目录 | `desktopReleaseDistribution` |
| 当前宿主的 Native Desktop ZIP | `nativeDesktopReleaseArchive` |
| 浏览器分发目录 | `browserDistribution(...)` |
| 指定平台的 Native 可执行文件 | `releaseExecutable(...)` |

```kotlin title="artifacts"
artifacts {
    copy(
        "androidArtifact",
        androidReleaseApk,
        C.root.outputs,
        "hello-android.apk",
        "Android Release",
    )
}
```

`copy` 复制单个文件，`zip` 归档目录，`sync` 同步分发目录，`aggregate` 组合多个发布任务。产物注册不替代签名、商店上传或部署脚本。

## Android

Android 产物由 Android 插件与 Rachel 模板共同生成。打包前确认：

- Manifest 指向正确的 Application 与 Activity；
- 所需权限与 FileProvider 已声明；
- 目标 ABI、NDK 与原生依赖匹配；
- Release 签名配置来自本地安全配置，不进入版本控制。

## iOS

iOS 需要 macOS 与 Xcode。首次打开工程或修改 SwiftPM 依赖后，先为 `portal` 及其传递依赖生成链接包：

```bash title="生成 SwiftPM 链接包"
XCODEPROJ_PATH="$PWD/ylcs-app/iosApp/iosApp.xcodeproj" \
  ./gradlew :ylcs-app:app:portal:integrateLinkagePackage
```

随后在 Xcode 中归档。工程会在 Swift 编译前构建对应架构的 framework。仅执行 Kotlin 链接任务不能验证 Swift 宿主编译与 Xcode 归档。

## Web

Web 开发服务由模块的 `webRun` 或浏览器运行配置启动。构建产物是静态站点，可直接部署到任意静态托管；浏览器请求仍受 CORS、Cookie 与混合内容策略约束。

## 原生服务端

服务端使用 Kotlin/Native 与 Ktor CIO。运行任务会传入 `--cd` 指向工作目录；配置文件、静态目录、日志目录与数据库连接都需要在部署环境中准备。

```powershell title="服务端"
.\gradlew.bat :ylcs-app:server:serverRunDebug
```

`serverPublish` 复制目标平台的 Release 可执行文件，用于部署。数据库、Redis、静态资源与反向代理不属于单个可执行文件。

## 文档与示例页面

文档发布流程会：

1. 生成 Gallery 与 ModernScreen 的 Web 示例；
2. 生成 Dokka API 页面；
3. 由 MkDocs 构建手写指南并发布站点。

手写指南只负责概念与用法；组件交互与 API 签名以生成页面为准。

## 交付检查清单

- [ ] 目标平台的实际运行任务已验证；
- [ ] 资源与工作目录在干净环境中可解析；
- [ ] Release 产物来自完整分发目录，而不是链接产物；
- [ ] 原生库、图标、权限与签名已包含；
- [ ] 没有把本地路径、令牌或真实用户数据写入产物与日志。
