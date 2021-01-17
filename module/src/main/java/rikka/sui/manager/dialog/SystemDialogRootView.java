package rikka.sui.manager.dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import static rikka.sui.manager.ManagerConstants.LOGGER;

public class SystemDialogRootView extends FrameLayout {

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onClose();
            dismiss();
        }
    };
    private final WindowManager windowManager;

    public SystemDialogRootView(@NonNull Context context) {
        super(context);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void onClose() {

    }

    public boolean onBackPressed() {
        return true;
    }

    public final void show(WindowManager.LayoutParams lp) {
        try {
            windowManager.addView(this, lp);
        } catch (Exception e) {
            LOGGER.w(e, "addView");
        }
    }

    public final void dismiss() {
        try {
            windowManager.removeView(this);
        } catch (Throwable e) {
            LOGGER.w(e, "removeView");
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event);
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            getKeyDispatcherState().startTracking(event, this);
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            getKeyDispatcherState().handleUpEvent(event);

            if (event.isTracking() && !event.isCanceled()) {
                if (onBackPressed()) {
                    dismiss();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        try {
            try {
                getContext().registerReceiver(receiver, intentFilter);
                LOGGER.i("registerReceiver android.intent.action.CLOSE_SYSTEM_DIALOGS");
            } catch (Exception e) {
                LOGGER.w(e, "registerReceiver android.intent.action.CLOSE_SYSTEM_DIALOGS");
            }
        } catch (Throwable e) {
            LOGGER.w(e, "registerReceiver");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        try {
            getContext().unregisterReceiver(receiver);
        } catch (Throwable e) {
            LOGGER.w(e, "unregisterReceiver");
        }
    }
}
