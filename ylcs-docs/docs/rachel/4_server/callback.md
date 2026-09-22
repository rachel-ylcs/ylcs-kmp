# 服务端回调分发

`APICallback` 不是网络回调。它是带 `APICallbackScope` receiver 的挂起函数类型，用于把一个输入按业务键分派给不同处理器：

```kotlin
typealias APICallback<I, O> = suspend APICallbackScope.(I) -> O
typealias APICallbackMap<K, I, O> = MutableMap<K, APICallback<I, O>>
```

## 建立分发表

```kotlin
class AppServerScope(engine: ServerEngine) : APIScope(engine) {
    val mailCallbacks =
        buildCallBackMap<MailType, MailEntry, String>()

    override fun api() {
        accountAPI() // 注册 Register 处理器
        rewardAPI()  // 注册 Reward 处理器
        mailAPI()    // 使用分发表
    }
}
```

领域模块注册自己的行为：

```kotlin
fun AppServerScope.accountAPI() {
    mailCallbacks[MailType.Register] = { entry ->
        val code = entry.param1.ifBlank { failure("验证码无效") }
        createAccount(entry, code)
        "注册成功"
    }
}
```

处理接口按键调用：

```kotlin
ApiMailProcess.response { token, mailId, confirm ->
    auth.throwExpireToken(token)
    val entry = loadMail(mailId)

    val message = if (confirm) {
        val type = MailType.fromValue(entry.type)
            ?: failure("未知邮件类型")
        val callback = mailCallbacks[type]
            ?: failure("该邮件类型暂不可处理")
        callback(entry)
    } else {
        "已拒绝"
    }

    markProcessed(mailId)
    result(message)
}
```

因为当前 API response scope 也继承 `APICallbackScope`，调用 callback 时可以隐式使用同一个 receiver，`failure()` / `expire()` 会沿请求错误协议传播。

## 适合场景

- 邮件/奖励类型对应不同领域动作；
- 命令名对应不同处理器；
- 插件在启动注册一组纯业务策略。

它不提供事件广播、多个监听器、优先级、隔离或持久队列。一个 key 只有一个 callback，重复赋值会覆盖旧处理器。

## 初始化与并发

注册通常发生在 `api()` 建路由期间，并在接收请求前结束。运行期间不要无锁修改普通 `MutableMap`。若确实需要动态注册，改用受 Mutex 保护的快照或不可变映射原子替换。

不要使用 `map[key]!!` 假设注册永远完整；新增枚举值或漏调某个 `xxxAPI()` 会变成 403 和服务端堆栈。启动准备阶段可以校验所有期望 key，并在缺失时直接拒绝启动。

## 事务边界

callback 只是函数调用，不自动创建数据库事务。像“执行奖励，再标记邮件已处理”这样的流程应放入同一数据库事务，否则中途失败会导致重复领取或状态不一致：

```kotlin
mysql.throwTransaction { tx ->
    // callback 若要共享 tx，应把它纳入输入/上下文设计
}
```

设计 callback 输入时传递完成操作所需的上下文，或让 callback 调用封装好的事务服务。不要让回调通过全局变量隐式抓取一次请求的临时状态。
