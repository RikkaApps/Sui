package app.rikka.sui.demo;

import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;

import androidx.annotation.Nullable;

import moe.shizuku.server.IShizukuService;

public class Sui {

    private static IBinder binder;
    private static IShizukuService service;

    private static final int BRIDGE_TRANSACTION_CODE = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';
    private static final String BRIDGE_SERVICE_DESCRIPTOR = "android.app.IActivityManager";
    private static final String BRIDGE_SERVICE_NAME = "activity";
    private static final int BRIDGE_ACTION_GET_BINDER = 2;

    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        binder = null;
        service = null;
    };

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

    protected static void setBinder(@Nullable IBinder binder) {
        if (Sui.binder == binder) return;

        if (Sui.binder != null) {
            Sui.binder.unlinkToDeath(DEATH_RECIPIENT, 0);
        }

        if (binder == null) {
            Sui.binder = null;
            Sui.service = null;
        } else {
            Sui.binder = binder;
            Sui.service = IShizukuService.Stub.asInterface(binder);

            try {
                Sui.binder.linkToDeath(DEATH_RECIPIENT, 0);
            } catch (Throwable ignored) {
            }
        }
    }

    static {
        setBinder(requestBinderFromBridge());
    }

    public static IBinder getBinder() {
        return binder;
    }

    public static IShizukuService getService() {
        return service;
    }
}
