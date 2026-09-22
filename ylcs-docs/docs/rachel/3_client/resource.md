# 客户端资源

`APIRes` 用共享类型描述静态资源路径。客户端通过 `.url` 使用它，服务端则把同一对象映射到公开目录或文件操作，避免两端分别拼接字符串。

## 定义资源树

```kotlin
object ServerRes : APIRes("public") {
    object Assets : APIRes(this) {
        val DefaultAvatar = APIRes(this, "default_avatar.webp")
    }

    object Users : APIRes(this) {
        class User(uid: Int) : APIRes(this, "$uid") {
            val avatar = APIRes(this, "avatar.webp")
            val wall = APIRes(this, "wall.webp")
        }
    }
}
```

生成路径：

```text
ServerRes.Assets.DefaultAvatar -> public/assets/default_avatar.webp
ServerRes.Users.User(42).avatar -> public/users/42/avatar.webp
```

只传父节点时，当前类的简单名称会转成小写路径段；显式传 name 时使用给定值。嵌套对象名因此也是公开路径的一部分，重命名需要迁移或兼容别名。

## 生成 URL

```kotlin
ClientEngine.init("https://api.example.com")

val avatarUrl = ServerRes.Users.User(uid).avatar.url
// https://api.example.com/public/users/42/avatar.webp
```

`.url` 只是 `baseUrl` 与资源 path 的直接拼接，不检查文件是否存在，也不附加版本、鉴权或缓存控制。图片变更后需要破缓存时，可由消费组件提供 key/query 策略，或使用内容摘要文件名。

## 动态路径安全

`APIRes(parent, name)` 不进行 percent 编码和路径净化。动态段应来自受控 ID，不能直接接受含 `/`、`..`、`?`、`#` 的用户输入：

```kotlin
require(id.matches(Regex("[A-Za-z0-9_-]+")))
val resource = APIRes(ServerRes.Users, id)
```

面向任意外部 URL 或复杂查询参数时使用 `Uri`，不要让 `APIRes` 兼任 URL builder。

## 与 UI 图片的组合

```kotlin
WebImage(
    uri = ServerRes.Users.User(uid).avatar.url,
    key = avatarVersion,
    circle = true
)
```

`APIRes` 只负责稳定地址；`WebImage`/Sketch 负责请求、内存与磁盘缓存。资源 URL 指向用户可变内容时，提供稳定的版本 key，避免每次重组绕过缓存，也避免更新后继续显示旧图。

## 公开资源与受保护内容

当前 ServerEngine 的静态路由直接提供目录内容。需要授权检查、临时签名或审计的文件不应仅靠 `APIRes.url` 暴露；应通过 API 验证后返回数据/短期 URL，或安装独立的受控路由。
