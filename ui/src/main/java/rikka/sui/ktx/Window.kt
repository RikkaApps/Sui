/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

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
