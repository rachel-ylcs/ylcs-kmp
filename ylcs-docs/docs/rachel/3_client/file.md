# 客户端文件上传

`API.form` 将普通 JSON 参数和文件内容编码为 multipart 请求。文件参数由 `APIFile` 标记，上传数据通过 `apiFile(...)` 构造。

## 声明与调用

```kotlin
val ApiDemoUpload by API.form.i<String, APIFile?>().o<String>()

ApiDemoUpload.request(
    "example",
    apiFile(selectedFiles)
) { resultId ->
    updateUploadedItem(resultId)
}.errorTip
```

上例为可空文件契约，空文件列表可传输为 `null`。非空文件契约应在调用前校验选择结果。

表单与普通请求具有相同的回调、`Data` 和 nullable 三种结果形式。参数数量与类型由共享声明决定。

## 构造文件参数

| 调用 | 数据来源 |
| --- | --- |
| `apiFile(text)` | 字符串转换的字节内容 |
| `apiFile(bytes)` | `ByteArray` |
| `apiFile(rawSource)` | 单个 `kotlinx.io.RawSource` |
| `apiFile(sources)` | `Sources<RawSource>` 多文件集合 |
| `apiFile(file)` | 挂起打开框架 `File` 的输入流 |
| `apiFile(files)` | 挂起打开文件列表；空列表或打开失败返回 `null` |

`Sources` 位于 `love.yinlin.io`，`File` 位于 `love.yinlin.fs`。大文件宜使用流，避免先完整读入内存。

客户端实现仅接受 `apiFile` 创建的 `ClientAPIFile`。`APIRes` 或自定义 `APIFile.files` 路径对象不能直接替代上传内容。

## 流所有权

表单构建将已加入请求的 `RawSource` 收集到 `Sources`，请求过程在 `sources.use { ... }` 内执行。无论成功、普通失败还是取消，作用域退出都会调用 `Sources.close()` 关闭已纳入管理的输入流。当前 close 实现按顺序关闭，单个流的关闭异常可能中断后续关闭。

上传参数属于一次性消费对象。调用方不得在请求期间提前关闭或并发读取流，也不得直接将已上传的流对象用于重试。需要重试时重新打开文件并创建新的 `APIFile`。

若在构造参数后、交给请求之前提前退出，尚未移交的流仍需由调用方释放。

## multipart 编码

假设契约为 `API.form.i<String, APIFile?, Int>()`：

| 参数 | 非空文件示例 | 文件为空 |
| --- | --- | --- |
| 第 1 项 | 名称 `0`，FormItem 内容为 JSON 字符串 | 相同 |
| 第 2 项 | 名称 `1`，一个或多个 FileItem | 名称 `1`，FormItem 内容为 `null` |
| 第 3 项 | 名称 `2`，FormItem 内容为 JSON 整数 | 相同 |

多文件使用重复名称，并按 part 到达顺序组成文件集合。当前协议不使用子索引或 `#` 前缀空值占位。服务端会拒绝非法索引、JSON 与文件混用等输入。

文件 part 当前使用通用 `filename="file"`，未传输原始文件名或推断 MIME。需要名称等元数据时，可作为独立普通参数声明。

## 约束

非空文件参数不能用空 `Sources` 表示；无文件应使用可空契约的 `null`。服务端会拒绝零字节 FileItem。

框架不提供该表单 API 的上传进度、分块续传或自动重试。调用方应明确文件数量、大小与格式限制，服务端必须重新验证相关条件。服务端临时文件生命周期见[服务端文件上传](../4_server/file.md)。

## 源码入口

- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/FormRequest.kt`
- `ylcs-module/foundation/filesystem/src/commonMain/kotlin/love/yinlin/io/Sources.kt`
