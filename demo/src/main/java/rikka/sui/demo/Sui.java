package rikka.sui.demo;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;

import androidx.annotation.Nullable;

import moe.shizuku.server.IShizukuApplication;
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

    private static final IShizukuApplication SHIZUKU_APPLICATION = new IShizukuApplication.Stub() {

        @Override
        public void bindApplication(Bundle data) {
            if (data == null) {
                return;
            }


        }

        @Override
        public void dispatchRequestPermissionResult(int requestCode, Bundle data) {
            if (data == null) {
                return;
            }
        }

        @Override
        public void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) {

        }
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

    protected static void setBinder(@Nullable IBinder newBinder) {
        if (binder == newBinder) return;

        if (binder != null) {
            binder.unlinkToDeath(DEATH_RECIPIENT, 0);
        }

        if (newBinder == null) {
            binder = null;
            service = null;
        } else {
            binder = newBinder;
            service = IShizukuService.Stub.asInterface(newBinder);

            try {
                binder.linkToDeath(DEATH_RECIPIENT, 0);
            } catch (Throwable ignored) {
            }

            try {
                service.attachApplication(SHIZUKU_APPLICATION, BuildConfig.APPLICATION_ID);
            } catch (Throwable e) {
                e.printStackTrace();
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
