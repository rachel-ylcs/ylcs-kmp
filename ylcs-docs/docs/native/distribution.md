---
description: 使用 composeDesktopPackage 生成 Native 桌面分发目录、处理符号、ICU 与上游验证。
---

# 分发与维护

<p class="rc-lead">Native 桌面分发不是一次链接。它需要可执行程序、Compose 资源、ICU 数据、平台运行文件、图标与符号归档共同组成一个可运行的目录。</p>

## composeDesktopPackage

```kotlin title="分发配置"
composeDesktopPackage {
    entryPoint = "example.main"
    packageName = "ExampleApp"
    displayName = "示例应用"
    packageVersion = "1.0.0"
    vendor = "Example"
    author = "Example Team"
    copyright = "Copyright Example"
    workingDirectory = rootProject.layout.projectDirectory.dir("work/example").asFile
    arguments = listOf("--profile", "development")

    windows { iconFile = file("icons/app.ico") }
    macos { iconFile = file("icons/app.icns") }
}
```

| 配置 | 作用 |
| --- | --- |
| `entryPoint` | 可执行程序入口 |
| `packageName`、`displayName` | 包名与显示名称 |
| `packageVersion`、`vendor`、`author` | 元数据 |
| `workingDirectory` | Gradle 运行任务的工作目录 |
| `arguments` | 运行参数 |
| `windows`、`macos` | 平台图标 |

`workingDirectory` 只设置 Gradle 启动进程的当前目录，不存在时自动创建。`arguments` 按独立参数传递，包含空格的参数无需手动加引号。两项配置都不会写入二进制、应用 Bundle 或启动脚本。

## 任务与输出

| 任务 | 结果 |
| --- | --- |
| `composeRun` | 运行宿主 Debug |
| `composeReleaseRun` | 运行宿主 Release |
| `composeDistribution` | 生成宿主 Release 完整目录 |

输出位于 `build/nativeApplication/<target>/<binary>/`：

| 平台 | 产物 |
| --- | --- |
| Windows | `ExampleApp.exe` |
| Linux | `ExampleApp.kexe` |
| macOS | `ExampleApp.app` |

`build/bin/` 中的单个链接产物不包含完整运行资源，不能直接作为分发包。

## 只使用桥插件

不使用 Rachel 应用模板时，可以配置 `nativeApplication { }`：

```kotlin title="nativeApplication"
nativeApplication {
    workingDirectory.set(file("work"))
    arguments.set(listOf("--profile", "development"))
}
```

该配置与 `composeDesktopPackage` 共享相同语义：只影响运行任务，不写入产物。

## 符号

Release 链接后自动分离符号：

| 平台 | 符号文件 |
| --- | --- |
| Windows / Linux | `<binary>.debug` |
| macOS | `<binary>.unstripped` 与 `.dSYM` |

符号归档在 `build/nativeSymbols/<target>/<binary>/`，不复制进分发目录。`symbols.properties` 记录剥离前后摘要，旁边的 `<binary>-distribution.properties` 记录最终分发文件摘要。

!!! warning "清理 build 前归档符号"
    Debug 构建不生成同等符号。普通 Release 未生成的 DWARF 行号信息不会凭空补齐；Windows 的 `.debug` 是 COFF，不是 PDB。

Linux 使用 `objcopy`，Windows 使用 Kotlin/Native 下载的 MinGW `objcopy`。特殊工具链可通过 Gradle 属性指定。

## ICU 与文本

应用默认分发 Skia SDK 自带的裁剪版 ICU 数据，包含中英文文本边界规则、中日文词典与 Unicode 规范化等基础数据。三个桌面目标共用同一份小端数据，桥插件在分发目录中命名为 `icudtl.dat`。

裁剪版不包含完整的各语言日期、数字、货币资源。需要完整本地化时，应选用完整 ICU 数据，并在发布前验证日期与数字格式。

## 资源与运行目录

- Compose 资源与平台运行数据位于分发目录；
- 直接启动分发后的程序时，工作目录与参数由启动方式决定；
- 应用不内置仓库工作路径；
- 图标在 Windows 嵌入 EXE，在 macOS 使用 `.app`、Info.plist 与 ICNS。

## 上游同步与验证

Native Compose 的维护者使用以下流程：

```bash title="准备与验证"
./gradlew -p build-logic :repository-conventions:prepareNativeSources
./gradlew -p build-logic :native-bridge:validatePlugins :public-plugin:validatePlugins
./gradlew :native-compose:testHost
./gradlew :native-compose:publishHost
./gradlew :native-compose:verifyHost
```

| 任务 | 作用 |
| --- | --- |
| `prepareNativeSources` | 按版本目录选择上游源码、校验摘要、应用补丁 |
| `testHost` | 执行当前宿主库测试 |
| `publishHost` | 输出到当前代的本地仓库 |
| `verifyHost` | 校验 Maven 文件、目标、KLIB 身份、静态库与依赖闭包 |

`verifyHost` 会检查缺失、失败或跳过的测试，不能把跳过的测试当作通过。

### 上游目录

| 目录 | 内容 |
| --- | --- |
| `adaptation/<group>_<artifact>/` | 适配源码、C++、CInterop 与测试 |
| `upstream/*.json` | 上游仓库、工件与构建配置 |
| `upstream/patches` | 上游差异补丁 |
| `adaptation/provenance/` | 适配源码出处 |
| `vendor/` | 忽略的上游源码、SDK 与构建输出 |
| `verification/` | 独立 Maven 发布/消费验证 |

`vendor/` 由同步器管理，不应直接修改生成源码。维护差异放入适配源码或补丁。

## CI 与宿主

- Windows 构建 SDL 需要 x64 MSVC、Windows SDK 与 Ninja；
- Linux 需要桌面开发包；
- macOS 需要 Xcode；
- macOS 的 Gradle 作业会限制 worker 与 Native 后端线程，避免冷构建争用内存；
- 桌面 Native 作业按当前代与宿主镜像恢复 SDL SDK。

## 交付检查清单

- [ ] 使用 `composeDistribution` 的完整目录；
- [ ] Compose 资源、ICU 与平台运行文件齐全；
- [ ] 图标、版本信息与 Bundle 元数据正确；
- [ ] 符号已单独归档并记录摘要；
- [ ] 透明、无边框、焦点、缩放与穿透按平台验证；
- [ ] 托盘、文件选择、输入法与系统主题在目标桌面验证；
- [ ] 独立 Maven 消费与 `verifyHost` 通过。

## 下一步

进入[模块全景](../module/index.md)，从更底层理解 Rachel 的模块边界与扩展方式。
