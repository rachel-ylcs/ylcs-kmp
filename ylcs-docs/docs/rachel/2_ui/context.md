# 平台上下文

`foundation:context` 通过 `PlatformContext` 表示平台基础上下文，通过 `PlatformContextProvider` 提供运行期间的宿主信息。应用与启动服务使用同一套上下文模型。

## 平台映射

| 平台 | `PlatformContext` | Provider 附加能力 |
| --- | --- | --- |
| Android | `android.app.Application` 的类型别名 | `contentResolver`、当前 `activity`、`activityResultRegistry` |
| iOS | 提供 `Instance` 的上下文对象 | 当前 `UIViewController` |
| JVM Desktop | 提供 `Instance` 的上下文对象 | 可空主窗口原生句柄 `windowHandle` |
| Web 与桌面 Native | 提供 `Instance` 的上下文对象 | 基础 `rawContext` 及平台实现提供的扩展 |

Android 的基础上下文为 Application。Activity 通过 Provider 另行提供，可能在启动时尚未建立，或在后续宿主重建时更新。

`windowHandle` 和 iOS `controller` 的写入由平台入口管理，消费者读取时应处理空值。Provider 的存在不保证所有宿主能力同时可用。

## Compose 中的访问

`Application.ComposedLayout()` 提供 `LocalPlatformContext`：

```kotlin
@Composable
fun ContextContent() {
    val provider = LocalPlatformContext.current
    val rawContext = provider.rawContext
}
```

`LocalPlatformContext` 位于 `love.yinlin.compose`。读取结果是 `PlatformContextProvider`，不是裸上下文。Android 的 `activityResultRegistry` 等属性只能在拥有相应声明的平台源集中使用。

组合树之外的业务逻辑应显式接收所需能力，避免将 CompositionLocal 作为隐藏的全局服务入口。

## 启动服务中的访问

`Startup` 保存所属 `StartupPool`，启动池本身继承 `PlatformContextProvider`。服务可通过 `pool.rawContext` 获取基础上下文，并在平台实现中按需读取宿主属性。

需要 Activity、控制器或窗口句柄的操作应安排在合适的生命周期阶段，并提供暂不可用时的处理。`initLater()` 的调用时机见[应用入口](entry.md)，依赖规则见[应用服务](service.md)。

## 平台目录

```kotlin
val dataPath = PlatformFileSystem.dataPath(provider.rawContext, "example")
val cachePath = PlatformFileSystem.cachePath(provider.rawContext, "example")
```

`PlatformFileSystem` 位于 `love.yinlin.fs`。`dataPath`、`cachePath` 与 `appPath` 都是带上下文和应用名参数的函数。

数据目录保存用户配置及需要保留的内容，缓存目录保存可重建数据。应用安装目录或可执行文件位置不应被假定为可写数据目录。获取路径也不代表目录已经创建，调用方需按用途创建目录。

## 文件 URI

系统返回的文件标识可能包含平台访问语义：

- Android 的 `ContentUri` 通过 ContentResolver 访问文档提供器资源。
- iOS 的 `SandboxUri` 保留平台 URL 访问方式。
- `RegularUri` 用于常规地址或路径包装。

这些类型实现 `ImplicitUri`。共享代码应使用相应读写能力，不得将任意 URI 的 path 字段直接视为普通文件绝对路径。

## 生命周期约束

长生命周期对象应保存稳定上下文或能力提供者，界面宿主相关操作应读取当前宿主。持有 Provider 并不能自动消除强引用风险；平台服务仍需按实际生命周期解除监听和释放资源。

## 源码入口

- `ylcs-module/foundation/context/src/commonMain/kotlin/love/yinlin/foundation/PlatformContext.kt`
- `ylcs-module/foundation/context/src/commonMain/kotlin/love/yinlin/foundation/PlatformContextProvider.kt`
- `ylcs-module/compose/core/src/commonMain/kotlin/love/yinlin/compose/LocalPlatformContext.kt`
