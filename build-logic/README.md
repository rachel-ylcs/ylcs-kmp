# Rachel 构建插件

`build-logic` 是通过 `pluginManagement.includeBuild` 接入的独立 Gradle 构建。项目不再依赖 `buildSrc`。

## 边界和配置入口

| 部分 | 职责 | 对外发布 |
| --- | --- | --- |
| `public-plugin` | 模板、源集和依赖 DSL、公共配置类型、`install`、代码生成、CMake/JNI 和桌面打包能力 | 是，插件 ID 为 `love.yinlin.rachel` |
| `repository-conventions` | Rachel 仓库的配置分发、`C` 读取入口、业务目录和终端信息展示 | 否，设置插件 ID 为 `ylcs.repository` |
| 根 `settings.gradle.kts` 的 `repository {}` | 当前仓库实际采用的数值和规则 | 否 |

`repository {}` 将本地配置放在一个入口中，按用途分成三部分：

- `shared`：框架与银临茶舍共用的命名空间和仓库主页；版本自动从版本目录注入。
- `rachel`：语言选项、JVM/SDK/ABI、资源、Native 工具链与库发布约定。
- `ylcs`：应用名称、服务环境和 MOD 管理器等业务信息。

所有自有版本统一定义在根 `libs.versions.toml` 的 `[versions]` 中：`rachel` 是公开构建插件、框架 Maven 产物与 APP 的共同版本名，`rachelCode` 是整数版本号，`rachelMinimum` 和 `rachelMinimumCode` 是业务最低兼容版本。`settings.gradle.kts` 不再重复这些数值。为保留原有产物命名，业务/框架 Gradle 项目的 `version` 仍使用整数版本号，Maven 坐标与桌面安装包使用版本名。

本地设置插件构建时，通过 Gradle 的 `WriteProperties` 任务从类型安全的 catalog 访问器生成版本资源，再由配置模型读取。这样设置阶段无需自行解析 TOML，也不会依赖尚未生成的项目 catalog 扩展。

设置脚本完成后，本地设置插件冻结配置，并通过 `gradle.lifecycle.beforeProject` 的隔离回调为每个项目创建独立副本。`Project.C` 从当前项目扩展读取数据；没有保存 `Project` 的静态 Map，也不在任务执行时通过 Map 查找常量。公开插件不会读取 `C`、银临茶舍目录、业务域名或签名文件。

公开代码合并在一个模块中。`public-plugin/src/bootstrap/kotlin` 保存不依赖 KGP/AGP/Compose 的轻量配置类型、`install` 和生成源码产物协议，供本地设置插件通过内部 `bootstrapElements` 产物提前加载；公开插件的完整 JAR 包含这些类。这个内部产物不单独发布到 Maven，仓库外只需一个公开插件及其 marker。

## 本机属性

本地设置插件在每轮配置开始时加载根目录的 `local.properties`，解析一次后向 Settings 和各 Project 提供同一份只读配置。脚本导入 `love.yinlin.repository.localProperties` 后，直接访问 `localProperties.mirrorEnabled`、`localProperties.androidKeyName` 和 `localProperties.androidKeyPassword`。未来的常用属性只需在 `LocalProperties` 中增加具名 getter；临时属性可以用 `localProperties["some.key"]` 读取，缺少该键时返回 null。

`mirrorEnabled` 保留原有的“存在 mirror.enabled 键即启用”语义；文件缺失、不可读或格式错误时镜像关闭。其他属性在文件不可用时报告错误；签名配置名 `androidKeyName` 是必需的非 null 属性，缺失时仍由 Android 签名原有的 try/catch 处理。读取使用 Gradle 的 `providers.fileContents(...).asBytes`，保持 Java Properties 的原有编码和转义规则，且让文件内容变化或创建、删除使配置缓存失效。属性对象不持有 Settings、Project 或可写的 Properties，也不跨构建保存静态缓存。任务需要属性时，应在配置阶段赋给任务输入。

## Settings DSL

根设置脚本只保留 included build 引导、设置插件声明和本地仓库 DSL：

```kotlin
pluginManagement { includeBuild("build-logic") }
plugins { id("ylcs.repository") }

repository {
    name = "ylcs-kmp"
    typeSafeProjectAccessors = true

    // shared、rachel 和 ylcs 配置仍放在这里。
    modules {
        module("ylcs-module") {
            module("build-tool")
            module("compose") {
                module("app")
            }
        }
        module("ylcs-app") {
            module("cs")
        }
    }
}
```

`module` 自动拼接当前父路径，以上声明分别包含 `:ylcs-module:build-tool`、`:ylcs-module:compose:app` 和 `:ylcs-app:cs` 等项目。父节点本身也注册为 Project，和 Gradle 原本由子模块自动创建父项目的结果一致；空的 `module("name") {}` 也能独立注册项目。参数使用相对路径，可用 `module("compose:core")` 缩写中间层，空路径、前导/尾随冒号和空路径段会在声明时被拒绝。简短清单仍可使用 `modules("ylcs-module:core", "ylcs-app:cs")`；两种写法都只收集路径，在 settings 完成时统一注册。

