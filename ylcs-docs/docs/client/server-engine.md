---
description: 使用 Kotlin/Native 与 Ktor CIO 构建原生服务端，注册接口、资源、文件与插件。
---

# 服务端引擎

<p class="rc-lead"><code>cs:server-engine</code> 基于 Kotlin/Native 与 Ktor CIO，不采用 JVM 服务进程。它把共享契约注册为路由，并把处理结果映射为稳定的 HTTP 协议。</p>

## 最小入口

```kotlin title="Main.kt"
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

`JsonPlugin` 配置 JSON 请求与响应。使用 WebSocket 时还需安装 `WebSocketPlugin`。

!!! warning "监听地址"
    引擎 connector 当前固定监听 `localhost:<port>`，没有公共 host 配置。公网访问通常通过部署层的反向代理转发，TLS 与外部访问边界由部署环境提供。

## 目录与配置

命令行解析 `--key=value` 形式的参数。`--cd` 指定引擎 `currentDirectory`：

```text title="启动参数"
server.kexe --cd=/srv/example
```

| 路径 | 内容 |
| --- | --- |
| `<currentDirectory>/config.json` | 服务端配置 |
| `<currentDirectory>/logs` | 文件日志目录 |
| `<currentDirectory>/<public>` | 静态资源目录 |

`--cd` 设置的是引擎路径属性，不调用进程级 `setCurrentDirectory`。业务代码使用相对文件路径时，需要确认进程工作目录与引擎目录的关系。

配置读取异常由 `catching` 忽略，失败时保留初始空 JSON 对象。部署启动逻辑应显式检查必要配置，不能仅凭日志判断配置有效。

## 启动顺序

1. 创建日志目录并读取配置；
2. 创建静态目录；
3. 按 `APIScope.services` 顺序启动服务；
4. 调用 `onServerPrepare()`；
5. 创建 CIO 服务并安装插件；
6. 注册静态路由与 `api()` 中的路由；
7. 调用 `onServerStart()`，等待服务结束；
8. 正常返回后调用 `onServerClose()`，逆序关闭服务并关闭日志器。

启动或关闭异常可能中断后续清理。整个流程没有统一 `try/finally` 包裹，自定义服务应明确部分初始化后的资源处理。

## 注册接口

```kotlin title="APIScope"
class DemoScope(engine: ServerEngine) : APIScope(engine) {
    override fun api() {
        demoAPI()
    }
}

