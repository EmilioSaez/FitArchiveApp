package com.emilio.fitarchive

actual fun platformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"