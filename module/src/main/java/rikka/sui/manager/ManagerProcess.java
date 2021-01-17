package rikka.sui.manager;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

import java.nio.ByteBuffer;
import java.util.Arrays;

import moe.shizuku.server.IShizukuApplication;
import moe.shizuku.server.IShizukuService;
import rikka.sui.ktx.HandlerKt;
import rikka.sui.manager.dialog.ConfirmationDialog;
import rikka.sui.manager.dialog.ManagementDialog;
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
            LOGGER.i("showPermissionConfirmation: %d %d %s %d", requestUid, requestPid, requestPackageName, requestCode);
            ConfirmationDialog.show(requestUid, requestPid, requestPackageName, requestCode);
        }
    };

    private static final BroadcastReceiver SHOW_MANAGEMENT_RECEIVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LOGGER.i("showManagement: action=%s", intent != null ? intent.getAction() : "(null)");
            ManagementDialog.show();
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
            service.attachApplication(APPLICATION, "com.android.systemui");
            LOGGER.i("attachApplication");
        } catch (RemoteException e) {
            LOGGER.w(e, "attachApplication");
            HandlerKt.getWorkerHandler().postDelayed(ManagerProcess::sendToService, 1000);
        }
    }

    private static void registerListener() {
        Context context = null;
        try {
            context = ActivityThread.currentActivityThread().getApplication();
        } catch (Exception e) {
            LOGGER.w(e, "getApplication");
        }

        if (context == null) {
            LOGGER.w("application is null, wait 1s");
            HandlerKt.getWorkerHandler().postDelayed(ManagerProcess::registerListener, 1000);
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SECRET_CODE");
        //intentFilter.addAction("android.telephony.action.SECRET_CODE");
        intentFilter.addDataAuthority("784784", null);
        intentFilter.addDataScheme("android_secret_code");

        try {
            context.registerReceiver(SHOW_MANAGEMENT_RECEIVER, intentFilter,
                    "android.permission.CONTROL_INCALL_EXPERIENCE", null);
            LOGGER.i("registerReceiver android.provider.Telephony.SECRET_CODE");
        } catch (Exception e) {
            LOGGER.w(e, "registerReceiver android.provider.Telephony.SECRET_CODE");
        }
    }

    public static void main(String[] args, ByteBuffer[] buffers) {
        LOGGER.d("main: %s", Arrays.toString(args));
        LOGGER.d("buffers: %s", Arrays.toString(buffers));
        WorkerHandler.get().post(ManagerProcess::sendToService);
        WorkerHandler.get().postDelayed(ManagerProcess::registerListener, 1000);

        Xml.setBuffers(buffers);
    }
}
