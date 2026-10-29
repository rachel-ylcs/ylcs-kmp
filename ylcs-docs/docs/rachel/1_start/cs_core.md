# C/S 共享契约

`ylcs-module/cs/core` 定义客户端与服务端共用的 API、资源路径、文件参数和 WebSocket 入口。客户端引擎实现请求编码，服务端引擎实现参数解码与响应。

## API 声明

```kotlin
val ApiDemoEcho by API.post.i<String>().o<String>()
val ApiDemoStatus by API.post.i().o<String, Long>()
val ApiDemoUpload by API.form.i<String, APIFile?>().o<String>()
```

`API.post` 支持 0 至 5 个输入，`API.form` 支持 1 至 5 个输入；两者均支持 0 至 5 个输出。`i<...>()` 和 `o<...>()` 按顺序声明类型，无输入或输出时使用无泛型版本。

普通参数和返回值需支持共享序列化。上传内容使用 `APIFile` 标记，由客户端和服务端分别提供运行时包装。

## 路由生成

API 委托按属性名生成路由：

```text
ApiDemoEcho              → /demo/echo
ApiCommonGetServerStatus → /common/getServerStatus
```

规则由 `APIDelegate` 中的正则表达式实现。属性名应符合 `Api<分组><操作>` 的命名形式；不匹配时不会自动生成预期的分组路径。

API 属性名属于协议的一部分。已发布接口的重命名需要兼容旧路由，输入输出位置与类型的调整也需同步评估旧客户端。

## POST 线格式

普通请求使用 JSON 数组，以位置对应声明参数。单参数和单输出仍使用数组：

```json
["example"]
```

多个输出示例：

```json
["ready", 123]
```

`APIResult1` 至 `APIResult5` 表示结果，其属性为 `o1` 至 `o5`。无输出的接口以空数组响应，客户端结果为 `Unit`。

框架依次读取所需位置并按声明类型解码。缺少必要位置或类型不匹配会失败；实现并未为所有 POST 情形统一检查数组长度完全相等。协议仍应严格按声明发送，不能依赖额外元素被忽略的行为。

## multipart 线格式

表单参数以从 `0` 开始的十进制索引作为 part 名称：

| 参数类型 | 传输形式 |
| --- | --- |
| 普通值 | 一个同索引的 JSON 文本 FormItem |
| 单文件 | 一个同索引的 FileItem |
| 多文件 | 多个名称相同的 FileItem，按接收顺序组成一个文件参数 |
| 可空文件为 `null` | 一个同索引、内容为 JSON `null` 的 FormItem |

例如第二个参数包含两个文件时，两个 FileItem 的名称均为 `1`。当前实现不使用 `1:0`、`1:1` 或 `#1` 等旧编码。第三方客户端必须与当前两端实现保持一致。

客户端上传请求接管并关闭纳入表单的 `RawSource`。服务端临时文件由请求解析器创建，并在表单处理作用域退出时清理。详细生命周期分别见[客户端文件上传](../3_client/file.md)与[服务端文件上传](../4_server/file.md)。

## 资源与文件

`APIRes` 构造层级路径，并实现 `APIFile` 的路径集合接口：

```kotlin
object PublicRes : APIRes("public") {
    object Assets : APIRes(this) {
        val Logo = APIRes(this, "logo.webp")
    }
}
```

仅指定父节点时，路径段由运行时类的简单名称转为小写得到。显式指定名称时使用该名称。客户端 `.url` 在资源路径前添加后端地址，服务端文件辅助函数将路径用于文件操作。

`APIRes` 的路径集合不包含客户端上传流。上传应通过 `apiFile(...)` 创建客户端包装，不能直接将 `APIRes` 作为客户端表单文件内容。

## WebSocket 与接口说明

```kotlin
object DemoSockets : Sockets("/demo/events", "示例事件")
```

`Sockets` 共享路径和标题，消息 DTO、鉴权和重连规则由业务协议定义。客户端入口见[客户端 WebSocket](../3_client/sockets.md)，服务端入口见[服务端 WebSocket](../4_server/sockets.md)。

`@APIDoc`、`@APIParam` 与 `@APIReturn` 用于保留接口说明，不替代运行时校验或鉴权。

## 业务契约示例

共享业务模块 `ylcs-app/cs` 集中维护真实接口声明。状态查询没有输入、返回一个 `ServerStatus`；头像上传接收令牌和非空文件、没有输出：

示例来源：[APIMap.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/cs/src/commonMain/kotlin/love/yinlin/cs/APIMap.kt#L43)。

```kotlin
val ApiCommonGetServerStatus by API.post.i().o<ServerStatus>()
```

示例来源：[APIMap.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/cs/src/commonMain/kotlin/love/yinlin/cs/APIMap.kt#L279)。

```kotlin
@APIParam("token")
@APIParam("avatar")
val ApiProfileUpdateAvatar by API.form.i<String, APIFile>().o()
```

它们分别生成 `/common/getServerStatus` 和 `/profile/updateAvatar`。`APIParam`、`APIReturn`、`APIDoc` 使用 SOURCE 保留策略，说明信息存在于源码，不提供运行时反射元数据或输入验证。客户端调用与服务端响应示例见对应章节。

## 源码入口

- `ylcs-module/cs/core/src/commonMain/kotlin/love/yinlin/cs/API.kt`
- `ylcs-module/cs/core/src/commonMain/kotlin/love/yinlin/cs/APIResult.kt`
- `ylcs-module/cs/core/src/commonMain/kotlin/love/yinlin/cs/APIRes.kt`
- `ylcs-module/cs/core/src/commonMain/kotlin/love/yinlin/cs/APIFile.kt`
