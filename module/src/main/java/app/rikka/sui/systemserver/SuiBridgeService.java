package app.rikka.sui.systemserver;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import static app.rikka.sui.systemserver.Constants.LOGGER;

public class SuiBridgeService {

    private static final String DESCRIPTOR = "android.app.IActivityManager";
    private static final int TRANSACTION = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';

    private static final int ACTION_SEND_BINDER = 1;
    private static final int ACTION_GET_BINDER = ACTION_SEND_BINDER + 1;
    private static final int ACTION_NOTIFY_FINISHED = ACTION_SEND_BINDER + 2;

    private static IBinder serviceBinder;
    private static boolean serviceStarted;

    public static IBinder getServiceBinder() {
        return serviceBinder;
    }

    public static boolean isServiceStarted() {
        return serviceStarted;
    }

    private void sendBinder(IBinder binder) {
        if (binder != null) {
            LOGGER.i("binder received");

            serviceBinder = binder;
            try {
                serviceBinder.linkToDeath(() -> {
                    serviceBinder = null;
                    LOGGER.i("service is dead");
                }, 0);
            } catch (RemoteException ignored) {
            }
        } else {
            LOGGER.w("received empty binder");
        }
    }

    public boolean isServiceDescriptor(String descriptor) {
        return Objects.equals(DESCRIPTOR, descriptor);
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
                    sendBinder(data.readStrongBinder());
                    if (reply != null) {
                        reply.writeNoException();
                    }
                    return true;
                }
                break;
            }
            case ACTION_GET_BINDER: {
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
