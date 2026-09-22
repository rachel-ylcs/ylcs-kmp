# C/S 共享契约

`love.yinlin.cs:core` 定义客户端与服务端共同编译的协议。一个 API 声明同时确定路由、传输形式、输入类型和输出类型；客户端引擎为它提供 `request`，服务端引擎为同一个对象提供 `response`。

## 声明 API

普通请求使用 `API.post`，文件表单使用 `API.form`：

```kotlin
val ApiCommonGetServerStatus by
    API.post.i().o<ServerStatus>()

val ApiAccountLogin by
    API.post.i<String, String, Platform>().o<String>()

val ApiActivityUpdatePhoto by
    API.form.i<String, Int, APIFile>().o<String>()
```

`i<...>()` 是输入列表，`o<...>()` 是输出列表，当前各支持 0 到 5 个元素。`form` 至少需要一个输入；其中普通值与 `APIFile` 可以混排，并由参数位置保持对应关系。

输入输出类型必须能被共享序列化规则处理。二进制内容不要塞进 JSON；使用 `APIFile` 与 multipart。

## 路由由名字生成

委托属性名遵循 `Api<分组><动作>`：

```text
ApiAccountLogin        -> /account/login
ApiCommonGetServerStatus -> /common/getServerStatus
```

生成规则把第一个大写片段作为一级路径，其余部分只把首字母改为小写。属性名因此是线协议的一部分，重命名会改变路由。若已经发布客户端，不要把普通重构工具对 API 属性的重命名当作无损操作；应保留旧声明或做服务端兼容迁移。

不符合 `ApiXxxYyy` 结构的名字不会得到期望路由，所以协议声明应集中放置并接受测试。

## 线格式

POST 请求和成功响应都使用 JSON 数组，位置即含义：

```json
["account", "password", "Android"]
```

单个输出仍是数组：

```json
["token-value"]
```

这样做让同一套生成的 `API10`、`API31`、`API55` 等接口覆盖固定元数的调用，并能在编译期解构结果；代价是不能随意交换字段顺序。对外暴露的长期协议如果需要命名字段与独立演进，可把一个 `@Serializable` DTO 作为单个输入或输出。

表单请求使用参数索引作为 part 名称。多文件参数会扩展为 `索引:子索引`；可空文件缺失时仍发送占位 part，确保后续参数不移位。不要在自定义客户端中擅自改成字段名，否则当前服务端解析器无法对应。

## 文档注解

共享层提供：

- `@APIDoc`：接口说明。
- `@APIParam`：输入参数说明。
- `@APIReturn`：返回值说明。

这些注解保留协议意图，但不会代替运行时校验。服务端仍需校验令牌、范围、长度和业务约束；客户端应把错误展示与重试策略放在调用边界。

## 资源与文件

`APIRes` 用类型构造静态资源层级，默认路径名来自类名的小写形式。它既可在客户端生成 URL，也可在服务端映射到资源文件：

```kotlin
object UserAvatar : APIRes("avatar")
```

`APIFile` 是上传文件在共享契约中的标记。客户端可以从路径、字节、Source 等构造上传内容；服务端得到的是缓存区中的文件句柄。它不表示静态资源，也不自动承诺文件在请求结束后永久存在。

## WebSocket 契约

WebSocket 只共享路径和标题：

```kotlin
object LyricsSockets : Sockets(
    path = "/lyricsGame",
    title = "歌词默写"
)
```

消息模型由应用自己定义，通常使用密封的 `@Serializable` 客户端消息和服务端消息。核心层不替你做版本协商、心跳、重连或消息确认；这些应由业务协议明确实现。

## 契约模块的边界

共享 C/S 模块只应包含：

- API、资源和 Socket 声明；
- 可序列化 DTO 与枚举；
- 客户端和服务端都需要的纯规则。

不要在这里引用 Compose 状态、数据库连接或平台文件 API。保持协议层纯净，服务端 Native 与所有客户端目标才能真正使用同一份类型。

下一步可分别阅读[客户端请求](../3_client/request.md)与[服务端请求](../4_server/request.md)，查看同一个声明在两端如何落地。
