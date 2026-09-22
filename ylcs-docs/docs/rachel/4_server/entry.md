# 原生服务端

`love.yinlin.cs:server-engine` 是 Kotlin/Native 上的 Ktor CIO 服务端，不是 JVM 服务。当前模板构建 Windows x64、Linux x64 与 macOS arm64，可与共享 C/S 契约直接编译在一起。

## 最小入口

```kotlin
fun main(args: Array<String>) = runBlocking {
    object : ServerEngine(args) {
        override val port = 1211
        override val public = "public"

        override val plugins: List<BasicServerPlugin> = listOf(
            JsonPlugin
        )

        override val apiScope = AppServerScope(this)
    }.run()
}

class AppServerScope(engine: ServerEngine) : APIScope(engine) {
    override fun api() {
        commonAPI()
        accountAPI()
    }
}
```

`JsonPlugin` 是 POST API 接收/响应 JSON 数组的必要插件。使用 WebSocket 时还要安装 `WebSocketPlugin`。

## 启动目录与参数

命令行只解析 `--key=value` 形式。`--cd=` 指定运行目录；未指定时使用可执行文件相关的 `appPath`：

```text
server.kexe --cd=/srv/ylcs
```

运行目录下约定：

```text
/srv/ylcs/
├─ config.json
├─ public/        # 名称由 public 属性决定
└─ logs/
```

引擎会创建 `logs` 和静态目录。`config.json` 缺失、不可读或 JSON 非法时，当前实现会静默保留空对象，之后仍记录“Config file is loaded”。依赖配置的服务可能继续使用默认值，因此部署前必须独立校验配置文件和密钥，不能把这条日志当成校验成功。

## 启动顺序

`run()` 的实际顺序是：

1. 创建日志目录并读取配置。
2. 创建静态目录。
3. 按 `APIScope.services` 顺序执行 `onStart()`。
4. 执行 `onServerPrepare()`。
5. 创建 CIO Server，按列表安装插件。
6. 注册静态路由、API 与 Socket 路由。
7. 执行 `onServerStart()`，随后阻塞等待服务结束。
8. 执行 `onServerClose()`。
9. 按相反顺序关闭 services，最后关闭 logger。

服务与 Startup 不同：这里没有自动依赖图，顺序就是列表顺序。依赖数据库的准备工作应放在数据库 service 启动之后的 `onServerPrepare`。

## 监听边界

当前引擎把 connector host 固定为：

```text
localhost:<port>
```

因此服务默认只接受本机连接，不能通过配置改成 `0.0.0.0`。生产部署通常让 Nginx/Caddy/平台网关在公网终止 TLS，再反向代理到本机端口；容器部署还要确认 localhost 是否与代理处于同一网络命名空间。

若确实需要直接监听外部接口，必须修改/扩展 ServerEngine 的 connector 配置，并同时规划 TLS、访问控制、请求大小和限流，而不是只改变一个 host 字符串。

## 日志

```kotlin
override val logger = ServerLogger(
    console = true,
    file = File(currentDirectory, "logs"),
    defaultLevel = LogLevel.INFO
)
```

文件日志按本地日期滚动为 `YYYY-MM-DD.log`，写入带锁并立即 flush。请求异常会记录 URI 与堆栈。日志中不要主动拼接令牌、密码、Cookie 或完整上传内容；日志目录也需要外部轮转、容量监控和权限控制。

## 构建与运行

先查看宿主上实际可用的 Native 任务：

```powershell
.\gradlew.bat :ylcs-app:server:tasks --all
```

仓库的 `serverPublish` 依赖 Linux Release 链接任务，用于发布部署产物；开发运行任务会自动传入工作目录，而发布产物应在部署脚本中显式给 `--cd`。

原生可执行文件会链接目标平台依赖，不能把 Windows 产物复制到 Linux 运行。数据库、Redis、TLS 代理和静态资源也不包含在可执行文件中，需要作为部署整体管理。
