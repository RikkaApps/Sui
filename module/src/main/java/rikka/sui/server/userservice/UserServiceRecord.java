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

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;

import java.util.UUID;

import moe.shizuku.server.IShizukuServiceConnection;

import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_TRANSACTION_destroy;
import static rikka.sui.server.ServerConstants.LOGGER;

public class UserServiceRecord {

    public IBinder.DeathRecipient deathRecipient;
    public final boolean standalone;
    public final int versionCode;
    public String token;
    public IBinder binder;
    public boolean startScheduled;
    public final RemoteCallbackList<IShizukuServiceConnection> callbacks = new RemoteCallbackList<>();

    public UserServiceRecord(boolean standalone, int versionCode) {
        this.standalone = standalone;
        this.versionCode = versionCode;
        this.token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    public void setBinder(IBinder binder) {
        LOGGER.v("binder received for service record %s", token);
        this.binder = binder;
        broadcastBinderReceived();
    }

    public void broadcastBinderReceived() {
        LOGGER.v("broadcast received for service record %s", token);

        int count = callbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                callbacks.getBroadcastItem(i).connected(binder);
            } catch (Throwable e) {
                LOGGER.w("failed to call connected");
            }
        }
        callbacks.finishBroadcast();
    }

    public void destroy() {
        binder.unlinkToDeath(deathRecipient, 0);

        if (binder != null && binder.pingBinder()) {
            Parcel data = Parcel.obtain();
            try {
                data.writeInterfaceToken(binder.getInterfaceDescriptor());
                binder.transact(USER_SERVICE_TRANSACTION_destroy, data, null, Binder.FLAG_ONEWAY);
            } catch (Throwable e) {
                LOGGER.w(e, "failed to destroy");
            } finally {
                data.recycle();
            }
        }

        callbacks.kill();
    }
}
