# 服务端文件上传

`API.form` 的 multipart 解析器把普通值和文件按索引恢复为共享声明中的参数。文件先流式写入服务端缓存目录，再以 `APIFile` 交给接口实现。

## 接收上传

共享声明：

```kotlin
val ApiProfileUpdateAvatar by
    API.form.i<String, APIFile>().o<String>()
```

服务端实现：

```kotlin
ApiProfileUpdateAvatar.response { token, file ->
    val uid = auth.throwExpireToken(token)
    if (file.isEmpty) failure("没有收到文件")

    val target = ServerRes.Users.User(uid).avatar
    validateImage(file[0])
    file[0].copy(target)

    result(target.toString())
}
```

可空 `APIFile?` 由客户端的 `#index` 占位恢复为空；多文件按 `index:subindex` 排序后成为一个 `APIFile`。

## 临时文件位置

解析器使用：

```text
PlatformFileSystem.cachePath(PlatformContext.Instance, "ServerNative")
```

每个成功写入的 part 获得随机数、参数索引和唯一时间组成的临时名。`APIFile.files` 存这些路径。

当前实现没有在请求结束时自动删除临时文件。接口无论成功、业务失败还是处理异常，都应安排清理；还需要进程启动时/定时清除过期临时文件，防止磁盘持续增长。复制到正式目录不会删除源文件。

## APIFile 工具

| API | 行为 |
| --- | --- |
| `file.isEmpty` | 是否没有任何路径 |
| `file.num` | 文件数量 |
| `file[index]` | 取单个文件的新 APIFile 包装 |
| `file.copy(target)` | 第一个源同步复制到第一个目标，返回目标 File |
| `file.delete()` | 递归删除包装中的所有路径 |
| `file.mkdir()` | 为第一个路径创建目录 |

多文件要逐项处理：

```kotlin
try {
    repeat(files.num) { index ->
        val source = files[index]
        val target = APIRes(albumRoot, "$index.webp")
        source.copy(target)
    }
} finally {
    files.delete()
}
```

这些 helper 使用同步文件操作；请求处理已位于 I/O 上下文，但大型转码、解压或扫描仍应分离成受控任务，并设置并发上限。

## 请求大小现状

multipart 解析直接使用请求的 `Content-Length` 作为 field limit；缺少长度时回退为 5 MiB。这里没有独立的可信“最大上传大小”，客户端可以声明很大的长度。生产环境必须在反向代理和应用层同时限制总大小、part 数量与单文件大小。

不要依赖客户端 `filename` 或 Content-Type；当前客户端本来也只发送通用文件名。根据魔数/解析结果验证真实内容，并限制图片像素、压缩包展开量、媒体时长等资源消耗。

## 安全落盘流程

推荐顺序：

1. 鉴权并检查资源归属。
2. 检查文件数与已写入大小。
3. 在临时区识别并完整解析格式。
4. 对图片/媒体重编码，去除不需要的元数据。
5. 使用服务端生成的安全文件名写临时目标。
6. 原子替换正式文件或提交数据库记录。
7. `finally` 删除请求临时文件。

不要把上传内容直接放进公开目录后再验证；静态路由可能在验证完成前就把它提供给外部。
