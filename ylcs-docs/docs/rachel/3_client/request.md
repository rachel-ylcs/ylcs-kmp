# 客户端请求

普通 `API.post` 把输入编码为 JSON 数组，并把成功响应按声明的输出顺序解码。即使没有输入或输出，线格式仍是数组。

## 回调版

```kotlin
val error: Throwable? = ApiProfileGetProfile.request(token) { profile ->
    app.config.userProfile = profile
}

error?.let { slot.tip.error(it.message) }
```

成功时调用 block 并返回 `null`；任意请求、状态码、解码或成功回调异常都会作为 `Throwable` 返回。页面基类提供 `.errorTip` / `.warningTip` 便捷属性：

```kotlin
ApiCommonSendFeedback.request(token, content) {
    slot.tip.success("提交成功")
}.errorTip
```

成功回调也在该错误边界内。若回调做了多步本地修改并在中途抛错，网络请求其实已经成功；需要事务性的本地操作应自己组织。

## 结果版

```kotlin
when (val result = ApiAccountValidateToken.request(token)) {
    is Data.Success -> {
        val valid = result.data.o1
        updateSession(valid)
    }
    is Data.Failure -> handleError(result.throwable)
}
```

输出使用 `APIResult1` 到 `APIResult5` 包装，可通过 `o1`…`o5` 或解构读取：

```kotlin
when (val result = ApiProfileSignin.request(token)) {
    is Data.Success -> {
        val (status, value, index) = result.data
    }
    is Data.Failure -> Unit
}
```

没有输出的 API 成功数据是 `Unit`。结果版最适合状态机、认证刷新和需要针对异常类型决定下一步的代码。

## Nullable 版

```kotlin
val page = ApiTopicGetLatestTopics
    .requestNull(offset, pageSize)
    ?.o1
    ?: return
```

它适合“取不到就不更新”的非关键刷新。登录、支付、写入、删除等操作不应使用 nullable 版，因为失败原因对用户和恢复策略都很重要。

## 输入输出规则

声明：

```kotlin
val ApiExampleQuery by
    API.post.i<String, Int?>().o<List<Item>, Long>()
```

请求体：

```json
["keyword", null]
```

响应体：

```json
[[{"id":"a"}], 42]
```

客户端与服务端严格依赖位置和数量。返回少一个元素、改变顺序或让非空输出变成 `null` 都会在解码阶段失败。需要演进复杂参数时，把它封装为带默认字段的 `@Serializable` DTO，比分散增加位置参数更可维护。

当前 DSL 最多 5 个输入和 5 个输出。接近上限通常说明接口应该用 request/response DTO 表达，而不是继续扩展元数。

## 错误处理建议

```kotlin
fun handleError(error: Throwable) {
    when (error) {
        is UnauthorizedException -> forceLogin()
        is FailureException -> slot.tip.warning(error.message)
        is RequestTimeoutException -> slot.tip.error("请求超时")
        else -> slot.tip.error(error.message ?: "网络异常")
    }
}
```

`FailureException` 是服务端主动返回的可预期业务失败；其他未分类异常可能来自网络、协议不兼容或本地代码。生产日志要保留原异常和 API route，但不要记录密码、令牌或完整隐私请求体。

## 并发与幂等

引擎不做重复请求合并或按钮防抖。读取型请求可在 DataSource 中用最新任务覆盖旧任务；写入型请求应在 UI 禁用重复提交，并让服务端以业务键保证幂等。导航自带的 300 ms 防抖不等于网络防重。