插件仓库默认使用 Maven Central、Google 和 Gradle Plugin Portal，依赖仓库默认使用 Maven Central 和 Google，与原来的用途、顺序一致。`localProperties.mirrorEnabled` 开启时，两类仓库都优先添加相应的 Aliyun 镜像，再保留官方源作为后备。这些行为无需在根脚本重复声明；需要覆盖时可写 `repositories { aliyun = false }`，也可分别设置 `mavenCentral`、`google` 和 `gradlePluginPortal` 开关，其中 Plugin Portal 仅用于插件仓库。版本目录默认从根 `libs.versions.toml` 创建 `libs`，可通过 `versionCatalogFile` 更换文件路径。

`repository {}` 先收集普通配置数据，设置插件在 `settingsEvaluated` 中统一设置项目名称、预览功能、模块、仓库和版本目录，再冻结项目所需的数据、分发到各 Project。此时 settings 尚未禁止修改，Project 尚未开始配置，`libs.*` 和 `projects.*` 访问器也尚未生成。DSL 后面的普通 settings 语句不会立即看到这些延迟应用的结果；需要调整模块或仓库时，应放进同一 DSL 中。

最前面的 `pluginManagement {}` 负责让 Gradle 找到本地设置插件。设置插件自己尚未被加载时，无法用自己的默认配置解析自己；如果以后在 settings 的 `plugins {}` 中加入外部设置插件，需要另外提供该引导阶段的解析仓库。`build-logic/settings.gradle.kts` 属于独立构建，仍使用自己的引导配置。

## 仓库内的插件声明

根项目统一请求版本，模块保留多行 `vararg` 写法：

```kotlin
// 根 build.gradle.kts
plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
        apply = false,
    )
}
```

```kotlin
// 模块 build.gradle.kts
plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
    )
}
```

`install(..., apply = false)` 通过 catalog alias 预加载版本；模块的 `install(...)` 应用已加载的实现，不重复请求版本。因此新插件需要先加入根项目的预加载列表。两种调用均直接接受 `Provider<PluginDependency>`，脚本中不维护插件 ID 字符串或全局注册列表。

`install` 特意放在默认包中，并使用 `inline`：Gradle 会单独编译 `plugins {}`，该阶段不会保留脚本外面的普通导入，也可能使用不同的类加载器。

设置插件的引导声明仍在 `settings.gradle.kts` 中使用 `id("ylcs.repository")`。该阶段早于本构建版本目录访问器的生成，不能使用项目的 `libs.plugins`。

## 模块 DSL

```kotlin
import love.yinlin.gradle.*

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibrary,
    )
}

rachel {
    multiplatform {
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        sources {
            commonMain {
                lib(
                    libs.kotlinx.json,
                    ExportLib,
                    projects.ylcsModule.core,
                )
            }
            val clientMain = createClient(commonMain)
            nativeMain(clientMain)
            val jvmMain = createJvm(clientMain)
            androidMain(jvmMain)
            desktopMain(jvmMain)
            iosMain(nativeMain)
            iosMainList(iosMain)
            windowsMain(nativeMain)
            linuxMain(nativeMain)
            macosMain(nativeMain)
            webMain(commonMain)
            jsMain(webMain)
            wasmJsMain(webMain)
        }
    }
}
```

每个项目只能声明一个模板。DSL 先收集目标开关与回调，再创建目标和源集，不需要匿名 `object` 或 `override`。

| 入口 | 对应能力 |
| --- | --- |
| `multiplatform {}` | Android、iOS、Desktop、JS/WasmJS 和按需启用的桌面 Native 目标 |
| `jvm {}` | Kotlin/JVM 模块，可配置 Java application |
| `js {}` | Kotlin/JS 浏览器程序与 webpack |
| `androidApplication {}` | Android 应用与签名回调 |
| `androidLibrary {}` | 保留原 Android NDK 库模板能力，使用独立 `com.android.library` 插件 |
| `native {}` | 桌面 Kotlin/Native 目标与源集 |
| `nativeLibrary {}` | Kotlin/Native shared library |
| `nativeExecutable {}` | Kotlin/Native executable 与运行参数 |

原来的钩子使用接收者 lambda：`sources {}`、`kotlin {}`、`android {}`、`ios {}`、`desktop {}`、`js {}`、`wasmJs {}`、`webpack {}`、`windows {}`、`linux {}`、`macos {}`、`native {}`、`sharedLib {}` 和 `executable {}`。源集使用 `desktopMain(parent) {}`，多源集使用 `iosMainList(parent) {}`；依赖导出仍由 `lib` 参数中的 `ExportLib` 分界。

可选的 Compose、序列化、Android、Dokka 和 Maven 发布插件在 `plugins {}` 中声明。模板本身会应用对应的基础 Kotlin 插件。仓库仍通过 `gradle.properties` 关闭默认层级模板，并显式组织源集依赖；独立使用时也应按自己的层级策略配置。

