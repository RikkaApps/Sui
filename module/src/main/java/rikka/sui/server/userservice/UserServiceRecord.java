package rikka.sui.server.userservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;

import java.util.UUID;

import moe.shizuku.server.IShizukuServiceConnection;

import static rikka.sui.server.ServerConstants.LOGGER;
import static rikka.sui.server.ShizukuApiConstants.USER_SERVICE_TRANSACTION_destroy;

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
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(binder.getInterfaceDescriptor());
                binder.transact(USER_SERVICE_TRANSACTION_destroy, data, reply, Binder.FLAG_ONEWAY);
            } catch (Throwable e) {
                LOGGER.w(e, "failed to destroy");
            } finally {
                data.recycle();
                reply.recycle();
            }
        }

        callbacks.kill();
    }
}
