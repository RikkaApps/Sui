package rikka.sui.manager;

import android.os.Handler;
import android.os.HandlerThread;

public class WorkerHandler {

    private static final HandlerThread HANDLER_THREAD;
    private static final Handler HANDLER;

    static {
        HANDLER_THREAD = new HandlerThread("SuiManager");
        HANDLER_THREAD.start();
        HANDLER = new Handler(HANDLER_THREAD.getLooper());
    }

    public static Handler get() {
        return HANDLER;
    }
}
