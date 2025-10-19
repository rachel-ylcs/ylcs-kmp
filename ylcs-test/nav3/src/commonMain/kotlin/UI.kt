import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable
object Home : NavKey

@Serializable
object Chat : NavKey

val backStack = mutableStateListOf<NavKey>(Home)

@Composable
fun UI() {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Home> {
                Column {
                    Text(text = "Home", modifier = Modifier.clickable {
                        backStack.add(Chat)
                    })
                    Button(onClick = { println(backStack) }) {
                        Text("Stack")
                    }
                }
            }
            entry<Chat> {
                Text(text = "Chat", modifier = Modifier.clickable {
                    backStack.add(Home)
                })
            }
        }
    )
}