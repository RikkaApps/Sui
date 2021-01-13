package rikka.sui.ktx

import android.view.Window
import android.view.WindowManager

private val addSystemFlags = try {
    Window::class.java.getDeclaredMethod("addSystemFlags", Int::class.javaPrimitiveType)
} catch (e: Throwable) {
    null
}

private val privateFlagsField = try {
    WindowManager.LayoutParams::class.java.getDeclaredField("privateFlags")
} catch (e: Throwable) {
    null
}

val SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS: Int = try {
    WindowManager.LayoutParams::class.java.getDeclaredField("SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS").getInt(null)
} catch (e: Throwable) {
    0
}

fun Window.addSystemFlags(flags: Int) {
    if (flags == 0) return
    addSystemFlags?.invoke(this, flags)
}

var WindowManager.LayoutParams.privateFlags: Int
    get() = try {
        privateFlagsField?.getInt(this) ?: 0
    } catch (e: Throwable) {
        0
    }
    set(value) {
        try {
            privateFlagsField?.setInt(this, value)
        } catch (e: Throwable) {
        }
    }
