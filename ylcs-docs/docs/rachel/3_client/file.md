# 客户端文件上传

文件上传由 `API.form` 与 `APIFile` 描述。普通参数仍按 JSON 编码，文件内容成为 multipart part；两者按声明位置组合。

## 声明与调用

```kotlin
val ApiProfileUpdateAvatar by
    API.form.i<String, APIFile>().o<String>()

val error = ApiProfileUpdateAvatar.request(
    token,
    apiFile(file)
) { newUrl ->
    updateAvatar(newUrl)
}
```

`form` 至少有一个输入。输出与普通 POST 相同，也支持 callback、`Data` 和 nullable 三套调用风格。

## 构造 APIFile

```kotlin
apiFile("text content")       // UTF-8 字节
apiFile(bytes)                // ByteArray
apiFile(rawSource)            // kotlinx.io.RawSource
apiFile(sources)              // Sources<RawSource>，多文件
apiFile(file)                 // 挂起：打开 File 的 RawSource
apiFile(files)                // 挂起：空列表返回 null
```

大文件优先 `File` / `RawSource`，避免完整复制进内存。手工传入 Source 时，不要在请求完成前读取、关闭或复用；调用方也应确认底层来源的最终关闭责任。最安全的常规路径是直接从框架 `File` 构造并在同一挂起调用中上传。

`apiFile(List<File>)` 返回可空值，因为空列表被表示为“没有文件”。如果协议要求至少一项，应在调用前验证，不要直接用 `!!` 把业务前提隐藏掉。

## multipart 索引协议

参数名不是属性名，而是位置：

```text
0             第 1 个参数
1             第 2 个参数
3:0, 3:1      第 4 个参数中的多文件
#2            第 3 个可空文件为空时的占位
```

普通值内容是 `toJsonString()`；单文件内容为字节/Source；多文件共享主索引并用子索引排序。空文件占位保证后续参数位置不移动。服务端解析器依赖这个规则，因此第三方客户端必须完全复刻，而不能用 `token`、`avatar` 等自定义 part name。

当前上传 part 的 Content-Disposition 使用通用 `filename="file"`，不携带原始文件名或 MIME。接口需要这些元数据时，把经过校验的文件名/MIME 作为单独普通参数传入；不要信任客户端声明来决定服务端落盘路径。

## 多文件示例

```kotlin
val upload = apiFile(selectedFiles)
if (upload == null) {
    slot.tip.warning("请至少选择一个文件")
    return
}

ApiTopicSendTopic.request(
    token,
    title,
    content,
    section,
    upload
) { topicId, storedNames ->
    navigate(::ScreenTopic, topicId)
}.errorTip
```

## 限制与校验

客户端选择器的扩展名过滤只是体验功能，不是安全校验。服务端必须重新检查：

- 总请求和单文件大小；
- 实际内容/MIME，而非文件名；
- 图片尺寸、压缩炸弹等格式风险；
- 用户权限与目标资源归属；
- 动态目标路径是否越界。

上传失败后客户端不会自动重试，也没有内置进度回调。需要大文件断点续传或可靠进度时，应设计专用协议，而不是继续扩展当前的一次性 multipart。
