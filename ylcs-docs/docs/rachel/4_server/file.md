# 服务端文件上传

`API.form` 解析 multipart 请求，将普通值与上传文件按参数索引恢复。文件内容先写入临时目录，处理器在请求作用域中接收 `APIFile`。

## 接收与保存

共享声明：

```kotlin
val ApiDemoUpload by API.form.i<String, APIFile?>().o<String>()
```

以下片段位于 `APIScope` 的接口注册函数中，仅展示路径校验与保存流程：

```kotlin
ApiDemoUpload.response { requestedId, upload ->
    if (!requestedId.matches(Regex("[A-Za-z0-9_-]+"))) failure("标识格式不正确")
    val file = upload ?: failure("未选择文件")
    if (file.num != 1) failure("文件数量不正确")

    val directory = File(engine.currentDirectory, "uploads")
    directory.mkdir()
    val target = APIRes(APIRes(directory.path), "$requestedId.bin")
    validateContent(file[0])
    file[0].copy(target)
    result(requestedId)
}
```

实际接口还需完成鉴权、资源归属和覆盖规则。`validateContent` 为业务校验。示例将文件保存到独立目录，避免未验证内容直接进入公开静态目录。

## multipart 解析规则

part 名称必须为从 `0` 开始、连续的十进制非负索引，不接受多余前导零、子索引或 `#` 前缀。

| 输入 | 行为 |
| --- | --- |
| 同索引的一个 FormItem | 将内容解析为 JSON |
| 同索引的多个 FileItem | 按接收顺序组成一个文件参数 |
| 文件参数的 JSON `null` | 仅在声明允许空值时恢复为 `null` |
| 重复 FormItem | 拒绝 |
| 同索引混用 JSON 与文件 | 拒绝 |
| 零字节 FileItem | 拒绝 |

参数类型与实际 part 不匹配时失败。解析器检查索引连续性，但未统一按 API 声明拒绝所有多余连续参数，接口调用方仍应严格遵循共享契约。

## 临时文件生命周期

临时目录通过以下调用获取：

```kotlin
PlatformFileSystem.cachePath(PlatformContext.Instance, "ServerNative")
```

`FormResult` 在写入前登记临时文件，通过 `use { ... }` 包围解析和业务处理。无论解析、处理成功，还是普通异常与取消，退出该作用域时都会尝试删除本次登记的文件；part 也在各自的 `finally` 路径释放。

临时文件仅在该请求处理作用域内有效。需要持久保存时，应在处理器返回前复制到业务目录；不得将临时路径直接交给稍后执行的后台任务。

清理会继续尝试其余文件并汇总错误，但文件系统错误或进程异常终止仍可能留下临时文件。是否需要额外的过期清理由部署和业务规模决定。

## 文件辅助接口

| 接口 | 行为 |
| --- | --- |
| `file.files` | 底层文件路径列表 |
| `file.isEmpty`、`file.num` | 空值与数量 |
| `file[index]` | 单文件包装 |
| `file.copy(target)` | 将第一个源同步复制到第一个目标，返回目标 File |
| `file.mkdir()` | 创建第一个路径对应的目录 |
| `file.delete()` | 递归删除包装中的路径 |

这些扩展位于 `love.yinlin.cs`。请求临时文件的自动清理由解析器承担，业务目标文件的保留与删除由业务承担。

## 大小与格式约束

当前代码调用无显式参数的 `receiveMultipart()`，未在框架层定义独立的总请求、单文件、文件数量或展开内容上限，也不再使用旧文档中的 Content-Length 推导规则。

限制应根据实际上传协议在入口和业务校验层明确。文件名、MIME 与扩展名不能替代内容验证。复杂媒体处理应在临时文件有效期内完成，或先转存至具有明确所有权的工作目录。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APIScope.kt` 中的 `FormResult` 与 `internalResponseForm`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APIFiles.kt`
- [客户端文件上传](../3_client/file.md)
