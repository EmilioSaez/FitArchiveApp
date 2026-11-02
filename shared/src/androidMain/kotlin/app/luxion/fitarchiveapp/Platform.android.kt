package app.luxion.fitarchiveapp

actual fun platformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"