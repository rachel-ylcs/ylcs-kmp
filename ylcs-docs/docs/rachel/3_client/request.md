如果你不了解如何创建接口清单，请先参看[服务端请求部分](../4_server/request.md)。

## APIMap.kt

```kotlin
val ApiTestHelloWorld by API.post.i<String>().o<String>()
```

假设我们已经在`APIMap`中定义了接口清单，客户端发送请求将十分容易。

我们只需要引用该接口调用`request`方法，传递与定义输入类型相同的参数即可发送请求，并收到与定义输出类型相同的服务器响应。

## Test.kt

```kotlin
ApiTestHelloWorld.request("Rachel") { output ->
    println(output)
}
```

上述例子将会输出`Rachel, hello world!`

`Rachel`框架为了方便发送客户端请求，设计了三种请求风格，它们可以互相替换，但是在不同的场景下某些方式可能写起来更轻松。

虽然框架在设计时我们完全可以使用返回值的方式请求并接受响应，例如：

```kotlin
println(ApiTestHelloWorld.request("Rachel")) // 实际上不允许这样
```

但我们并没有采取这样的设计，主要原因有以下几点：

1. 在`kotlin`语言层面上你无法同时获取多个返回值，使用`Pair`或手动构造`data class`呈现的繁琐度与我们初衷相违背。

2. 使用`lambda`来处理响应的好处在于将所谓的“返回值”转变为了`lambda`的参数，这样便能自由定义多个数量的返回值了，并且我们可以用`_`去标记不关心的值。

3. 如果直接返回值，那么我们将无法处理错误的情况。而现在`request`函数的返回值是`Throwable`, 在请求成功情况下我们无需关心，直接丢弃返回值。
而请求失败的情况下，我们也可以通过漂亮的`DSL`来实现错误处理。例如：
```kotlin
ApiTestHelloWorld.request("Rachel") { output ->
    println(output)
}.error { err -> err.printStackTrace() }
```

### 风格一：回调式

```kotlin
ApiTestHelloWorld.request("Rachel") { output ->
    println(output)
}
```

回调式写法最为容易，通常用于消耗响应结果的情况，不需要将结果留存执行后续任务。

这种风格`request`的返回值是错误处理，当然如果你只需要在`lambda`中执行相关操作，而不关心处理错误，则无需处理。

!!! Tip
    我知道你们关心回调地狱的事，事实上`request`的`lambda`回调式写法只是在语法层面上提供更好的编程效率，请求在作用域下仍然是**同步**的。
    位于`lambda`体外部的语句仍然是在`lambda`执行完后才开始执行，因为`request`的`block`参数也是`suspend`函数，**所以根本不存在嵌套回调**。


包含错误处理的示例如下：

```kotlin
ApiTestHelloWorld.request("Rachel") { output ->
    println(output)
}?.let { it.printStackTrace() }
println("我一定最后输出，无论请求成功或失败")
```

请求会先执行`lambda`体内的操作，如果发生了错误则立即返回错误，此时由`printStackTrace`打印错误堆栈。
无论成功失败与否，最后一条输出语句始终会等待前面请求包括`lambda`体中所有操作完成。

请求始终发生在`IO`的协程作用域，所以你无需手动切换协程上下文。

请求一定会捕获所有异常，所以你无需担心程序崩溃。

!!! Tip
    需要注意的是，你的请求参数并不在请求的错误处理范围内，如果在构造参数时发生异常则不会被请求处理。

### 风格二：返回式

```kotlin
val output = ApiTestHelloWorld.request("Rachel")
println(output.o1) // 输出 Rachel, Hello world!
```

返回式通常用于需要获取结果等待后续处理的情景，但实际上这种情景大多数可以完全被回调式覆盖，除非你在某些需要返回值的函数中发生请求。

返回式的结果是一个`Data`类包装，你可以区分其是`Data.Sucess`或`Data.Failure`来分别获取实际的值与错误。

要注意的是，不推荐使用返回式的原因正是因为函数很难同时返回多个值（在你不手动定义`data`类的前提下，这样会显得繁琐）。
所以`Data.Sucess`的结果仍然是一个`APIResult`的包装，它可以接受一个或多个不同类型的参数构造，在服务端返回也是如此。
你可以通过其成员依次访问每一个值。

### 风格三：返回 Null 式

```kotlin
val output = ApiTestHelloWorld.requestNull("Rachel")
println(output) // 输出 Rachel, Hello world! 或 null
```

有时候可能必须要使用风格二，但是我们对响应结果的错误处理并不关心，那么可以更加方便的直接使用`requestNull`来发送请求。

它把`Data<O1>`类包装改为`APIResult<O1>?`作为结果返回，配合`kotlin`可空表达式我们便可以更方便的书写。

!!! Tip
    特别的，对于返回值只有一个参数的情况，`APIResult<O1>?`直接退化成`O1?`类型。对于没有返回值的情况，我们也可以直接省略。