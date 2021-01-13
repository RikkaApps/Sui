package rikka.sui.ktx

import android.view.View
import android.widget.TextView
import rikka.sui.manager.res.Strings

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
                if (message != null && format != 0) text = String.format(Strings.get(Strings.brackets_format), message, countdown.toString())
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