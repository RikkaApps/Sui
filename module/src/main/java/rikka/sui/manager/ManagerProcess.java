package rikka.sui.manager;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;

import java.util.Arrays;

import moe.shizuku.server.IShizukuManager;
import moe.shizuku.server.IShizukuService;
import rikka.sui.ktx.HandlerKt;

import static rikka.sui.manager.ManagerConstants.LOGGER;

public class ManagerProcess {

    private static final IShizukuManager MANAGER = new IShizukuManager.Stub() {

        @Override
        public void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) {
            PermissionConfirmation.show(requestUid, requestPid, requestPackageName, requestCode);
        }
    };

    private static final HandlerThread HANDLER_THREAD;
    private static final Handler HANDLER;

    static {
        HANDLER_THREAD = new HandlerThread("SuiManager");
        HANDLER_THREAD.start();
        HANDLER = new Handler(HANDLER_THREAD.getLooper());
    }

    private static void sendToService() {
        IShizukuService service = BridgeServiceClient.getService();
        if (service == null) {
            LOGGER.w("service is null, wait 1s");
            HandlerKt.getWorkerHandler().postDelayed(ManagerProcess::sendToService, 1000);
            return;
        }

        try {
            service.attachManager(MANAGER);
        } catch (RemoteException e) {
            LOGGER.w(e, "attachManager");
            HandlerKt.getWorkerHandler().postDelayed(ManagerProcess::sendToService, 1000);
        }
    }

    public static void main(String[] args) {
        LOGGER.d("main: %s", Arrays.toString(args));
        HANDLER.post(ManagerProcess::sendToService);
    }
}
