让我们来看第一节中的示例，仔细分解代码。

```kotlin
fun main() = object : ServerEngine() {
    override val port = 1211
    override val public = "public"
    override val APIScope.api get() = listOf(::helloAPI)
}.run()
```

这里是你的服务端入口，你可以在这里定义服务端的配置与接口清单。

### 1. public

你可以指定服务端静态资源路径，资源文件或图片均在此处存储。

例如指定为字符串`public`，则`localhost:port/public/`就成为静态资源的目录了。

!!! Tip
    由于前后端自动绑定，最好在外部（例如C/S公共代码处）定义资源路径名称，防止前后端访问的路径不同。

你还可以重写属性`isCopyResources`为`true`使得服务器启动前先将`jar`中`resources`里`public`对应的初始目录全部写出到服务器运行目录环境下。
如果已经存在相应目录则不会写出。

### 2. api

在此处定义你的所有接口清单。

## APIMap.kt

```kotlin
val ApiTestHelloWorld by API.post.i<String>().o<String>()
```

在这里定义你的前后端交互接口与数据结构。

!!! Warning
    为了前后端能够使用相同的数据结构，极大程度提高开发效率，必须将`APIMap.kt`放在前后端的共享模块中。

上述代码使用了API来委托了接口，其中接口名称定义是硬性要求的。

!!! Warning
    API名称必须由三部分构成，由Api起始，中间是一个首字母大写的单词，后面是首字母大写的若干单词。

这样定义的接口在请求时路径会自动被解析成`/test/helloWorld`。

!!! Tip
    `kotlin`的委托是保留`property`名称的，所以一切都是自动的，你无需在混淆时保留接口名。

这里使用了`API.post`来表示这是一个`POST`请求，`i`表示输入，`o`表示输出。

你需要为接口提供所有输入输出参数的类型，这对你的代码提高安全性强而有力。输入输出参数类型均可以为空，表示请求或响应中可能不包含此参数。

例如本例表示这个接口请求时需要携带一个`String`类型的参数，而响应接受一个`String`类型的参数。

!!! Tip
    如果你需要标注更多的接口文档信息，可以使用`@APIParam`和`@APIReturn`等注解标注。注解仅在源码层面起提示作用，不参与任何编译过程。

## HelloAPI.kt

```kotlin
fun APIScope.helloAPI() {
    ApiTestHelloWorld.response { name ->
        result("$name, hello world!")
    }
}
```

在`ServerEngine`的`api`中接口是由`listOf`指定的，这是为了更好的分类接口，例如你可以将归属于不同类别的接口放置在不同的文件中。

通常把相关功能的接口统一放在一个API文件下，在这个API文件中你可以定义多个请求响应处理函数。

只需要对`APIMap`中定义的委托调用`response`并搭配`DSL`即可写出漂亮的响应。

`lambda`中的参数会由编译器自动检查并提示类型，返回值你可以通过`result`成功传递回去，或者`failure`等异常处理函数返回错误信息，同样`result`能接受的参数类型与个数也与委托定义相同。

是的，一切都是自动的，都是强类型安全的。

这个示例的结果是，客户端向服务端路径`/test/helloWorld`发送POST请求，并携带参数`Rachel`，服务端将会返回`Rachel, hello world!`。