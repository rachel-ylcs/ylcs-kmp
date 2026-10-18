# 原生服务端引擎

`cs:server-engine` 基于 Kotlin/Native 与 Ktor CIO。当前服务端应用配置 Windows x64、Linux x64 与 macOS arm64 目标，不采用 JVM 服务进程。

## 最小入口

以下共享声明同时供客户端与服务端依赖：

```kotlin
val ApiDemoEcho by API.post.i<String>().o<String>()
```

原生服务端入口：

```kotlin
import kotlinx.coroutines.runBlocking
import love.yinlin.cs.*
import love.yinlin.cs.plugin.JsonPlugin

class DemoScope(engine: ServerEngine) : APIScope(engine) {
    override fun api() {
        ApiDemoEcho.response { text ->
            result(text)
        }
    }
}

fun main(args: Array<String>) = runBlocking {
    object : ServerEngine(args) {
        override val port = 1211
        override val public = "public"
        override val plugins: List<BasicServerPlugin> = [JsonPlugin]
        override val apiScope = DemoScope(this)
    }.run()
}
```

`JsonPlugin` 配置 JSON 请求与响应。使用 WebSocket 时还需安装 `WebSocketPlugin`，见[插件](plugin.md)。

## 配置目录

命令行解析 `--key=value` 形式的参数。`--cd` 指定引擎 `currentDirectory`，缺省时通过 `PlatformFileSystem.appPath` 获取：

```text
server.kexe --cd=/srv/example
```

配置文件为 `<currentDirectory>/config.json`，日志目录为 `<currentDirectory>/logs`，静态目录为 `<currentDirectory>/<public>`。

`--cd` 设置的是引擎路径属性，当前 `ServerEngine` 不据此调用 `setCurrentDirectory`。业务代码使用相对文件路径时，仍需确认进程工作目录与引擎目录的关系。

配置读取异常由 `catching` 忽略，失败时保留初始空 JSON 对象，后续仍输出配置加载日志。部署与启动准备逻辑应显式检查必要配置，不能仅以该日志判断配置有效。

## 启动顺序

1. 创建日志目录并读取配置。
2. 创建静态目录。
3. 按 `APIScope.services` 顺序启动服务。
4. 调用 `onServerPrepare()`。
5. 创建 CIO 服务并按列表安装插件。
6. 注册静态路由及 `APIScope.api()` 中的路由。
7. 在应用模块初始化中调用 `onServerStart()`，由 `.start(wait = true)` 等待服务结束。
8. 正常返回后调用 `onServerClose()`，逆序关闭服务，最后关闭日志器。

`onServerStart()` 的位置不等同于独立的端口就绪通知。整个运行流程未由统一 `try/finally` 包裹，启动或关闭异常可能中断后续清理。

## 监听地址

引擎 connector 当前固定为 `localhost:<port>`，没有公共 host 配置项。公网访问通常通过部署层的反向代理转发，TLS 和外部访问边界由部署环境提供。

容器或多进程部署需要同时检查网络命名空间与代理转发目标。改变 `port` 不会改变监听接口。

## 日志

`ServerLogger.Default` 输出控制台日志，不启用文件输出。需要文件日志时显式配置：

```kotlin
override val logger = ServerLogger(
    console = true,
    file = File(currentDirectory, "logs"),
    defaultLevel = LogLevel.INFO
)
```

文件日志按日期切换文件，并在写入后刷新。记录请求错误时应保留定位信息，同时避免记录密码、令牌及完整隐私请求体。

## 构建任务

```powershell
.\gradlew.bat :ylcs-app:server:tasks --all
```

`serverPublish` 明确依赖 Linux Release 链接任务 `linkReleaseExecutableLinux`，用于生成部署产物。开发任务的参数由 `ylcs-app/server/build.gradle.kts` 设置，当前会在非发布场景追加 `--cd`。

可执行文件需与目标操作系统及架构匹配。数据库、Redis、静态资源和外部代理属于部署依赖，不包含在单个可执行文件中。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/ServerEngine.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/ServerLogger.kt`
- `ylcs-app/server/build.gradle.kts`
