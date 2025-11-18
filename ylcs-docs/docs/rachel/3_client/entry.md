客户端引擎配置十分简单，在你的程序启动时为引擎初始化`BASE_URL`即可。

```kotlin
fun main() {
    ClientEngine.init("https://api.my.webiste")
}
```

!!! Tip
    推荐将`BASE_URL`放置在公共模块，以便服务端能够共用相同的路径。