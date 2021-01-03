package app.rikka.sui.systemserver;

import android.os.IBinder;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.rikka.sui.util.ParcelUtils;

import static app.rikka.sui.systemserver.Constants.LOGGER;

public final class SystemProcess {

    private static final SuiBridgeService SERVICE = new SuiBridgeService();

    private static final int V23_0 = 49;

    private final int versionCode;

    private IBinder activityService;

    private List<String> addedServices = new ArrayList<>();

    private SystemProcess(int versionCode) {
        this.versionCode = versionCode;
    }

    public static boolean execTransact(int code, long dataObj, long replyObj, int flags) {
        if (!SERVICE.isServiceTransaction(code)) {
            return false;
        }

        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null) {
            return false;
        }

        boolean res = false;

        try {
            String descriptor = ParcelUtils.readInterfaceDescriptor(data);
            data.setDataPosition(0);

            if (SERVICE.isServiceDescriptor(descriptor)) {
                res = SERVICE.onTransact(code, data, reply, flags);
            }
        } catch (Exception e) {
            if ((flags & IBinder.FLAG_ONEWAY) != 0) {
                LOGGER.w(e, "Caught a Exception from the binder stub implementation.");
            } else {
                reply.setDataPosition(0);
                reply.writeException(e);
            }
            res = true;
        }

        if (res) {
            if (data != null) data.recycle();
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
