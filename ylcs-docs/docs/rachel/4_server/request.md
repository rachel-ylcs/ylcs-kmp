# 服务端请求

`APIScope` 为共享 `API.post` / `API.form` 提供 `response` 扩展。路由和参数类型来自同一份声明，服务端只实现验证、业务逻辑与返回值。

## 注册接口

把同一领域的实现放在 `APIScope` 扩展函数中：

```kotlin
fun AppServerScope.accountAPI() {
    ApiAccountLogin.response { account, password, platform ->
        val token = authenticate(account, password, platform)
            ?: failure("账号或密码错误")
        result(token)
    }

    ApiAccountLogOff.response { token ->
        val uid = auth.throwExpireToken(token)
        sessions.remove(uid)
    }
}
```

然后只在 `api()` 中调用一次：

```kotlin
override fun api() {
    accountAPI()
    profileAPI()
}
```

所有共享 API 当前注册为 HTTP POST。`response` 会按声明解构 JSON 输入；有输出时 block 的 receiver 是对应 `APIResultScopeN`，用 `result(...)` 返回固定数量的值；无输出时正常结束即可。

## 验证与失败

所有 Response/Result Scope 都继承 `APICallbackScope`：

```kotlin
val uid = validateToken(token) ?: expire()
if (name.isBlank()) failure("名称不能为空")
return result(savedProfile)
```

映射规则与客户端成对：

| 服务端异常 | HTTP | 响应 | 客户端 |
| --- | --- | --- | --- |
| 正常 | 200 | JSON 输出数组 | 成功 |
| `failure(message)` | 202 | 文本 message | `FailureException` |
| `expire()` / `UnauthorizedException` | 401 | 空 JSON 数组 | `UnauthorizedException` |
| 其他异常 | 403 | 空 JSON 数组 | `IllegalArgumentException` |

所有异常都会先记录 `CallError: <uri>` 与堆栈。未预期异常被映射为 403 是当前内部协议约定，并不代表它真的是权限错误；运维必须查看服务端日志，客户端不应从 403 推断具体原因。

不要用 `failure` 隐藏基础设施故障。业务可预期拒绝才使用 202；数据库连接断开、序列化缺陷等应抛出原异常，让日志保留原因。

## 参数校验

共享序列化保证“能解成某个 Kotlin 类型”，不保证业务安全。每个接口仍需验证：

- 字符串长度、字符集和枚举范围；
- 分页数量与游标边界；
- 令牌、角色和资源归属；
- 重复提交与幂等键；
- 资源 ID 是否存在且未删除；
- 文件类型、大小和目标路径。

把复用验证封装成抛出 `FailureException` / `UnauthorizedException` 的函数，可以让接口主体保持线性控制流。

## 线程与阻塞

引擎把接口 block 包在 `Coroutines.io` 中。数据库/文件 API 可以挂起调用，但长时间 CPU 工作仍应明确切到 CPU 调度器，并设置业务级超时。不要在进程全局可变集合上假设请求串行执行；共享房间、回调表或内存缓存需要互斥或原子设计。

## 协议演进

路由来自 API 属性名，参数来自数组位置。已经发布后：

- 重命名 `ApiAccountLogin` 会改变路由；
- 在中间插入参数会改变后续位置；
- 改变输出数量会让旧客户端解码失败。

长期兼容接口应新增 API 声明，或把可演进字段包进带默认值的 DTO。服务端可以同时注册新旧声明，在迁移窗口结束后再移除旧路由。
