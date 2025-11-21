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