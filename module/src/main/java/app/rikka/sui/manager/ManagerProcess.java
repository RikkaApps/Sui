package app.rikka.sui.manager;

import android.os.RemoteException;

import java.util.Arrays;

import app.rikka.sui.ktx.HandlerKt;
import moe.shizuku.server.IShizukuManager;
import moe.shizuku.server.IShizukuService;

import static app.rikka.sui.manager.ManagerConstants.LOGGER;

public class ManagerProcess {

    private static final IShizukuManager MANAGER = new IShizukuManager.Stub() {

        @Override
        public void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) {
            PermissionConfirmation.show(requestUid, requestPid, requestPackageName, requestCode);
        }
    };

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
        HandlerKt.getWorkerHandler().post(ManagerProcess::sendToService);
    }
}