桌面分发在 `desktopPackage {}` 中设置 `packageName`、`mainClass`，并添加 `windows {}` 等平台块，平台块决定要配置的安装包格式。`actions {}` 保留在项目评估末尾执行，用于依赖 Compose/Android 延迟注册任务的仓库工作流；没有声明该块的模块不会注册空的 `afterEvaluate` 回调。任务执行中的业务操作仍应通过声明的输入、输出和注入服务实现；迁移不意味着任意旧 `doLast { project... }` 都自动兼容配置缓存。

## Native 和代码生成

JVM/KMP 模板保留 `buildDesktopNative` 任务以及 `jar`/`desktopJar` 对它的依赖。源码目录分别为 `src/main/cpp` 与 `src/desktopMain/cpp`；不存在源码或存在 `native.ignore` 时跳过 CMake。Windows 使用 `VS_PATH` 下的 `vcvars64.bat` 和 Ninja，Linux/macOS 使用宿主 CMake 工具链。

Native 任务显式声明源文件、JNI 头文件、编译选项、宿主、构建类型等输入及动态库输出，构建工作目录标记为本地状态。任务执行不读取 `Project.C`，也不会把机器相关的 CMake 结果放入远程构建缓存。

`CopyDesktopNativeTask` 从当前桌面 runtime 依赖图重新选择 `rachelDesktopNativeElements` 产物，纯 JVM 模板使用 `runtimeClasspath`，并保留自身库和预编译库的复制能力。模板在 runtime 与 Native 产物就绪后接入任务输入，避免任务提前创建时访问尚不存在的 configuration。它不会为收集动态库解析 Android、iOS、JS 等全部 configuration。输入文件与 producer 的构建依赖一起传递，复制目标使用同步操作，移除已退出依赖链的旧文件。

```kotlin
rachel {
    multiplatform {
        sources {
            commonMain {
                generateCode("generateExample") {
                    className = "example.Generated"
                    code = "package example\nobject Generated"
                }
            }
        }
    }
}
```

`generateCode` 在所属源集中注册通用的 `GenerateCodeTask`、接入生成目录并绑定编译依赖，无需手动添加 `kotlin.srcDir` 或放到 `actions` 中。任务默认写入当前模块 `build/generated/kotlin`。源码内容和类名是输入，目标文件是输出；类名不接受目录穿越。需要自行组织源码目录时，仍可用 `setupGenerateCode` 注册任务。

生成文件通过 `rachelGeneratedBuildSourcesElements` 暴露为带有任务依赖的产物。当前项目的 IDE model 准备任务自动依赖这些产物；本地仓库约定还根据 settings 中的模块描述符，为根项目聚合所有模块的同类产物。没有生成任务的模块提供空产物，不会触发编译、打包或 Native 构建。这项关联不依赖任何业务模块路径或生成任务名称，根 `build.gradle.kts` 无需配置业务代码生成。

## 发布与仓库外接入

当前构建环境为 JDK 25 / Gradle 9.8，编译使用根版本目录中的 KGP、AGP 和 Compose 插件。对其他版本组合应另行验证。

本地文件 Maven 仓库可用于验证发布内容，不涉及远程发布：

```powershell
.\gradlew.bat -p build-logic :public-plugin:publishAllPublicationsToLocalPluginRepositoryRepository
```

输出位于 `build-logic/public-plugin/build/plugin-repository`。其中包含公开插件 marker 与 `love.yinlin:public-plugin`，版本来自 `versions.rachel`。`repository-conventions` 没有 Maven 发布配置。未来发布到真实 Maven 仓库时，只需为公开插件配置发布地址和凭据；应用签名与 Rachel 库现有发布配置不会用于构建插件发布。

仓库外的最小接入方式：

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven { url = uri("/path/to/plugin-repository") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
rootProject.name = "example"
```

```toml
# gradle/libs.versions.toml
[versions]
rachel = "3.7.1"

[plugins]
rachel = { id = "love.yinlin.rachel", version.ref = "rachel" }
```

```kotlin
// build.gradle.kts
plugins { alias(libs.plugins.rachel) }

rachel {
    defaults {
        namespace = "example"
        jvm { version = 17 }
    }
    jvm {}
}
```

独立项目首次加载公开插件时直接使用 catalog alias；根项目加载完成后，子模块可以使用 `install(libs.plugins.rachel, ...)`。仓库内由本地设置插件提前提供轻量 API，因此连根项目也可以直接使用 `install`。

## 定向验证

```powershell
.\gradlew.bat -p build-logic :public-plugin:validatePlugins :repository-conventions:validatePlugins
.\gradlew.bat help
.\gradlew.bat :ylcs-app:cs:generateConstants --configuration-cache
```

本地文件 Maven 仓库可用独立项目验证插件 marker、依赖解析、DSL 和编译行为。模型核对、配置缓存复用与真实 Native 编译应分别验证。当前 Windows 主机上的结果不能证明 macOS、Linux、iOS 工具链均已验证。
