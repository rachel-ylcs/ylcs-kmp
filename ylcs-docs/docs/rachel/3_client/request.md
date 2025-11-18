如果你不了解如何创建接口清单，请参看[服务端请求部分](../4_server/request.md)。

## APIMap.kt

```kotlin
val ApiTestHelloWorld by API.post.i<String>().o<String>()
```

假设我们已经在`APIMap`中定义了接口清单，客户端发送请求将十分容易。

我们只需要引用该接口调用`request`方法，传递与定义输入类型相同的参数即可发送请求，并收到与定义输出类型相同的服务器响应。

```kotlin
ApiTestHelloWorld.request("Rachel") { output ->
    println(output)
}
```

上述例子将会输出`Rachel, hello world!`

值得注意的是：

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

我知道你们关心回调地狱的事，事实上`request`的`lambda`回调式写法只是在语法层面上提供更好的编程效率，请求在作用域下仍然是**同步**的。
位于`lambda`体外部的语句仍然是在`lambda`执行完后才开始执行，因为`request`是`suspend`函数，**所以根本不存在嵌套回调**。
    
`request`的返回值是一个包装了错误与结果的联合类型，当然如果你只需要在`lambda`中执行相关操作，而不关心处理响应之后的值，你无需处理这个结果。
如果你希望得到处理响应结果后的值，可以调用`result`成员获取，类似的你可以使用`error`来处理错误。

所以你完全可以写出如下的代码：

```kotlin
val result = ApiTestHelloWorld.request("First") { output ->
    output
}.error { err -> err.printStackTrace() }.result
println(result)

ApiTestHelloWorld.request("Second") { output ->
    println(output)
}.error { err -> err.printStackTrace() }

println("我一定最后输出，无论请求成功或失败")
```

!!! Tip
    要注意的是，如果你使用了`error`后再获取`result`，那么你的`result`将是O?类型了。

这个示例的结果一定是输出：

```text
First, hello world!
Scond, hello world!
我一定最后输出，无论请求成功或失败
```
