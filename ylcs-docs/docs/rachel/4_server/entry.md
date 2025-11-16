下面是一个最简单的后端服务器代码，在`src`目录的`love.yinlin`包下创建`MainServerEngine.kt`文件，运行`love.yinlin.Mainkt`即可启动服务端。

!!! Warning
    `love.yinlin.MainServerEngine`包名和类名都是强制的，如果你的程序需要混淆，请保留此类名，因为服务器引擎需要通过反射获取到它。

## MainServerEngine.kt

```kotlin
@Suppress("unused", "unchecked_cast")
data object MainServerEngine : ServerEngine() {
    override val public = "public"
    override val APIScope.api get() = listOf()
}
```