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

package rikka.sui.permission;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import rikka.sui.util.Logger;

public class SystemDialogRootView extends FrameLayout {

    private static final Logger LOGGER = new Logger("SystemDialogRootView");

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
            requestFocus();
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
