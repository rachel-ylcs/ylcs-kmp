### First Rachel App

```kotlin

fun main() = MyApplicaiton().run()

class MyApplication : PlatformApplication<MyApplication>(mApp) {
    @Composable
    override fun Content() {
        Text("hello world!")
    }
}

private val mApp = LazyReference<RachelApplication>()
val app: RachelApplication by mApp

```

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

### Floating Component

```kotlin

class ScreenMain(manager: ScreenManager) : Screen(manager) {
    @Composable
    override fun Content() {
        PrimaryButton("open dialog") {
            val result = dialog.openSuspend()
            println("the input text is $result")
        }
        PrimaryButton("open sheet") {
            sheet.open()
        }
        PrimaryButton("open sheet") {
            // application floating tip
            slot.tip.warning("open the tip")
        }
    }

    // applicaiton dialog
    val dialog = FloatingDialogInput()
    
    // application side sheet
    val sheet = object : FloatingSheet() {
        @Composable
        override fun Content() {
            Text("from sheet")
        }
    }

    // application fab
    override val fabIcon: ImageVector = Icons.outlined.Submit

    override suspend fun onFabClick() {
        println("from fab button")
    }
    
    @Composable
    override fun Floating() {
        sheet.Land()
        dialog.Land()
    }
}

```

### Service Startup

```kotlin

// All services are automatically registered.
// Services can be accessed from any location globally
// Services can set priorities to resolve dependency relationships
// Services can be set to start synchronously or asynchronously
class MyApplication : PlatformApplication<MyApplication>() {
    // bind os service, then you can use os module functions and variables
    val os by service(::StartupOS)
    // bind picker service, then you can open native system dialog, pick the file, pick a photo...
    val picker by service(::StartupPicker)
    // bind config service, then you can read or write application config in disk
    val config by service(::StartupAppConfig)
    @Composable
    override fun Content() {
        PrimaryButton(text = os.storage.dataPath) {
            picker.pickPhoto()
        }
    }
}

```