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

import android.os.IBinder;
import android.os.Parcel;
import android.util.ArrayMap;

import java.util.Arrays;
import java.util.Map;

import rikka.sui.systemserver.launcherapps.LauncherAppsWrapper;
import rikka.sui.util.ParcelUtils;

public final class SystemProcess {

    private static final BridgeService SERVICE = new BridgeService();
    private static final Map<IBinder, LauncherAppsWrapper> LAUNCHER_APPS_WRAPPER_CACHE = new ArrayMap<>();

    private final int versionCode;

    private SystemProcess(int versionCode) {
        this.versionCode = versionCode;
    }

    public static boolean execTransact(IBinder binder, int code, long dataObj, long replyObj, int flags) {
        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null) {
            return false;
        }

        boolean res = false;

        try {
            String descriptor = ParcelUtils.readInterfaceDescriptor(data);
            data.setDataPosition(0);

            if (SERVICE.isServiceDescriptor(descriptor) && SERVICE.isServiceTransaction(code)) {
                res = SERVICE.onTransact(code, data, reply, flags);
            } else if ("android.content.pm.ILauncherApps".equals(descriptor)) {
                LauncherAppsWrapper wrapper = LAUNCHER_APPS_WRAPPER_CACHE.get(binder);
                if (wrapper == null) {
                    wrapper = new LauncherAppsWrapper(binder);
                    LAUNCHER_APPS_WRAPPER_CACHE.put(binder, wrapper);
                }
                res = wrapper.transact(code, data, reply, flags);
            }
        } catch (Exception e) {
            if ((flags & IBinder.FLAG_ONEWAY) != 0) {
                LOGGER.w(e, "Caught a Exception from the binder stub implementation.");
            } else {
                if (reply != null) {
                    reply.setDataPosition(0);
                    reply.writeException(e);
                }
            }
            res = false;
        } finally {
            data.setDataPosition(0);
            if (reply != null) reply.setDataPosition(0);
        }

        if (res) {
            data.recycle();
            if (reply != null) reply.recycle();
        }

        return res;
    }

    public static void main(String[] args) {
        LOGGER.d("main: %s", Arrays.toString(args));

        int versionCode = -1;
        if (args != null) {
            for (String arg : args) {
                if (arg.startsWith("--version-code=")) {
                    try {
                        versionCode = Integer.parseInt(arg.substring("--version-code=".length()));
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        LOGGER.d("version code %d", versionCode);
    }
}
