## ComposeCore 模块

Compose核心模块封装了大部分UI工具函数与操作，对一些特定情景提供了`DSL`方便编写代码。

### (1) 状态DSL love.yinlin.compose.StateEx

核心将大部分Compose State代码封装成便于使用的形式, 减少样板代码的书写，例如：

```kotlin
val old1 = remember(key) { mutableStateOf(2) }
val new1 = rememberState(2)

val old2 = remember { mutableStateOf(false) }
val new2 = rememberFalse()

val old3 = remember(key) { derivedStateOf { someState.call() } }
val new3 = rememberDerivedState(key) { someState.call() }
```

### (2) 唯一启动 love.yinlin.compose.LaunchFlag

用于在Compose作用域下唯一启动某个函数。

### (3) 屏幕旋转感应 love.yinlin.OrientationController

仅用于移动端，当屏幕方向变化时触发。

### (4) 离屏感应 love.yinlin.compose.OffScreenEffect

当手机端划走页面或桌面端隐藏时以及恢复时触发。