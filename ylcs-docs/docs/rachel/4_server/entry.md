下面是一个最简单的后端服务器代码，编写简单的`main`即可启动服务端。

## Main.kt

```kotlin
fun main() = object : ServerEngine() {
    override val port = 1211
    override val public = "public"
    override val APIScope.api get() = listOf()
}.run()
```

这里的`port`定义了服务器后端端口号，而`host`默认是启动在`localhost`上的。

想要了解更多服务器引擎相关配置请参阅后续章节。