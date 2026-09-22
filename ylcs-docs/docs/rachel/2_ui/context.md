# 平台上下文

`PlatformContext` 是最小的 `expect` 类型，`PlatformContextProvider` 则在应用与启动服务中保存并补充宿主信息。这个拆分避免共享 API 到处出现 Android `Context` 或 UIKit 类型，同时又允许平台实现拿到真正需要的句柄。

## 各平台含义

| 平台 | `PlatformContext` 的实际含义 |
| --- | --- |
| Android | `android.app.Application` 的 typealias |
| iOS / Desktop / Web / Native | 具有单例 `Instance` 的平台占位上下文 |

在 Android 上刻意使用 Application 而不是 Activity，保证长生命周期服务不会意外泄漏界面。需要 Activity 的能力通过 Provider 的可变宿主字段取得。

## Provider 提供的附加能力

应用和每个 `Startup` 都可以经由 `StartupPool` 访问 `rawContext`。平台 Provider 还补充：

- Android：`contentResolver`、当前 `activity`、`activityResultRegistry`。
- iOS：当前 `UIViewController`。
- Desktop：主窗口的原生 `windowHandle`。
- 其他目标：至少保留 `rawContext`。

这些宿主字段只有在相应 UI 已建立后才可能有值。因此，文件选择器、权限请求或需要窗口句柄的原生库应在 `initLater` 或用户操作时访问，而不是在同步服务构造期间强制解包。

## 在 Compose 中读取

`Application.ComposedLayout` 会提供 `LocalPlatformContext`：

```kotlin
@Composable
fun OpenButton() {
    val context = LocalPlatformContext.current

    PrimaryButton("选择文件") {
        val registry = context.activityResultRegistry
        // 平台能力存在时再执行
    }
}
```

`LocalPlatformContext.current` 的类型是 `PlatformContextProvider`，不是裸 `PlatformContext`。这让公共组件既可以把 `rawContext` 传给跨平台构造器，也可以在平台源集中读取扩展属性。

纯业务逻辑不应从 CompositionLocal 隐式获取上下文。将所需文件、服务或回调作为参数传入，测试会更简单；CompositionLocal 适合 UI 树中天然依附宿主的能力。

## 路径与上下文

平台数据目录需要上下文时，统一从 `PlatformFileSystem` 获取：

```kotlin
val dataPath = PlatformFileSystem.dataPath(rawContext, "my-app")
val cachePath = PlatformFileSystem.cachePath(rawContext, "my-app")
```

这两个目录语义不同：数据目录用于需要保留的配置与内容，缓存目录可以随时删除并重建。不要用 `appPath` 存可写用户数据；在打包应用、移动端或只读安装位置上它可能不可写。

## 文件 URI

系统分享和文件选择返回的不一定是本地路径：

- Android 常见 `content://`，由 `ContentResolver` 授权读取。
- iOS 可能需要沙盒访问语义。
- Desktop 常见普通文件 URI。

共享层使用 `ImplicitUri`、`ContentUri`、`SandboxUri` 和 `RegularUri` 保留区别。只有当 API 明确需要路径且平台实现能够解析时，才转换成 `File`。把 `content://` 的 path 字段直接当绝对路径会绕过权限模型并在真实设备上失败。

## 设计建议

1. 应用级对象保存 `PlatformContextProvider`，不要保存 Activity/ViewController。
2. UI 宿主相关工作延迟到 `initLater` 或交互时。
3. 公共代码依赖能力接口，平台源集完成最后一跳。
4. 对可空宿主句柄设计“暂不可用”的路径，而不是使用 `!!`。

这个模型看似比直接传 Android Context 多一层，但它明确了所有权：应用拥有稳定上下文，窗口/Activity 只是随生命周期附着的临时宿主。
