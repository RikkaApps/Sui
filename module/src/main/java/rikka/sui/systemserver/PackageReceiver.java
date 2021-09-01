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

package rikka.sui.systemserver;

import static rikka.sui.systemserver.SystemServerConstants.LOGGER;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextHidden;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandleHidden;

import dev.rikka.tools.refine.Refine;

public class PackageReceiver {

    private static final BroadcastReceiver RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            String pkgName = (uri != null) ? uri.getSchemeSpecificPart() : null;
            int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            LOGGER.d("%s: %s (%d)", intent.getAction(), pkgName, uid);
            Bridge.dispatchPackageChanged(intent);
        }
    };

    public static void register() {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            LOGGER.w("ActivityThread is null");
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        Handler handler = new Handler(Looper.getMainLooper());

        try {
            Refine.<ContextHidden>unsafeCast(ActivityThread.currentActivityThread().getSystemContext())
                    .registerReceiverAsUser(
                            RECEIVER,
                            Refine.unsafeCast(UserHandleHidden.ALL),
                            intentFilter,
                            null,
                            handler
                    );
            LOGGER.d("register package receiver");
        } catch (Throwable e) {
            LOGGER.w("registerReceiver failed", e);
        }
    }
}
