---
description: 理解 Rachel 的显式源集层级、依赖导出、公共 API 与代码约定。
---

# 源集与 API

<p class="rc-lead">Rachel 关闭了 Kotlin 默认层级模板，所有源集继承都显式声明。这样做的代价是配置更长，收益是每个模块的平台边界都可以被审查，而不是依赖目录名猜测。</p>

## 源集层级

```text title="典型层级"
commonMain
├─ clientMain
│  ├─ jvmMain
│  │  ├─ androidMain
│  │  └─ desktopMain
│  ├─ appleMain
│  │  └─ iosMain
│  ├─ webMain
│  │  ├─ jsMain
│  │  └─ wasmJsMain
│  └─ skikoMain
└─ nativeMain
   └─ posixMain
```

| 源集 | 典型职责 |
| --- | --- |
| `commonMain` | 所有启用目标共享的模型、状态与逻辑 |
| `clientMain` | 客户端共享，排除服务端 |
| `jvmMain` | Android 与 JVM Desktop 共享实现 |
| `webMain` | JS 与 WasmJS 共享实现 |
| `nativeMain` | Kotlin/Native 共享实现 |
| `posixMain` | POSIX 平台共享实现 |
| `appleMain`、`iosMain` | Apple / iOS 共享实现 |
| `skikoMain` | 依赖 Skiko 的共享实现 |
| `androidMain`、`desktopMain` | 平台专属入口与系统 API |

同名源集在不同模块中可能具有不同继承关系。以模块的 `sources { }` 为准。

## 依赖导出

```kotlin title="ExportLib"
commonMain {
    lib(
        projects.ylcsModule.core,
        ExportLib,
        projects.ylcsModule.compose.app,
    )
}
```

| 位置 | 语义 | 消费者 |
| --- | --- | --- |
| `ExportLib` 之前 | 实现依赖 | 不可见 |
| `ExportLib` 之后 | API 依赖 | 随公开签名传递 |

判断方法：如果公开函数、类、属性签名中出现了某个库的类型，该库通常需要位于 `ExportLib` 之后。否则消费者在编译或链接时会缺少类型。

!!! warning "导出不是越多越好"
    导出会扩大消费者的依赖面，影响编译时间与版本冲突。只导出公开 API 真正需要的依赖。

## `expect` / `actual`

```kotlin title="commonMain"
expect class PlatformClock() {
    fun now(): Long
}
```

```kotlin title="androidMain"
actual class PlatformClock actual constructor() {
    actual fun now(): Long = System.currentTimeMillis()
}
```

规则：

- 修改 `expect` 时同步所有已启用的 `actual`；
- 平台类型不要出现在 `commonMain` 的公开签名中；
- 能用接口或能力提供者表达时，优先使用接口，减少 `expect/actual` 数量；
- 平台实现应处理权限、空值与生命周期，而不是只完成编译。

## 公共 API 设计

### 稳定性

- 已发布的类名、函数签名、默认行为与序列化格式属于兼容面；
- 破坏性变更需要迁移说明或新增 API；
- 不要在没有消费者的情况下提前抽象；
- 内部实现应使用 `internal`，避免无意扩大 API。

### 资源所有权

公共 API 应明确：

| 问题 | 需要在 KDoc 中说明 |
| --- | --- |
| 谁创建资源 | 调用方还是库 |
| 谁关闭资源 | 显式关闭还是作用域关闭 |
| 取消语义 | 是否传播 `CancellationException` |
| 失败语义 | 返回 `null`、异常还是结果类型 |
| 线程约束 | 是否要求主线程或特定上下文 |

!!! tip "语义不明显的 API 需要 KDoc"
    说明边界、异常与资源所有权，而不是复述函数名。注释应解释“为什么”和“何时”，不是“做了什么”。

## 资源组织

| 资源 | 位置 | 注意 |
| --- | --- | --- |
| Compose 资源 | `src/commonMain/composeResources` | 通过生成的 `Res` 访问 |
| Android 资源 | `androidMain/res` | 平台专属 |
| iOS 资源 | Xcode 资源目录 | 与 Swift 宿主协调 |
| 原生库 | 模块构建配置与分发目录 | 不提交构建产物 |
| 文档图片 | `ylcs-docs/docs/assets` | 仅保留公开可用的素材 |

资源路径、缓存目录与用户数据目录是不同概念。不要把安装目录当作可写数据目录。

## 代码约定

仓库使用统一的 Kotlin 风格与框架语法糖：

| 场景 | 约定 |
| --- | --- |
| 集合字面量 | 使用 `[]` 替代 `emptyList`、`listOf` 等 |
| 结构化绑定 | 基于位置用 `[]`，基于名称用 `()` |
| 范围 | 使用 `..` 与 `..<`，左右留空格 |
| 缩进 | 四空格 |
| 语句 | 不使用分号合并语句 |
| 异常 | 使用 `Catching` 系列 |
| 枚举 | 使用 `enumEx` 系列 |
| JSON | 使用 `JsonEx`、`SerializerEx` |
| 日期 | 使用 `DateEx` |
| 字节 | 使用 `ByteArrayIO` |
| 状态 | 使用 `love.yinlin.compose.extension` 的状态工厂 |

`@Stable` 优先于 `@Immutable`。数据类与用于 UI 显示的类型应考虑稳定性注解。

Lint 细节以 `ylcs-module/lint` 中的 `RepositoryKtlint` 规则为准。框架模块遵循最新 Kotlin 语法；`build-logic` 与 `native-compose` 为兼容上游可能使用旧版语法，不能把新语法直接套用到这些目录。

## 模块边界检查

- [ ] 框架模块没有依赖 `ylcs-app`；
- [ ] `commonMain` 没有引用平台专属 API；
- [ ] 平台依赖没有放进错误源集；
- [ ] 公开类型所需的依赖已经导出；
- [ ] 新增资源路径已同步构建与分发；
- [ ] 平台实现处理了权限、空值与生命周期；
- [ ] 没有把业务常量写入公共库。

## 下一步

继续阅读[数据处理](data.md)，了解字节、压缩、加密、检索与解析器的深层约束。
