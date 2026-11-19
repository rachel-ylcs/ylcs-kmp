---
description: 使用 API、APIRes、APIFile 与 Sockets 声明客户端和服务端共享的协议。
---

# 共享契约

<p class="rc-lead"><code>ylcs-module/cs/core</code> 只定义“接口长什么样”，不关心请求如何发送、响应如何生成。契约稳定，两端才能独立演进。</p>

## 声明接口

```kotlin title="API 声明"
val ApiDemoEcho by API.post.i<String>().o<String>()
val ApiDemoStatus by API.post.i().o<String, Long>()
val ApiDemoUpload by API.form.i<String, APIFile?>().o<String>()
```

| 构造 | 输入 | 输出 |
| --- | --- | --- |
| `API.post` | 零至多个位置参数 | 零至多个位置输出 |
| `API.form` | 至少一个表单参数 | 零至多个位置输出 |
| `i<...>()` | 按顺序声明输入类型 | — |
| `o<...>()` | — | 按顺序声明输出类型 |

普通参数与返回值需要支持共享序列化。文件参数使用 `APIFile` 标记，由客户端与服务端分别提供运行时包装。

## 路由生成

API 属性名决定路由：

```text title="命名到路由"
ApiDemoEcho              → /demo/echo
ApiCommonGetServerStatus → /common/getServerStatus
```

规则由 `APIDelegate` 中的正则表达式实现。属性名应符合 `Api<分组><操作>` 的形式；不匹配时不会自动生成预期路径。

!!! danger "API 名称属于协议"
    已发布接口的重命名需要兼容旧路由。输入输出位置与类型调整也必须同步评估旧客户端，不能只修改共享声明。

## POST 线格式

普通请求使用 JSON 数组，位置对应声明参数：

```json title="单参数单输出"
["example"]
```

```json title="多输出"
["ready", 123]
```

`APIResult1` 至 `APIResult5` 表示结果，属性为 `o1` 至 `o5`。无输出的接口以空数组响应，客户端结果为 `Unit`。缺少必要位置或类型不匹配会解码失败。

!!! warning "不要依赖额外元素被忽略"
    实现按位置读取所需元素，但不应假设所有情形都会严格检查数组长度。协议仍应严格按声明发送。

## multipart 线格式

表单参数以从零开始的十进制索引作为 part 名称：

| 参数类型 | 传输形式 |
| --- | --- |
| 普通值 | 同索引的 JSON 文本 FormItem |
| 单文件 | 同索引的 FileItem |
| 多文件 | 多个同名 FileItem，按到达顺序组成一个参数 |
| 可空文件为 `null` | 同索引、内容为 JSON `null` 的 FormItem |

例如第二个参数包含两个文件，两个 FileItem 的名称都为 `1`。当前实现不使用子索引或 `#` 前缀空值占位。

文件流由客户端请求接管并关闭；服务端临时文件在请求处理作用域退出时清理。生命周期见[客户端引擎](client-engine.md)与[服务端引擎](server-engine.md)。

## 资源树

`APIRes` 构造层级路径，并实现 `APIFile` 的路径集合接口：

```kotlin title="资源树"
object PublicRes : APIRes("public") {
    object Assets : APIRes(this) {
        val Logo = APIRes(this, "logo.webp")
    }

    object Users : APIRes(this) {
        class User(uid: Int) : APIRes(this, "$uid") {
            val avatar = APIRes(this, "avatar.webp")
        }
    }
}
```

```text title="路径"
PublicRes.Assets.Logo           → public/assets/logo.webp
PublicRes.Users.User(42).avatar → public/users/42/avatar.webp
```

仅指定父节点时，路径段由运行时类的简单名称转为小写；显式指定名称时按名称拼接。资源层级中的类型重命名可能改变公开路径。

!!! caution "动态路径段需要受控"
    `APIRes(parent, name)` 不对动态段进行编码或路径净化。用户 ID、文件名等输入应限制字符集，不能直接使用任意外部字符串。

## 文件标记

`APIFile` 同时表达两种角色：

| 角色 | 使用方式 |
| --- | --- |
| 客户端上传内容 | 通过 `apiFile(...)` 创建 `ClientAPIFile` |
| 服务端资源路径 | `APIRes` 实现的路径集合 |

`APIRes` 不能直接替代客户端上传内容。上传数据必须由 `apiFile` 创建，否则客户端实现会拒绝。

## Socket 入口

```kotlin title="Sockets"
object DemoSockets : Sockets("/demo/events", "示例事件")
```

`Sockets` 共享路径与标题。消息 DTO、序列化格式、鉴权与重连规则由业务协议定义，传输层不自动生成这些规则。

## 文档注解

```kotlin title="接口说明"
@APIParam("token")
@APIParam("avatar")
val ApiProfileUpdateAvatar by API.form.i<String, APIFile>().o()
```

`@APIDoc`、`@APIParam` 与 `@APIReturn` 使用 SOURCE 保留策略，只保留在源码中，不提供运行时反射元数据、输入验证或鉴权。它们不能替代业务校验。

## 兼容性检查

- 已发布的路由是否保持不变？
- 输入输出的位置与类型是否兼容旧客户端？
- 新增字段是否有默认值？
- 资源路径是否因类型重命名而改变？
- 文件参数从可空改为非空时，旧客户端是否会发送 `null`？
- 服务端是否仍然接受旧协议的边界值？

## 下一步

契约声明完成后，阅读[客户端引擎](client-engine.md)，了解如何在应用中调用这些接口。
