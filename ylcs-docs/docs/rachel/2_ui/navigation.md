### Screen Pages and Navigation

```kotlin

class ScreenMain(manager: ScreenManager) : Screen(manager) {
    @Composable
    override fun Content() {
        PrimaryButton("go to greet page") {
            navigate(::ScreenGreet, "hello world!")
        }
    }
}

class ScreenGreet(manager: ScreenManager, message: String) : Screen(manager) {
    @Composable
    override fun Content() {
        Text(message)
        PrimaryButton("back to main page") {
            pop()
        }
    }
}

class MyApplication : PlatformApplication<MyApplication>() {
    @Composable
    override fun Content() {
        AppScreen<ScreenMain> {
            screen(::ScreenMain)
            screen(::ScreenGreet)
        }
    }
}

```