---
description: 使用 Rachel 的模块模板、源集 DSL 与依赖导出规则建立应用模块。
---

# 模块与依赖

<p class="rc-lead">Rachel 的模块配置不是普通的 Kotlin 源集堆叠，而是一套有明确边界的构建描述：模板决定目标平台，<code>sources</code> 决定源集继承，<code>lib</code> 决定依赖是内部实现还是对外 API。</p>

## 一个应用模块的最小脚本

下面的脚本展示一个共享 Compose 应用模块的完整骨架。插件通过根构建脚本预加载，模块只声明需要安装的插件。

```kotlin title="build.gradle.kts"
import love.yinlin.gradle.*

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibrary,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.kotlinSerialization,
    )
}

rachel {
    multiplatform {
        sources {
            commonMain {
                lib(
                    projects.ylcsModule.compose.app,
                    projects.ylcsModule.compose.screen,
                )
            }

            androidMain(commonMain)
            iosMain(commonMain)
            iosMainList(iosMain)
            desktopMain(commonMain)
            webMain(commonMain)
            jsMain(webMain)
            wasmJsMain(webMain)
        }
    }
}
```

| 部分 | 作用 |
| --- | --- |
| `install(...)` | 安装 Rachel、Kotlin、Android、Compose 与序列化插件 |
| `multiplatform { }` | 选择一个 Rachel 模板并配置目标 |
| `sources { }` | 显式声明源集继承关系 |
| `lib(...)` | 添加依赖，区分实现与导出 |

!!! warning "一个模块只能选择一种 Rachel 模板"
    重复声明会由构建扩展拒绝。需要同时构建 JVM 与桌面 Native 的模块，应使用一个 `multiplatform` 模板并开启相应目标。

## 源集：显式建立继承

仓库关闭了 Kotlin 默认层级模板。`commonMain` 不会自动成为 `androidMain`、`iosMain` 或 `desktopMain` 的父源集，必须显式声明：

```kotlin title="源集关系"
sources {
    commonMain { /* ... */ }

    clientMain(commonMain)
    nativeMain(commonMain)
    posixMain(nativeMain)

    androidMain(commonMain)
    iosMain(commonMain)
    desktopMain(commonMain)

    webMain(commonMain)
    jsMain(webMain)
    wasmJsMain(webMain)
}
```

常见源集职责：

| 源集 | 典型内容 |
| --- | --- |
| `commonMain` | 所有已启用目标共享的模型、状态与逻辑 |
| `clientMain` | 客户端共享、排除服务端的实现 |
| `jvmMain` | Android 与 JVM Desktop 共享实现 |
| `webMain` | JS 与 WasmJS 共享实现 |
| `nativeMain`、`posixMain` | Kotlin/Native 与 POSIX 能力 |
| `skikoMain` | 依赖 Skiko 的共享实现 |
| `androidMain`、`iosMain`、`desktopMain` | 平台专属入口与系统 API |

!!! tip "先找最上层可用源集"
    能放在 `commonMain` 的逻辑不要下沉到平台源集；确实需要平台 API 时，优先使用 `expect/actual` 收敛到最小边界，而不是在每个平台复制整段业务。

## 依赖：实现与导出

`lib(...)` 中的依赖顺序有语义：

```kotlin title="导出边界"
commonMain {
    lib(
        projects.ylcsModule.core,          // 实现依赖
        ExportLib,
        projects.ylcsModule.compose.app,   // 对外导出的 API 依赖
    )
}
```

| 位置 | 含义 | 消费者可见性 |
| --- | --- | --- |
| `ExportLib` 之前 | 仅当前模块使用的实现依赖 | 不暴露 |
| `ExportLib` 之后 | 出现在公开签名中的 API 依赖 | 传递给消费者 |

如果公开类型引用了某个库的类型，却没有导出该库，消费者会在编译或链接时缺少类型。调整顺序前先检查公开 API。

## 使用项目依赖与版本目录

仓库内部优先使用类型安全的 `projects.*`：

```kotlin title="项目依赖"
commonMain {
    lib(
        projects.ylcsModule.compose.theme,
        projects.ylcsModule.compose.ui,
        projects.ylcsModule.cs.core,
    )
}
```

外部依赖统一通过版本目录引用：

```kotlin title="版本目录"
commonMain {
    lib(
        libs.ktor.client.core,
        libs.kotlinx.coroutines.core,
        libs.kotlinx.serialization.json,
    )
}
```

不要在各模块中硬编码版本字符串；版本升级应集中在 `libs.versions.toml`。

## 从一个小模块开始

如果你只是验证共享界面，可以先依赖 `compose:app`。它导出启动服务、文件系统与基础 UI 能力，适合最小应用：

```kotlin title="最小依赖"
commonMain {
    lib(projects.ylcsModule.compose.app)
}
```

当你需要导航、网络、图片或媒体时，再按需增加模块：

| 需求 | 模块 |
| --- | --- |
| 页面与导航 | `compose:screen` |
| 主题与基础组件 | `compose:theme`、`compose:ui` |
| 图片加载与缓存 | `compose:components:image` |
| 客户端请求 | `cs:client-engine` |
| 原生服务端 | `cs:server-engine` |
| 平台原生视图 | `compose:platform-view` |

## 注册新模块

新增 Gradle 模块时，需要同时完成：

1. 在 `settings.gradle.kts` 的 `modules { }` 中注册模块路径；
2. 创建模块的 `build.gradle.kts`，选择 Rachel 模板并声明目标；
3. 通过 `sources` 建立源集关系；
4. 通过 `lib` 与 `ExportLib` 声明依赖边界；
5. 如需发布，检查发布坐标与产物配置；
6. 如需测试，确认模块已启用对应目标并接入测试聚合任务。

完整构建约定见[模块 DSL](../build/dsl.md)。

## 下一步

依赖声明完成后，进入[编写第一个应用](first-app.md)，把模块变成一个真正可运行的共享应用。
