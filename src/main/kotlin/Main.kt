import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.jdisktree.ui.App

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 900.dp)
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "J-DiskTree - Disk Space Analyzer"
    ) {
        App()
    }
}
