# 服务端请求

`APIScope` 以共享 API 声明注册 POST 路由，并提供类型化 `response` 扩展。业务处理负责鉴权、参数验证与结果生成。

## 注册接口

```kotlin
class DemoScope(engine: ServerEngine) : APIScope(engine) {
    override fun api() {
        demoAPI()
    }
}

fun DemoScope.demoAPI() {
    ApiDemoEcho.response { text ->
        if (text.isBlank()) failure("内容不能为空")
        result(text)
    }
}
```

`ApiDemoEcho` 为 `API.post.i<String>().o<String>()`。领域扩展应由 `api()` 调用，确保注册时路由宿主已经建立。重复或遗漏注册需由应用结构避免。

有输出时，处理器使用 `APIResultScopeN` 并返回 `result(...)`；无输出时使用 `APIResponseScope`，正常结束后响应空数组。`API.form` 的注册方式相同，文件生命周期见[文件上传](file.md)。

## 失败协议

响应作用域继承 `APICallbackScope`：

```kotlin
val userId = validateToken(token) ?: expire()
if (!canEdit(userId, itemId)) failure("无权修改该条目")
result(saveItem(itemId))
```

上例的验证与保存函数由业务实现。

| 处理结果 | HTTP 状态 | 响应形式 |
| --- | --- | --- |
| 正常 | 200 | JSON 输出数组 |
| `failure(message)` / `FailureException` | 202 | 错误文本 |
| `expire()` / `UnauthorizedException` | 401 | 空 JSON 数组 |
| 其他普通异常 | 403 | 空 JSON 数组 |

普通异常记录请求 URI 与堆栈，再执行上述映射。`CancellationException` 由 `catchingError` 继续抛出，不进入普通错误响应分支。

403 是当前引擎的未分类异常协议，不足以推断真实故障原因。客户端映射见[客户端引擎](../3_client/entry.md)。

## 参数校验

共享类型仅约束传输结构，业务处理仍需检查内容长度、数值范围、资源存在性、用户权限、资源归属和重复操作语义。

POST 解析器按数组位置读取所需参数，未对所有接口统一检查完整数组长度。客户端与服务端应严格按声明发送和接收，不依赖额外值被忽略的细节。

## 调度与资源

接口 block 在 `Coroutines.io` 中执行，请求之间可并发。共享可变状态需采用适当同步方式。长时间 CPU 工作应切换至相应上下文，并避免无限占用请求处理资源。

数据库事务、文件流和上传临时文件具有请求作用域，不应保存为全局服务字段或交给请求结束后才读取的后台任务。

## 协议兼容

API 名称决定路由，数组位置决定参数与输出含义。已发布接口的重命名、类型修改或位置调整均需评估旧客户端。

复杂协议可使用带默认字段的可序列化 DTO；需要破坏性变更时应保留兼容处理或新增接口，避免直接使旧路由失效。

## 业务中的状态接口

服务端 `commonAPI()` 注册与客户端更新检查对应的真实接口：

示例来源：[CommonAPI.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/server/src/nativeMain/kotlin/love/yinlin/cs/CommonAPI.kt#L11)。

```kotlin
ApiCommonGetServerStatus.response {
    result(ServerStatus(
        targetVersion = Local.info.version,
        minVersion = Local.info.minVersion,
    ))
}
```

`ServerScope.api()` 调用 `commonAPI()` 完成注册。`Local.info` 提供业务版本，`result` 将 `ServerStatus` 作为单个输出编码成数组。示例没有输入；令牌检查与业务校验可参考同文件的反馈接口，不能从共享参数类型推断接口已经鉴权。

## 源码入口

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APIScope.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APIResponseScope.kt`
- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APICallback.kt`
