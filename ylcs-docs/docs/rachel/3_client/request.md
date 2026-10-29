# 客户端请求

普通 API 请求使用 HTTP POST 与 JSON 数组，输入输出位置对应共享声明。本文沿用以下示例契约：

```kotlin
val ApiDemoEcho by API.post.i<String>().o<String>()
val ApiDemoStatus by API.post.i().o<String, Long>()
```

## 成功回调

```kotlin
val error: Throwable? = ApiDemoEcho.request("example") { message ->
    content = message
}

error?.let { slot.tip.error(it.message) }
```

成功时执行回调并返回 `null`。网络、状态码、解码和回调中的普通异常以 `Throwable` 返回，取消异常继续抛出。

`BasicScreen` 提供 `.errorTip` 与 `.warningTip` 等成员扩展：

```kotlin
ApiDemoEcho.request("example") { message ->
    content = message
}.errorTip
```

这些扩展依赖页面浮层槽，只适用于相应页面上下文。

## 显式结果

```kotlin
when (val result = ApiDemoStatus.request()) {
    is Data.Success -> {
        val [message, timestamp] = result.data
        updateStatus(message, timestamp)
    }
    is Data.Failure -> handleError(result.throwable)
}
```

`Data` 位于 `love.yinlin.data`。`APIResult1` 至 `APIResult5` 提供 `o1` 至 `o5` 属性，也可按位置解构。无输出接口的成功数据为 `Unit`。

显式结果形式适合根据失败类型选择下一步的流程，例如鉴权状态更新和多步骤业务操作。

## 可空结果

```kotlin
val message = ApiDemoEcho.requestNull("example")?.o1 ?: return
```

此形式将普通失败折叠为 `null`，不适合需要保留失败原因的提交、删除或权限处理。取消异常不会被转换为空结果。

## 参数与线格式

`ApiDemoEcho` 的请求体与成功响应均为单元素数组：

```json
["example"]
```

输入与输出必须保持声明位置和类型。实现会读取所需的数组元素；缺少必要元素或类型不兼容会解码失败。协议演进应依据共享声明及兼容规则，不应依赖额外元素被忽略的实现细节。

复杂接口可将参数封装为可序列化 DTO，并通过默认值和明确字段名维护兼容性。当前 DSL 的输入输出上限均为五项。

## 错误分类

```kotlin
when (error) {
    is UnauthorizedException -> requestLogin()
    is FailureException -> slot.tip.warning(error.message)
    is RequestTimeoutException -> slot.tip.error("请求超时")
    null -> Unit
    else -> slot.tip.error(error.message ?: "请求失败")
}
```

`FailureException` 表示服务端按协议返回的业务失败。其他异常可能来自连接、协议解码或本地成功回调。

客户端不自动合并重复请求或重试写入。读取任务可由状态所有者取消或替换；写入操作应明确重复提交限制及服务端幂等语义。

## 业务中的结果转换

`DataSourceAccount.checkUpdate` 保留请求失败通道，只将成功结果的第一项转换为业务模型：

示例来源：[DataSourceAccount.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/app/account/src/commonMain/kotlin/love/yinlin/compose/ds/DataSourceAccount.kt#L126)。

```kotlin
suspend fun checkUpdate(): Data<ServerStatus> {
    return ApiCommonGetServerStatus.request().map { it.o1 }
}
```

`ApiCommonGetServerStatus` 在共享 `APIMap.kt` 中声明为零输入、单输出；`Data.map` 只转换成功数据。该写法适用于上层继续区分网络、鉴权或协议错误的场景，比在请求后直接丢弃错误信息更适合更新检查的状态展示。

## 源码入口

- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/PostRequest.kt`
- `ylcs-module/cs/core/src/commonMain/kotlin/love/yinlin/cs/APIResult.kt`
- `ylcs-module/core/src/commonMain/kotlin/love/yinlin/data/Data.kt`
