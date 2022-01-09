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
 * Copyright (c) 2022 Sui Contributors
 */

package rikka.sui.installer;

import android.annotation.TargetApi;
import android.content.pm.IPackageManager;
import android.content.pm.IShortcutService;
import android.content.pm.IShortcutServiceV31;
import android.os.Build;
import android.os.Handler;
import android.os.IUserManager;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.rikka.tools.refine.Refine;
import rikka.sui.shortcut.ShortcutConstants;

@TargetApi(Build.VERSION_CODES.O)
public class Uninstaller {

    private static final String TAG = "SuiUninstaller";

    private static void removeShortcuts() throws InterruptedException, RemoteException {
        IShortcutService shortcutService = null;
        IUserManager userManager = null;

        while (true) {
            //noinspection ConstantConditions
            if (shortcutService == null) {
                shortcutService = IShortcutService.Stub.asInterface(ServiceManager.getService("shortcut"));
            }
            if (userManager == null) {
                userManager = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
            }
            if (shortcutService != null
                    && userManager != null
                    && userManager.isUserUnlocked(0)) {
                break;
            }

            //noinspection BusyWait
            Thread.sleep(1000);
            Log.v(TAG, "wait 1s");
        }

        List<String> list = new ArrayList<>();
        list.add(ShortcutConstants.SHORTCUT_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Refine.<IShortcutServiceV31>unsafeCast(shortcutService).removeDynamicShortcuts(
                    "com.android.settings", list, 0);
        } else {
            shortcutService.removeDynamicShortcuts(
                    "com.android.settings", list, 0);
        }
    }

    public static void main(String[] args) throws IOException, ErrnoException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        Log.i(TAG, "main");

        //noinspection deprecation
        Os.setuid(1000);

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                removeShortcuts();
            } catch (Throwable e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            Log.i(TAG, "exit");
            System.exit(0);
        });

        Looper.loop();
    }
}
