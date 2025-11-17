通常服务端实现中，可能存在某些操作需要延迟响应，甚至条件响应，而不是在请求的时刻立即处理。

最常见的便是邮件系统，当用户发起注册请求时，此时的请求可能仅仅只是发送一份通知到后台管理员的邮件中，数据可能登记入库。

但实际用户的注册行为需要等到管理员审核同意申请后再实际返回，此时管理员端发送的通过请求，从数据库中取出记录数据后，该如何找到相应的处理函数呢？

```kotlin
val APIScope.callMap by lazy { buildCallBackMap<Type, Entry, String>() }
```

我们可以在`Main.kt`中定义一个全局变量，并使用`APIScope`作为接收器，这样我们可以在接口作用域下访问到`callMap`。

我们可以类似于接口一般在相同位置定义回调响应，这里的三个泛型参数分别代表用于区分回调的值、回调输入参数、回调输出参数，例如：

```kotlin
ApiAccountRegister.response {
    // 将用户注册数据存入数据库，并额外标记一个字段Type.Register
}

ApiSomeMailSystem.response {
    // 某种邮件系统实现，例如同意请求后，查找数据库中的Type字段和Entry字段
    val type = Type.fromValue(entry.type)!!
    val callback = callMap[type]!!
    callback(entry)
}

callMap[Type.Register] = { uid: Int ->
    // 通过
    "注册成功"
}
```

这里用户在注册时将用户数据和类型标记存入数据库后，待管理员端处理邮件请求时，可以从数据库中获取数据与标记，并根据标记对应的类型在回调表中查找对应的回调函数。
再通过传递给回调函数参数，将回调结果传回主接口中。

!!! Tip
    注意，此方法与服务器引擎实现无关，仅作为一种解决这类情景的方案。你完全可以自己实现其他方法。