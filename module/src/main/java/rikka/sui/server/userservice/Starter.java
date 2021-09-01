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

package rikka.sui.server.userservice;

import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_TOKEN;

import android.app.ActivityThread;
import android.content.Context;
import android.content.ContextHidden;
import android.ddm.DdmHandleAppName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.util.Log;

import dev.rikka.tools.refine.Refine;
import moe.shizuku.server.IShizukuService;

public class Starter {

    private static final String TAG = "SuiUserServiceStarter";
    private static final int BRIDGE_TRANSACTION_CODE = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';
    private static final String BRIDGE_SERVICE_DESCRIPTOR = "android.app.IActivityManager";
    private static final String BRIDGE_SERVICE_NAME = "activity";
    private static final int BRIDGE_ACTION_GET_BINDER = 2;

    public static void main(String[] args) {
        String name = null;
        String token = null;
        String pkg = null;
        String cls = null;
        int uid = -1;

        for (String arg : args) {
            if (arg.startsWith("--debug-name=")) {
                name = arg.substring(13);
            } else if (arg.startsWith("--token=")) {
                token = arg.substring(8);
            } else if (arg.startsWith("--package=")) {
                pkg = arg.substring(10);
            } else if (arg.startsWith("--class=")) {
                cls = arg.substring(8);
            } else if (arg.startsWith("--uid=")) {
                uid = Integer.parseInt(arg.substring(6));
            }
        }

        int appId = uid % 100000;
        int userId = uid / 100000;

        Log.i(TAG, String.format("starting service %s/%s...", pkg, cls));

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        IBinder service = null;
        Context systemContext = ActivityThread.systemMain().getSystemContext();

        DdmHandleAppName.setAppName(name != null ? name : pkg + ":user_service", 0);

        try {
            UserHandle userHandle = Refine.unsafeCast(UserHandleHidden.of(userId));
            Context context = Refine.<ContextHidden>unsafeCast(systemContext)
                    .createPackageContextAsUser(pkg, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY, userHandle);
            ClassLoader classLoader = context.getClassLoader();
            Class<?> serviceClass = classLoader.loadClass(cls);
            service = (IBinder) serviceClass.newInstance();
        } catch (Throwable tr) {
            Log.w(TAG, String.format("unable to start service %s/%s...", pkg, cls), tr);
            System.exit(1);
        }

        if (!sendBinder(service, token)) {
            System.exit(1);
        }

        Looper.loop();
        System.exit(0);

        Log.i(TAG, String.format("service %s/%s exited", pkg, cls));
    }

    private static IBinder requestBinderFromBridge() {
        IBinder binder = ServiceManager.getService(BRIDGE_SERVICE_NAME);
        if (binder == null) return null;

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(BRIDGE_SERVICE_DESCRIPTOR);
            data.writeInt(BRIDGE_ACTION_GET_BINDER);
            binder.transact(BRIDGE_TRANSACTION_CODE, data, reply, 0);
            reply.readException();
            IBinder received = reply.readStrongBinder();
            if (received != null) {
                return received;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
        return null;
    }

    private static boolean sendBinder(IBinder binder, String token) {
        IShizukuService shizukuService = IShizukuService.Stub.asInterface(requestBinderFromBridge());
        if (shizukuService == null) {
            return false;
        }

        Bundle data = new Bundle();
        data.putString(USER_SERVICE_ARG_TOKEN, token);
        try {
            shizukuService.attachUserService(binder, data);
        } catch (Throwable e) {
            Log.w(TAG, Log.getStackTraceString(e));
            return false;
        }
        return true;
    }
}
