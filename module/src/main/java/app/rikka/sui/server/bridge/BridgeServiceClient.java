package app.rikka.sui.server.bridge;

import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;

import java.lang.reflect.Field;
import java.util.Map;

import app.rikka.sui.server.Service;

import static app.rikka.sui.server.ServerConstants.LOGGER;

public class BridgeServiceClient {

    private static final int BRIDGE_TRANSACTION_CODE = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';
    private static final String BRIDGE_SERVICE_DESCRIPTOR = "android.app.IActivityManager";
    private static final String BRIDGE_SERVICE_NAME = "activity";

    private static final int ACTION_SEND_BINDER = 1;
    private static final int ACTION_GET_BINDER = ACTION_SEND_BINDER + 1;
    private static final int ACTION_NOTIFY_FINISHED = ACTION_SEND_BINDER + 2;

    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        LOGGER.i("service %s is dead.", BRIDGE_SERVICE_NAME);

        try {
            //noinspection JavaReflectionMemberAccess
            Field field = ServiceManager.class.getDeclaredField("sServiceManager");
            field.setAccessible(true);
            field.set(null, null);

            //noinspection JavaReflectionMemberAccess
            field = ServiceManager.class.getDeclaredField("sCache");
            field.setAccessible(true);
            Object sCache = field.get(null);
            if (sCache instanceof Map) {
                //noinspection rawtypes
                ((Map) sCache).clear();
            }
            LOGGER.i("clear ServiceManager");
        } catch (Throwable e) {
            LOGGER.w(e, "clear ServiceManager");
        }

        sendToBridge(true);
    };

    public interface Listener {

        void onSystemServerRestarted();

        void onResponseFromBridgeService(boolean response);
    }

    private static Listener listener;

    private static void sendToBridge(boolean isRestart) {
        IBinder bridgeService;
        do {
            bridgeService = ServiceManager.getService(BRIDGE_SERVICE_NAME);
            if (bridgeService != null && bridgeService.pingBinder()) {
                break;
            }

            LOGGER.i("service %s is not started, wait 1s.", BRIDGE_SERVICE_NAME);

            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (Throwable e) {
                LOGGER.w("sleep", e);
            }
        } while (true);

        if (isRestart && listener != null) {
            listener.onSystemServerRestarted();
        }

        try {
            bridgeService.linkToDeath(DEATH_RECIPIENT, 0);
        } catch (Throwable e) {
            LOGGER.w(e, "linkToDeath");
            sendToBridge(false);
            return;
        }

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean res = false;
        try {
            data.writeInterfaceToken(BRIDGE_SERVICE_DESCRIPTOR);
            data.writeInt(ACTION_SEND_BINDER);
            IBinder binder = Service.getInstance();
            LOGGER.v("binder %s", binder);
            data.writeStrongBinder(binder);
            res = bridgeService.transact(BRIDGE_TRANSACTION_CODE, data, reply, 0);
            reply.readException();
        } catch (Throwable e) {
            LOGGER.e(e, "send binder");
        } finally {
            data.recycle();
            reply.recycle();
        }

        if (listener != null) {
            listener.onResponseFromBridgeService(res);
        }
    }

    public static void send(Listener listener) {
        BridgeServiceClient.listener = listener;
        sendToBridge(false);
    }

    public static void notifyStarted() {
        IBinder bridgeService = ServiceManager.getService(BRIDGE_SERVICE_NAME);
        if (bridgeService == null) {
            return;
        }

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean res = false;
        try {
            data.writeInterfaceToken(BRIDGE_SERVICE_DESCRIPTOR);
            data.writeInt(ACTION_NOTIFY_FINISHED);
            res = bridgeService.transact(BRIDGE_TRANSACTION_CODE, data, reply, 0);
            reply.readException();
        } catch (Throwable e) {
            LOGGER.e(e, "notify started");
        } finally {
            data.recycle();
            reply.recycle();
        }

        if (res) {
            LOGGER.i("notify started");
        } else {
            LOGGER.w("notify started");
        }
    }
}
