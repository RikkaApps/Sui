package rikka.sui.ktx

import android.os.Handler
import android.os.Looper

val mainHandler by lazy {
    Handler(Looper.getMainLooper())
}