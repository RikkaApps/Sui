package rikka.sui.manager;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;

import java.nio.ByteBuffer;
import java.util.Arrays;

import moe.shizuku.server.IShizukuApplication;
import moe.shizuku.server.IShizukuService;
import rikka.sui.ktx.HandlerKt;
import rikka.sui.manager.res.Xml;

import static rikka.sui.manager.ManagerConstants.LOGGER;

public class ManagerProcess {

    private static final IShizukuApplication APPLICATION = new IShizukuApplication.Stub() {

        @Override
        public void bindApplication(Bundle data) {

        }

        @Override
        public void dispatchRequestPermissionResult(int requestCode, Bundle data) {

        }

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
            service.attachApplication(APPLICATION, "com.android.systemui");
        } catch (RemoteException e) {
            LOGGER.w(e, "attachApplication");
            HandlerKt.getWorkerHandler().postDelayed(ManagerProcess::sendToService, 1000);
        }
    }

    public static void main(String[] args, ByteBuffer[] buffers) {
        LOGGER.d("main: %s", Arrays.toString(args));
        LOGGER.d("buffers: %s", Arrays.toString(buffers));
        HANDLER.post(ManagerProcess::sendToService);

        Xml.setBuffers(buffers);
    }
}
