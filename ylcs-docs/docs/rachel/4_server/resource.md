# 服务端静态资源

`ServerEngine.public` 同时决定本地静态目录名和远程路由前缀。若值为 `public`：

```text
本地：<currentDirectory>/public/assets/logo.webp
远程：GET /public/assets/logo.webp
```

共享 `APIRes` 可以在客户端与服务端引用同一条路径：

```kotlin
object ServerRes : APIRes("public") {
    object Assets : APIRes(this) {
        val Logo = APIRes(this, "logo.webp")
    }
}

override val public = ServerRes.toString()
```

## 静态路由行为

- 只处理 GET。
- 请求目录时尝试返回目录内的 `index.html`。
- 根据扩展名推断 Content-Type，无法识别时使用 `application/octet-stream`。
- 文本、SVG 及部分 application 文本格式补 UTF-8 charset。
- 文件不存在时返回 404。
- 通过 Source 流式响应并提供文件长度。

当前实现不生成目录列表，也没有 ETag、Range、压缩或显式 Cache-Control。大流量资源更适合由反向代理/CDN 直接服务；应用服务端可专注于 API 和授权。

## 写入资源

服务端的 `APIRes` 同时实现 `APIFile`，可作为上传文件目标：

```kotlin
val target = ServerRes.Users.User(uid).avatar
uploaded.copy(target)
```

这会把第一个上传临时文件同步复制到目标的第一个路径。目标父目录需要提前建立；覆盖、原子替换和旧文件清理应由业务逻辑明确控制。

建议写入临时目标，验证和处理完成后再移动/替换正式资源，避免请求中断留下半文件。对图片等内容，生成服务端决定的文件名，不使用客户端原始文件名。

## 安全边界

静态目录里的任何文件都应视为无需鉴权即可读取。不要放置：

- `config.json`、数据库备份或日志；
- 用户原始上传、未审核内容；
- 临时文件或含访问令牌的导出；
- 服务端二进制和调试符号。

动态 `APIRes` 名称必须限制字符并拒绝分隔符、`..`、查询字符。静态路由自身没有额外业务 allowlist；还应在反向代理层验证路径处理、禁用不需要的方法并限制响应大小。

## 缓存策略

框架不设置缓存头。常见做法：

- 不可变资源使用内容摘要文件名和长缓存；
- 用户头像等稳定 URL 使用版本 query/key 或短缓存；
- 私有资源不走公开静态目录，改用鉴权 API/签名 URL；
- HTML 入口短缓存，避免新版引用旧资源图。

客户端 `.url` 只是地址拼接，缓存语义必须由部署层和资源命名共同定义。
