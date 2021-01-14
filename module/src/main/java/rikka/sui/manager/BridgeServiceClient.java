package rikka.sui.manager;

import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;

import androidx.annotation.Nullable;

import java.util.List;

import moe.shizuku.server.IShizukuService;
import rikka.shizuku.ShizukuApiConstants;
import rikka.sui.model.AppInfo;
import rikka.sui.server.ServerConstants;
import rikka.sui.util.ParceledListSlice;

public class BridgeServiceClient {

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
        if (BridgeServiceClient.binder == binder) return;

        if (BridgeServiceClient.binder != null) {
            BridgeServiceClient.binder.unlinkToDeath(DEATH_RECIPIENT, 0);
        }

        if (binder == null) {
            BridgeServiceClient.binder = null;
            BridgeServiceClient.service = null;
        } else {
            BridgeServiceClient.binder = binder;
            BridgeServiceClient.service = IShizukuService.Stub.asInterface(binder);

            try {
                BridgeServiceClient.binder.linkToDeath(DEATH_RECIPIENT, 0);
            } catch (Throwable ignored) {
            }
        }
    }

    public static IShizukuService getService() {
        if (service == null) {
            setBinder(requestBinderFromBridge());
        }
        return service;
    }

    public static List<AppInfo> getApplications(int userId) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        List<AppInfo> result;
        try {
            data.writeInterfaceToken("moe.shizuku.server.IShizukuService");
            data.writeInt(userId);
            try {
                getService().asBinder().transact(ServerConstants.BINDER_TRANSACTION_getApplications, data, reply, 0);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            reply.readException();
            if ((0 != reply.readInt())) {
                //noinspection unchecked
                result = ParceledListSlice.CREATOR.createFromParcel(reply).getList();
            } else {
                result = null;
            }
        } finally {
            reply.recycle();
            data.recycle();
        }
        return result;
    }
}
