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

package rikka.sui.manager;

import static rikka.sui.manager.ManagerConstants.LOGGER;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import moe.shizuku.server.IShizukuApplication;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.ShizukuApiConstants;
import rikka.sui.resource.SuiApk;
import rikka.sui.server.ServerConstants;
import rikka.sui.shortcut.SuiShortcut;
import rikka.sui.util.BridgeServiceClient;

public class ManagerProcess {

    private static Intent intent;
    private static SuiApk suiApk;

    private static final IShizukuApplication APPLICATION = new IShizukuApplication.Stub() {

        @Override
        public void bindApplication(Bundle data) {

        }

        @Override
        public void dispatchRequestPermissionResult(int requestCode, Bundle data) {

        }

        @Override
        public void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) {
            LOGGER.i("showPermissionConfirmation: %d %d %s %d", requestUid, requestPid, requestPackageName, requestCode);

            if (suiApk == null) {
                LOGGER.e("Cannot load apk");
                return;
            }

            try {
                suiApk.getSuiRequestPermissionDialogConstructor().newInstance(
                        ActivityThread.currentActivityThread().getApplication(), suiApk.getResources(),
                        requestUid, requestPid, requestPackageName, requestCode
                );
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                LOGGER.e(e, "showPermissionConfirmation");
            }
        }
    };

    private static void showManagement() {
        Context context;
        try {
            context = ActivityThread.currentActivityThread().getApplication();
            if (intent == null) {
                intent = SuiShortcut.getIntent(context, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Throwable e) {
            LOGGER.w(e, "showManagement");
        }
    }

    private static final BroadcastReceiver SHOW_MANAGEMENT_RECEIVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LOGGER.i("showManagement: action=%s", intent != null ? intent.getAction() : "(null)");
            showManagement();
        }
    };

    private static void sendToService() {
        IShizukuService service = BridgeServiceClient.getService();
        if (service == null) {
            LOGGER.w("service is null, wait 1s");
            WorkerHandler.get().postDelayed(ManagerProcess::sendToService, 1000);
            return;
        }

        try {
            service.attachApplication(APPLICATION, "com.android.systemui");
            LOGGER.i("attachApplication");
        } catch (RemoteException e) {
            LOGGER.w(e, "attachApplication");
            WorkerHandler.get().postDelayed(ManagerProcess::sendToService, 1000);
        }

        suiApk = SuiApk.createForSystemUI();
    }

    private static void registerListener() {
        Context context = null;
        try {
            context = ActivityThread.currentActivityThread().getApplication();
        } catch (Throwable e) {
            LOGGER.w(e, "getApplication");
        }

        if (context == null) {
            LOGGER.w("application is null, wait 1s");
            WorkerHandler.get().postDelayed(ManagerProcess::registerListener, 1000);
            return;
        }

        WorkerHandler.get().post(ManagerProcess::sendToService);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SECRET_CODE");
        //intentFilter.addAction("android.telephony.action.SECRET_CODE");
        intentFilter.addDataAuthority("784784", null);
        intentFilter.addDataScheme("android_secret_code");

        try {
            context.registerReceiver(SHOW_MANAGEMENT_RECEIVER, intentFilter,
                    "android.permission.CONTROL_INCALL_EXPERIENCE", null);
            LOGGER.i("registerReceiver android.provider.Telephony.SECRET_CODE");
        } catch (Exception e) {
            LOGGER.w(e, "registerReceiver android.provider.Telephony.SECRET_CODE");
        }
    }

    public static void main(String[] args) {
        LOGGER.d("main: %s", Arrays.toString(args));
        WorkerHandler.get().postDelayed(ManagerProcess::registerListener, 5000);
    }
}