fun DemoScope.demoAPI() {
    ApiDemoEcho.response { text ->
        if (text.isBlank()) failure("内容不能为空")
        result(text)
    }
}
```

领域注册函数应由 `api()` 调用，确保注册时路由宿主已经建立。有输出时使用 `APIResultScopeN` 并返回 `result(...)`；无输出时正常结束后响应空数组。

## 响应协议

| 处理结果 | HTTP 状态 | 响应形式 |
| --- | --- | --- |
| 正常 | 200 | JSON 输出数组 |
| `failure(message)` / `FailureException` | 202 | 错误文本 |
| `expire()` / `UnauthorizedException` | 401 | 空 JSON 数组 |
| 其他普通异常 | 403 | 空 JSON 数组 |

```kotlin title="失败与鉴权"
val userId = validateToken(token) ?: expire()
if (!canEdit(userId, itemId)) failure("无权修改该条目")
result(saveItem(itemId))
```

403 是当前引擎的未分类异常协议，不足以推断真实故障原因。`CancellationException` 不属于普通错误分支，会继续传播。

## 参数校验

共享类型只约束传输结构。业务处理仍需检查：

- 内容长度、数值范围与格式；
- 资源存在性、归属与权限；
- 重复操作语义与幂等性；
- 文件数量、大小与内容类型。

POST 解析器按位置读取所需参数，但不应依赖“额外元素一定被忽略”。客户端与服务端都应严格按声明发送与接收。

## 文件上传

```kotlin title="multipart"
ApiDemoUpload.response { requestedId, upload ->
    if (!requestedId.matches(Regex("[A-Za-z0-9_-]+"))) failure("标识格式不正确")

    val file = upload ?: failure("未选择文件")
    if (file.num != 1) failure("文件数量不正确")

    val directory = File(engine.currentDirectory, "uploads")
    directory.mkdir()

    val target = APIRes(APIRes(directory.path), "$requestedId.bin")
    validateContent(file[0])
    file[0].copy(target)
    result(requestedId)
}
```

| 规则 | 行为 |
| --- | --- |
| part 名称 | 从零开始、连续的十进制索引 |
| 同索引一个 FormItem | 解析为 JSON |
| 同索引多个 FileItem | 按顺序组成一个文件参数 |
| JSON `null` | 仅在声明允许空值时恢复为 `null` |
| 重复 FormItem | 拒绝 |
| JSON 与文件混用 | 拒绝 |
| 零字节文件 | 拒绝 |

临时文件在请求处理作用域退出时删除。需要持久保存时，应在处理器返回前复制到业务目录。

框架层没有统一的请求总大小、单文件大小与文件数量上限；限制应在入口与业务校验层明确。

## 静态资源

`ServerEngine.public` 决定静态目录名称与路由前缀：

```text title="静态路径"
本地目录：<currentDirectory>/public
访问地址：GET /public/assets/logo.webp
```

当前实现：

- 只处理 GET；
- 目录请求尝试读取 `index.html`；
- 根据扩展名选择 Content-Type，未知类型使用 `application/octet-stream`；
- 通过流式响应并提供文件长度；
- 文件不存在或长度为零时返回 404。

封装未提供目录列表、ETag、Range、压缩与显式 Cache-Control。需要这些能力时由部署层或独立路由实现。静态目录按公开内容处理，鉴权资源应放在其他目录。

## 服务

`ServerService` 管理与服务端同生命周期的连接池与客户端：

```kotlin title="服务列表"
class DemoScope(engine: ServerEngine) : APIScope(engine) {
    val mysql = MysqlService(this)
    val redis = RedisService(this)

    override val services: List<ServerService> = [mysql, redis]
}
```

启动按列表顺序执行 `onStart()`，正常退出后按逆序执行 `onClose()`。该机制不使用客户端 `StartupPool` 的依赖图，也不提供自动并行启动与依赖环检查。

MySQL 与 Redis 的配置、事务与边界见[数据与实时](data-realtime.md)。

## 插件

`ServerEngine` 在注册路由前按 `plugins` 列表安装 Ktor 应用插件：

| 插件 | 作用 |
| --- | --- |
| `JsonPlugin` | ContentNegotiation 与框架 JSON |
| `WebSocketPlugin` | ping、超时、帧大小与 masking |
| `IPLogPlugin` | 读取 `X-Real-IP` 并记录 |

安装顺序由列表决定。插件异常不一定进入 API 的 202 / 401 / 403 映射，不能假定所有错误都会转换为统一响应。

## 日志

`ServerLogger.Default` 输出控制台日志，不启用文件输出。需要文件日志时显式配置：

```kotlin title="文件日志"
override val logger = ServerLogger(
    console = true,
    file = File(currentDirectory, "logs"),
    defaultLevel = LogLevel.INFO,
)
```

记录请求错误时保留定位信息，同时避免记录密码、令牌与完整隐私请求体。

## 构建与运行

```powershell title="服务端任务"
.\gradlew.bat :ylcs-app:server:serverRunDebug
.\gradlew.bat :ylcs-app:server:serverRunRelease
```

`serverPublish` 复制目标平台的 Release 可执行文件。数据库、Redis、静态资源与反向代理属于部署依赖，不包含在单个可执行文件中。

## 下一步

继续阅读[数据与实时](data-realtime.md)，了解 WebSocket、回调、MySQL、Redis 与代理现状。
