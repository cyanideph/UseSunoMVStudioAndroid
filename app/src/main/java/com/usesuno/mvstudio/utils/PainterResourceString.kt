package androidx.compose.ui.res

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/**
 * Compatibility overload allowing string-based icon resource references in Compose UI.
 */
@Composable
fun painterResource(name: String): Painter {
    val imageVector = when (name) {
        "ic_arrow_back" -> Icons.AutoMirrored.Filled.ArrowBack
        "ic_settings" -> Icons.Default.Settings
        "ic_close" -> Icons.Default.Close
        "ic_check" -> Icons.Default.Check
        "ic_play_arrow" -> Icons.Default.PlayArrow
        "ic_expand_less" -> Icons.Default.KeyboardArrowUp
        "ic_expand_more" -> Icons.Default.KeyboardArrowDown
        else -> Icons.Default.Add
    }
    return rememberVectorPainter(imageVector)
}
