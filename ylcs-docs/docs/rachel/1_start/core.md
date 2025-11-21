`Rachel`框架的核心主要包括三个，分别位于`core`、`compose-core`、`cs-core`三个模块，主要提供了更底层的类型设置、数据结构和语法糖形式。

其中`core`是最底层的核心模块，`compose-core`是UI层模块，`cs-core`是C/S交互层模块。

## Core 模块

核心模块封装了大部分工具函数与操作，对一些特定情景提供了`DSL`方便编写代码，如无特殊说明，大部分函数均是异常安全的，发生错误时仅返回空值。

### (1) 平台 love.yinlin.platform.Platform

核心提供了全局字段`platform`用于判断当前环境处于哪个平台, 并提供了平台`DSL`语法来允许函数仅在某些平台上调用。

平台列表如下：

- Android

- IOS

- Windows

- Linux

- MacOS

- WebWasm

其中`Android`, `IOS`还归属于`Phone`，`Windows`, `Linux`, `MacOS`归属于`Desktop`。

例如如果你希望只在`Android`上输出`hello world`则可以这样写：

```kotlin
Platform.use(Platform.Android) {
    println("hello world")
}
```

### (2) 协程 love.yinlin.platform.Coroutines

核心封装了`kotlinx-coroutines`库，并提供一系列协程`DSL`方便开发使用。

其中对每个平台的协程上下文都以`main`、`io`、`cpu`、`wait`做区分, 如果平台不存在该上下文则会默认使用`cpu`。

`main`协程上下文主要用于UI更新，`io`主要用于外部读写，`cpu`主要用于大量计算，`wait`创建的协程将不会被取消。

1. 协程上下文全局字段

    `mainContext`, `cpuContext`, `ioContext`, `waitContext`

2. 非协程作用域启动
    
    `startMain`, `startCPU`, `startIO`, `startWait`

3. 协程作用域启动新协程

    `main`, `cpu`, `io`, `wait`

4. 协程上下文切换

    `with`

核心还提供了一种将异步回调修改为协程同步调用的方式，例如某些外部或第三方库函数可能要求你提供若干个回调函数，
它们实现了功能并返回结果是发生在异步回调中的，你不知道回调什么时候触发，但当前线程上无法等待这个回调停止。

此时便可以使用`sync`同步转化，例子如下。

```kotlin

// 回调形式
fun someOuterFunction() {
    someOuterVariable.setListener(object : someListener {
        override fun onSuccess(someResult: String) {
            println("我不知道什么时候返回 $someResult")
        }
        override fun onError() {
            println("我不知道什么时候发生错误")
        }
    })
    
    println("此处我无法获取回调结果，我需要在回调中继续处理，层层嵌套会产生回调地狱")
}

// 同步形式
fun someOuterFunction() {
    val result = Coroutines.sync { future ->
        someOuterVariable.setListener(object : someListener {
            override fun onSuccess(someResult: String) {
                future.send(someResult) // return someResult
            }
            override fun onError() {
                future.send() // return null
            }
        })
    }
    // 一直等到回调触发future.send，result的类型是结果的可空类型，当使用send()时返回null
    println(result)
}
```

!!! Warning
    有时候在回调中可能发生异常，如果你没有对future使用send，那么协程将一直阻塞在此处消耗资源，为了防止这种情况出现应该使用future.catching包裹确保至少使用send。

### (3) URI love.yinlin.uri.Uri

核心提供了跨平台的URI实现，并提供了与各平台原生的URI类互相转换方法。

同时定义了ImplicitUri隐式接口对需要权限包装或沙盒引用的Android Content Uri或iOS Sandbox Uri等类做了包装。

### (4) 语言拓展 love.yinlin.extension.KotlinEx

核心将常规异常处理操作封装成`catching`系列函数，对于大部分不关心异常的场景可以极简化代码。

### (5) 集合拓展 love.yinlin.extension.CollectionEx

核心提供了若干集合自身操作的拓展函数。

### (6) 日期时间拓展 love.yinlin.extension.DateEx

核心提供了日期、时间、时间戳整数三者间互相转化的拓展函数，并提供了获取当前时间或日期的方法。

Formatter类可以快速将日期时间与字符串间互相转换。

### (8) 文件拓展 love.yinlin.extension.PathEx

核心封装了`kotlinx-io`库，并为基础的路径`Path`类提供了一系列拓展函数方便文件信息读取、位置操作、读写等。

### (8) Json拓展 love.yinlin.extension.JsonEx

核心封装了`kotlinx-json`库，提供了`Serializable Object`、`String`、`JsonElement`三者之间互相的转换, 它们全部由拓展函数实现。

你无需关心隐藏在后面的Json层，例如：

```kotlin
@Serializable
data class Student(
    val name: String,
    val age: Int
)

val student = Student("Alice", 18)
val text = student.toJsonString() // 转成Json文本
println(text.parseJsonValue<Student>()) // 转回对象
val json = text.parseJson // 转换JsonElement
println(json.to<Student>()) // 转回对象
```

核心还提供了快速构建Json数组或Json对象的`DSL`，例如：

```kotlin
val myArray = makeArray {
    add(2)
    add("hello")
    arr {
        add("child array")
    }
    obj {
        "name" with "Alice"
        "age" with 18
    }
}

val myObject = makeObject {
    "enabled" with true
    arr("keywords") {
        add(2)
        add("world")
    }
    obj("data") {
        "name" with "Alice"
        "age" with 18
    }
}
```