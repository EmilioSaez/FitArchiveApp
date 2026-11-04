package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberReducirMovimiento(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
