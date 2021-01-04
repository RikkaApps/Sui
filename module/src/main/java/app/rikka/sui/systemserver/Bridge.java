package app.rikka.sui.systemserver;

import android.content.Intent;

import moe.shizuku.server.IShizukuService;

import static app.rikka.sui.systemserver.SystemServerConstants.LOGGER;

public class Bridge {

    public static void onPackageChanged(Intent intent) {
        IShizukuService service = BridgeService.get();
        if (service == null) {
            LOGGER.d("binder is null");
            return;
        }

        try {
            service.onPackageChanged(intent);
        } catch (Throwable e) {
            LOGGER.w(e, "onPackageChanged");
        }
    }

    public static boolean isHidden(int uid) {
        IShizukuService service = BridgeService.get();
        if (service == null) {
            LOGGER.d("binder is null");
            return false;
        }

        try {
            return service.isHidden(uid);
        } catch (Throwable e) {
            LOGGER.w(e, "isHidden");
            return false;
        }
    }
}
