# 快速开始

本节说明共享应用的最小结构及平台接入方式。使用前需完成[编译环境](environment.md)配置。

## 声明模块依赖

仓库内模块使用 `rachel { multiplatform {} }` 与源集 DSL。根项目已统一预加载插件版本，模块通过 `install` 声明所需插件：

```kotlin
import love.yinlin.gradle.*

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibrary,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
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

目标开关和插件声明应与实际平台保持一致。`compose:app` 导出启动服务、文件系统和 UI 模块；导航、网络引擎及可选组件应按使用范围增加依赖。

## 定义共享应用

```kotlin
import androidx.compose.runtime.Composable
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.extension.LazyStateReference
import love.yinlin.compose.ui.text.Text
import love.yinlin.foundation.PlatformContext

private val appReference = LazyStateReference<HelloApplication>()
val app: HelloApplication by appReference

open class HelloApplication(
    context: PlatformContext
) : PlatformApplication<HelloApplication>(appReference, context) {
    @Composable
    override fun Content() {
        Text("Hello Rachel")
    }
}
```

`PlatformApplication` 继承 `Application` 与 `StartupPool`，负责共享内容、主题参数和应用级服务组合。平台启动流程会初始化 `appReference`，因此不得在平台入口启动前读取 `app`。引用仅接受首次初始化，同一引用应对应同一个应用实例。

`ComposedLayout()` 安装主题与 `LocalPlatformContext`。标准平台入口会调用该方法，无需在 `Content()` 内重复配置根主题。

## 接入平台入口

Desktop、Web 和 iOS 的上下文提供 `PlatformContext.Instance`。Desktop 与 Web 可直接启动：

```kotlin
fun main() {
    HelloApplication(PlatformContext.Instance).run()
}
```

Android 通过平台宿主建立应用：

```kotlin
class AndroidApplication : ComposeApplication() {
    override fun buildInstance() = HelloApplication(this)
}

class MainActivity : ComposeActivity()
```

上述类型位于 Android 源集，Manifest 需指定对应 application 与 activity。iOS 由 Swift 宿主持有同一个 Kotlin 应用实例，先调用 `run()`，再通过 `buildUIViewController()` 创建界面控制器。各平台完整生命周期见[应用入口](../2_ui/entry.md)。

## 注册启动任务

一次性任务在应用构造阶段注册，依赖通过任务 ID 声明：

```kotlin
private val prepareDirectories by async(id = "prepareDirectories") {
    val cachePath = PlatformFileSystem.cachePath(rawContext, "hello")
    cachePath.mkdir()
}

private val prepareSession by async(
    id = "prepareSession",
    dependencies = ["prepareDirectories"]
) {
    // 在目录准备任务完成后执行会话初始化。
}
```

`PlatformFileSystem` 位于 `love.yinlin.fs`。`async` 注册任务，不在声明位置立即执行任务内容。启动池在初始化时解析依赖；同步服务不得依赖异步服务。复用服务的定义方式见[应用服务](../2_ui/service.md)。

## 添加页面导航

根内容可替换为导航宿主：

```kotlin
@Composable
override fun Content() {
    ScreenManager.Navigation<ScreenHome> {
        screen(::ScreenHome)
        screen(::ScreenDetails)
    }
}

class ScreenHome : Screen() {
    override val title = "首页"

    @Composable
    override fun Content() {
        PrimaryButton("查看详情") {
            navigate(::ScreenDetails, "demo")
        }
    }
}

class ScreenDetails(private val id: String) : Screen() {
    override val title = "详情"

    @Composable
    override fun Content() {
        Text(id)
    }
}
```

导航类型位于 `love.yinlin.compose.screen`，按钮位于 `love.yinlin.compose.ui.input`。根页面应能以零参数创建，并显式注册。页面参数、导航策略及状态恢复边界见[导航与页面](../2_ui/navigation.md)。

## 示例运行

```powershell
.\gradlew.bat :ylcs-app:gallery:galleryRun
.\gradlew.bat :ylcs-app:modern-screen:modernScreenRun
```

前者展示组件用法，后者展示导航行为。以上命令用于开发者本地验证，不代表文档示例已通过所有平台的编译与运行验证。
