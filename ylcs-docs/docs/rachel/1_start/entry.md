# 快速开始

这一页建立一个最小 Rachel 应用，并说明真正需要理解的三个对象：应用实例、平台入口和启动服务。项目本身是大型多模块工程；新功能更适合从已有应用模块复制骨架，而不是把全部模块一次性引入。

## 选择依赖

在仓库内开发时，优先使用类型安全的项目引用：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.ylcsModule.compose.app)
            implementation(projects.ylcsModule.compose.screen)
            implementation(projects.ylcsModule.compose.ui)
        }
    }
}
```

发布坐标由模块路径生成，版本基线为 `3.7.0`。例如已发布到所配置仓库时，可使用：

```kotlin
commonMain.dependencies {
    implementation("love.yinlin.compose:app:3.7.0")
    implementation("love.yinlin.compose:screen:3.7.0")
    implementation("love.yinlin.compose:ui:3.7.0")
}
```

`compose:app` 导出了启动、文件系统、UI 与原生库加载等基础依赖，但没有替你引入所有可选组件和 C/S 引擎。显式列出直接使用的模块，可以避免因为传递依赖偶然存在而让构建变得脆弱。

## 定义共享应用

应用类负责三件事：持有启动池、提供全局主题入口、渲染根内容。

```kotlin
import androidx.compose.runtime.Composable
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.extension.LazyStateReference
import love.yinlin.compose.ui.text.Text
import love.yinlin.foundation.PlatformContext

private val appReference = LazyStateReference<HelloApplication>()
val app by appReference

class HelloApplication(
    context: PlatformContext
) : PlatformApplication<HelloApplication>(appReference, context) {
    @Composable
    override fun Content() {
        Text("Hello Rachel")
    }
}
```

这里的自引用不是装饰：`Application` 在平台启动时才创建，但共享代码希望通过 `app` 访问同一个实例。`LazyStateReference` 在初始化后还会通知 Compose；如果调用方不需要观察初始化状态，普通 `LazyReference` 更轻。

不要在模块加载阶段读取 `app`。只有平台入口调用 `run()`，或 Android `Application.onCreate()` 完成初始化后，它才有值。

## 接入平台入口

各平台负责提供 `PlatformContext`，并在正确的生命周期阶段触发两段初始化：

- `initApplication`：设置全局实例、解析启动依赖、立即创建同步服务并启动异步服务。
- `initPoolLater`：等 UI 宿主可用后再执行第二阶段初始化。

完整入口写法见[应用入口](../2_ui/entry.md)。最短的 Desktop 入口是：

```kotlin
fun main() {
    HelloApplication(PlatformContext.Instance).run()
}
```

Web 也调用 `run()`；iOS 先调用 `run()`，再把 `buildUIViewController()` 返回给 SwiftUI/UIKit；Android 则由 `ComposeApplication` 和 `ComposeActivity` 承接生命周期，不能在 `main` 中直接启动。

## 加入第一个启动服务

小型一次性初始化可以直接声明在应用类中：

```kotlin
private val prepareDirectories by async(id = "prepareDirectories") {
    PlatformFileSystem.cachePath.mkdir()
}

private val prepareSession by async(
    id = "prepareSession",
    dependencies = listOf("prepareDirectories")
) {
    // 这里开始时，prepareDirectories 已成功完成
}
```

可复用服务应实现 `Startup` 并提供自己的 `StartupFactory`。启动池会检查不存在的依赖和依赖环；同步服务不能依赖异步服务。详细规则见[应用服务](../2_ui/service.md)。

## 建议的第一个验证

仓库已经有一个跨 Desktop/Wasm 的组件展厅，可用它验证本机工具链：

```powershell
.\gradlew.bat :ylcs-app:gallery:galleryRun
```

若只想确认工程配置而不运行 UI：

```powershell
.\gradlew.bat projects
.\gradlew.bat :ylcs-app:gallery:tasks --all
```

## 下一步

- 需要多页面应用：阅读[导航与页面](../2_ui/navigation.md)。
- 需要主题和响应式布局：阅读[主题系统](../2_ui/theme.md)。
- 需要访问 HTTP：简单请求读[通用网络](../2_ui/network.md)，类型化后端读[客户端引擎](../3_client/entry.md)。
- 需要本地持久化、缓存或选择器：阅读[应用服务](../2_ui/service.md)。

不要把所有能力都塞进应用单例。应用单例适合组合服务和提供跨模块入口；页面状态属于 `Screen`，可独立复用的长生命周期能力属于 `Startup`，一次性的 UI 状态留在组合中。
