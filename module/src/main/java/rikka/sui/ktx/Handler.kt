package rikka.sui.ktx

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process

val mainHandler by lazy {
    Handler(Looper.getMainLooper())
}

private val workerThread by lazy(LazyThreadSafetyMode.NONE) {
    HandlerThread("Worker", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
}

val workerHandler by lazy {
    Handler(workerThread.looper)
}