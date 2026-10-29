# 服务端静态资源

`ServerEngine.public` 决定静态目录名称与远程路由前缀。例如设置为 `public` 时：

```text
本地目录：<currentDirectory>/public
访问地址：GET /public/assets/logo.webp
```

共享资源树的根应与该前缀保持一致：

```kotlin
object PublicRes : APIRes("public") {
    object Assets : APIRes(this) {
        val Logo = APIRes(this, "logo.webp")
    }
}

override val public = PublicRes.path
```

## 响应行为

当前静态路由实现如下：

- 处理 GET 请求。
- 目录请求尝试读取其中的 `index.html`。
- 根据扩展名选择 Content-Type，未知类型使用 `application/octet-stream`。
- 为文本、SVG 及部分 application 文本类型补充 UTF-8 charset。
- 通过 Source 流式响应，并提供文件长度。
- 路径无对应文件或文件长度为零时返回 404，零字节文件不会以 200 响应。

封装未提供目录列表、ETag、Range、压缩和显式 Cache-Control。需要相关能力时由部署层或独立路由实现。

## 文件操作与路径

`APIRes` 实现 `APIFile`，可以作为服务端 `copy` 的目标：

```kotlin
val target = PublicRes.Assets.Logo
uploaded.copy(target)
```

此辅助函数直接使用 `APIFile.files` 中的路径，同步复制第一个文件，不自动以 `engine.currentDirectory` 重新定位。使用相对路径时，必须确认进程工作目录；也可显式以引擎目录构造绝对目标路径。

父目录、覆盖策略和持久文件生命周期由业务管理。复制上传文件不会改变解析器对源临时文件的所有权。

## 公开范围

静态目录中的文件按公开内容处理。需要鉴权的导出、原始上传、配置和日志应放置在其他目录，并通过受控接口访问。

`APIRes` 不校验动态路径段。业务 ID 与文件名应使用受控字符集，静态路由直接拼接请求路径的实现也不应被描述为已经完成所有路径安全校验。

## 缓存策略

不可变资源可使用内容摘要名称；头像等可变资源应通过版本与缓存头明确更新行为。客户端 `.url` 仅生成地址，资源缓存策略需与部署配置共同确定。

## 业务中的资源落盘

头像处理器将共享用户资源路径作为复制目标：

示例来源：[ProfileAPI.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/server/src/nativeMain/kotlin/love/yinlin/cs/ProfileAPI.kt#L93)。

```kotlin
ApiProfileUpdateAvatar.response { token, avatar ->
    val uid = AN.throwExpireToken(token)
    avatar.copy(ServerRes.Users.User(uid).avatar)
}
```

`AN` 负责校验业务令牌；`ServerRes.Users.User(uid).avatar` 生成 `public/users/<uid>/avatar.webp`。该片段展示当前实现，复制调用本身不检查图片编码或尺寸，也不会将路径自动改为引擎绝对目录。内容校验、目录准备与文件覆盖策略需在使用场景中明确。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/StaticRoute.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APIFiles.kt`
- `ylcs-module/cs/core/src/commonMain/kotlin/love/yinlin/cs/APIRes.kt`
