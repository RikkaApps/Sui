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

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import androidx.annotation.NonNull;

import java.util.Arrays;

import rikka.sui.util.ParcelUtils;

public final class SystemProcess {

    private static final BridgeService SERVICE = new BridgeService();

    private static boolean execActivityTransaction(@NonNull Binder binder, int code, Parcel data, Parcel reply, int flags) {
        return SERVICE.onTransact(code, data, reply, flags);
    }

    public static boolean execTransact(@NonNull Binder binder, int code, long dataObj, long replyObj, int flags) {
        String descriptor = binder.getInterfaceDescriptor();

        if (!(SERVICE.isServiceDescriptor(descriptor) && SERVICE.isServiceTransaction(code)
                || "android.content.pm.ILauncherApps".equals(descriptor))) {
            return false;
        }

        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null) {
            return false;
        }

        boolean res;

        try {
            if (SERVICE.isServiceDescriptor(descriptor) && SERVICE.isServiceTransaction(code)) {
                res = execActivityTransaction(binder, code, data, reply, flags);
            } else {
                res = false;
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
    }
}
