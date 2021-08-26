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
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import moe.shizuku.server.IShizukuService;

public class BridgeService {

    private static final String DESCRIPTOR = "android.app.IActivityManager";
    private static final int TRANSACTION = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';

    private static final int ACTION_SEND_BINDER = 1;
    private static final int ACTION_GET_BINDER = ACTION_SEND_BINDER + 1;
    private static final int ACTION_NOTIFY_FINISHED = ACTION_SEND_BINDER + 2;

    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        serviceBinder = null;
        service = null;
        LOGGER.i("service is dead");
    };

    private static IBinder serviceBinder;
    private static IShizukuService service;
    private static boolean serviceStarted;

    public static IShizukuService get() {
        return service;
    }

    public static boolean isServiceStarted() {
        return serviceStarted;
    }

    private void sendBinder(IBinder binder) {
        if (binder == null) {
            LOGGER.w("received empty binder");
            return;
        }

        if (serviceBinder == null) {
            PackageReceiver.register();
        } else {
            serviceBinder.unlinkToDeath(DEATH_RECIPIENT, 0);
        }

        serviceBinder = binder;
        service = IShizukuService.Stub.asInterface(serviceBinder);
        try {
            serviceBinder.linkToDeath(DEATH_RECIPIENT, 0);
        } catch (RemoteException ignored) {
        }

        LOGGER.i("binder received");
    }

    public boolean isServiceTransaction(int code) {
        return code == TRANSACTION;
    }

    public boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) {
        data.enforceInterface(DESCRIPTOR);

        int action = data.readInt();
        LOGGER.d("onTransact: action=%d, callingUid=%d, callingPid=%d", action, Binder.getCallingUid(), Binder.getCallingPid());

        switch (action) {
            case ACTION_SEND_BINDER: {
                if (Binder.getCallingUid() == 0) {
                    IBinder binder = data.readStrongBinder();
                    long identity = Binder.clearCallingIdentity();
                    try {
                        sendBinder(binder);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                    if (reply != null) {
                        reply.writeNoException();
                    }
                    return true;
                }
                break;
            }
            case ACTION_GET_BINDER: {
                if (Bridge.isHidden(Binder.getCallingUid())) {
                    return false;
                }

                if (reply != null) {
                    reply.writeNoException();
                    LOGGER.d("saved binder is %s", serviceBinder);
                    reply.writeStrongBinder(serviceBinder);
                }
                return true;
            }
            case ACTION_NOTIFY_FINISHED: {
                if (Binder.getCallingUid() == 0) {
                    serviceStarted = true;

                    if (reply != null) {
                        reply.writeNoException();
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
