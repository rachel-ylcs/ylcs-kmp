# 环境与工程结构

Rachel 的构建脚本本身也是工程的一部分。版本、目标平台和打包规则集中在根目录的版本目录与 `buildSrc` 中；阅读某个模块时，应同时查看它的 `build.gradle.kts`，不要假设每个模块支持同一组平台。

## 当前基线

| 项目 | 版本或约束 |
| --- | --- |
| Gradle Wrapper | 9.7.1 |
| Kotlin | 2.4.20 |
| Compose Multiplatform | 1.13.0-alpha01 |
| Android Gradle Plugin | 9.1.1 |
| Ktor | 3.5.2 |
| Coroutines | 1.11.0 |
| JDK / JVM target | 25 |
| Android | minSdk 29，compileSdk / targetSdk 37 |
| Android NDK | 29.0.14206865，仅 `arm64-v8a` |
| iOS deployment target | 26.0 |
| 应用版本 | 3.7.0（versionCode 370） |

不要用本机安装的 Gradle 替代 Wrapper。IDE 的 Gradle JVM 也必须指向 JDK 25；只把项目源码语言级别设为 25 仍不足以运行构建进程。

## 主机要求

- Android、JVM Desktop 与 Web 可以在常见桌面系统构建。
- iOS 与 macOS Native 需要 macOS/Xcode 工具链；当前模板只启用设备 `iosArm64`，模拟器目标被注释。
- Windows Native 由 `mingwX64` 构建，Linux Native 由 `linuxX64` 构建，macOS Native 由 `macosArm64` 构建。跨主机构建能力受 Kotlin/Native 工具链限制。
- Android 原生组件固定使用 arm64 ABI。仓库内预置的本地库也按这个约束组织。

## 目录地图

```text
ylcs-kmp/
├─ buildSrc/                    # 统一的 KMP、Android、打包与发布模板
├─ ylcs-module/
│  ├─ core/                    # 与 UI 无关的通用 Kotlin 能力
│  ├─ foundation/              # context、filesystem、network、startup
│  ├─ compose/                 # app、theme、ui、screen、组件与启动服务
│  ├─ cs/                      # 共享协议、客户端、原生服务端和服务插件
│  ├─ platform/                # OS、FFI、动态库加载
│  └─ native/                  # Win32 等原生封装
├─ ylcs-app/
│  ├─ app/                     # 按领域拆分的共享业务页面
│  ├─ android-app/             # Android 壳
│  ├─ desktop-app/             # Desktop 壳
│  ├─ web-app/                 # JS/Wasm 壳
│  ├─ server/                  # Kotlin/Native 服务端
│  ├─ gallery/                 # UI 展厅
│  └─ mod-manager/             # MOD 桌面工具
└─ ylcs-docs/                  # MkDocs 与 Dokka 站点
```

## 源集约定

模板不仅有标准的 `commonMain` 与平台源集，还会按能力建立中间源集：

| 源集 | 用途 |
| --- | --- |
| `commonMain` | 完全共享的声明与实现 |
| `clientMain` | Android、iOS、Desktop、Web 等客户端共有代码 |
| `webMain` | JS 与 WasmJS 共有实现 |
| `appleMain` / `iosMain` | Apple / iOS 共有实现 |
| `jvmMain` / `desktopMain` | JVM 或 Compose Desktop 实现 |
| `posixMain` / `nativeMain` | POSIX 或 Kotlin/Native 共有实现 |
| `skikoMain` | 依赖 Skiko 的 Compose 平台实现 |

先把代码放在能够真实编译的最上层公共源集；只有平台 API 不同才下沉到 `expect/actual`。不要为了“以后可能不同”提前复制实现。

## 模块坐标

发布坐标由 Gradle 项目路径自动生成：父路径变成 group，最后一段变成 artifact。

```text
:ylcs-module:compose:app       -> love.yinlin.compose:app:3.7.0
:ylcs-module:foundation:network -> love.yinlin.foundation:network:3.7.0
:ylcs-module:cs:client-engine -> love.yinlin.cs:client-engine:3.7.0
```

模块使用 `ExportLib` 分隔私有实现依赖与向消费者暴露的 API 依赖。使用者仍应显式声明自己直接调用的模块；这能在内部导出关系改变时保持构建稳定。

## 常用任务

```powershell
# 展厅桌面版
.\gradlew.bat :ylcs-app:gallery:galleryRun

# 完整桌面应用 Debug
.\gradlew.bat :ylcs-app:desktop-app:desktopRunDebug

# Wasm Web 开发服务器
.\gradlew.bat :ylcs-app:web-app:webRun

# Android Debug 包
.\gradlew.bat :ylcs-app:android-app:androidPackage

# 查询某个模块的实际任务
.\gradlew.bat :ylcs-app:server:tasks --all
```

Linux/macOS 将 `gradlew.bat` 换成 `./gradlew`。原生目标的任务名会包含目标名和构建类型，先查询任务比把某台机器上的名称硬编码进脚本更可靠。

## 构建配置的两个注意点

1. `buildSrc` 会根据当前操作系统和 CPU 架构选择打包行为。构建产物不一定能在另一主机上原样复现全部目标。
2. 根配置当前是生产环境，API 地址和发布路径会随 `BuildEnvironment` 变化。不要在业务源码中再维护一份环境判断；统一从生成配置或应用配置读取。

遇到奇怪的源集解析问题时，依次确认 Wrapper、JDK、宿主支持的 Native 目标，再查看具体模块是否关闭了目标。多数问题并不是共享代码本身，而是把某个模块不存在的目标当成了全仓库默认值。
