# 服务端回调分发

`APICallback` 用于按业务键调用处理器，可显式传递事务或其他请求上下文。它是进程内函数调用，不创建网络请求或持久任务。

## 类型定义

```kotlin
typealias APICallback<I, O, C> = suspend APICallbackScope.(C, I) -> O
typealias APICallbackMap<K, I, O, C> = MutableMap<K, APICallback<I, O, C>>
```

| 类型参数 | 含义 |
| --- | --- |
| `K` | 分发键 |
| `I` | 业务输入 |
| `O` | 处理结果 |
| `C` | 显式上下文，例如 `QueryExecutor` |

receiver `APICallbackScope` 提供 `failure()` 与 `expire()`。上下文 `C` 是额外参数，不替代该 receiver。

## 注册处理器

以下字段位于自定义 `APIScope` 中：

```kotlin
val callbacks = buildCallBackMap<String, String, String, QueryExecutor>()
```

在 `api()` 或其调用的注册函数中设置行为：

```kotlin
callbacks["store"] = { transaction, text ->
    if (text.isBlank()) failure("内容不能为空")
    transaction.throwExecuteSQL(
        "INSERT INTO demo(value) VALUES(?)",
        text
    )
    "处理完成"
}
```

`QueryExecutor` 来自 SQLx4k，SQL 扩展位于 `love.yinlin.cs.service`。示例表结构由业务提供。

## 在事务中调用

共享接口为 `API.post.i<String, String>().o<String>()` 时：

```kotlin
ApiDemoProcess.response { operation, text ->
    val callback = callbacks[operation] ?: failure("不支持的操作")
    val message = mysql.throwTransaction { transaction ->
        callback(transaction, text)
    }
    result(message)
}
```

API 响应作用域继承 `APICallbackScope`，因此可作为 callback 的 receiver。事务通过参数明确传入，各处理器应使用该 executor 完成需要处于同一事务内的操作。

回调本身不会创建事务。如果处理器改为通过服务重新获取连接，其操作不再由传入事务统一约束。

## 生命周期与并发

`buildCallBackMap` 返回普通可变 Map，一个键对应一个处理器，重复赋值会覆盖。注册宜在开始接收请求前完成；运行期动态修改需要相应同步策略。

回调输入、事务和临时资源属于当前调用，不应保存至全局字段。未知键应显式返回业务失败，不应依赖强制解包。

## 业务中的回调注册

邮件奖励处理器通过传入的同一事务更新余额，并返回提示文本：

示例来源：[CommonAPI.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/server/src/nativeMain/kotlin/love/yinlin/cs/CommonAPI.kt#L24)。

```kotlin
callMap[Mail.Filter.CoinReward] = { transaction, (val uid, val param1) ->
    val coin = param1?.toIntOrNull() ?: 0
    VN.throwIf(coin <= 0)
    transaction.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", coin, uid)
    "领取成功"
}
```

MailAPI 先在事务中以 `FOR UPDATE` 读取邮件，检查处理状态，再按 filter 查找此回调；回调和将邮件标记为已处理的 UPDATE 使用同一个 `QueryExecutor`。`VN`、`Mail` 与邮件字段是业务上下文，事务与状态检查是调用方建立的约束，回调 Map 本身不提供一次性执行保证。

## 应用示例与源码

业务服务端的 `ServerScope.callMap` 使用 `Mail.Filter`、`MailEntry`、String 与 `QueryExecutor` 四个类型参数。`MailAPI` 在事务中调用 `callback(transaction, mailEntry)`，各领域注册对应邮件处理器。

- `ylcs-module/cs/server-engine/src/nativeMain/kotlin/love/yinlin/cs/APICallback.kt`
- `ylcs-app/server/src/nativeMain/kotlin/love/yinlin/cs/ServerScope.kt`
- `ylcs-app/server/src/nativeMain/kotlin/love/yinlin/cs/MailAPI.kt`
