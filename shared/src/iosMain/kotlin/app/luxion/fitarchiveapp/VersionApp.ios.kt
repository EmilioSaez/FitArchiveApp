package app.luxion.fitarchiveapp

import androidx.compose.runtime.Composable
import platform.Foundation.NSBundle

@Composable
actual fun rememberVersionApp(): String {
    val info = NSBundle.mainBundle
    val version = info.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    val build = info.objectForInfoDictionaryKey("CFBundleVersion") as? String
    return when {
        version == null -> "desconocida"
        build == null -> version
        else -> "$version ($build)"
    }
}
