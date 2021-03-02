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

import android.view.View
import android.widget.TextView
import rikka.sui.resource.Res
import rikka.sui.resource.Strings

private const val tag_countdown = 1599296841

fun TextView.applyCountdown(countdownSecond: Int, message: CharSequence? = null, format: Int = 0) {
    val countdownRunnable = object : Runnable {
        override fun run() {
            val countdown = getTag(tag_countdown) as Int
            setTag(tag_countdown, countdown - 1)
            if (countdown == 0) {
                isEnabled = true
                if (message != null) text = message
            } else {
                isEnabled = false
                if (message != null && format != 0) text = String.format(Strings.get(Res.string.brackets_format), message, countdown.toString())
                postDelayed(this, 1000)
            }
        }
    }

    val attached = isAttachedToWindow

    setTag(tag_countdown, countdownSecond)
    if (attached) {
        countdownRunnable.run()
    }

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            if (!attached) {
                countdownRunnable.run()
            }
        }

        override fun onViewDetachedFromWindow(view: View) {
            removeOnAttachStateChangeListener(this)
            removeCallbacks(countdownRunnable)
        }
    })
}