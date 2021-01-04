package app.rikka.sui.manager;

import android.app.ActivityThread;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;

import app.rikka.sui.ktx.HandlerKt;
import app.rikka.sui.util.UserHandleCompat;
import hidden.HiddenApiBridge;

import static app.rikka.sui.manager.ManagerConstants.LOGGER;
import static app.rikka.sui.server.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static app.rikka.sui.server.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;

public class PermissionConfirmation {

    private static final IBinder TOKEN = new Binder();

    public static void show(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        HandlerKt.getMainHandler().post(() -> showInternal(requestUid, requestPid, requestPackageName, requestCode));
    }

    private static void showInternal(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        Context context = ActivityThread.currentActivityThread().getApplication();
        if (context == null) {
            return;
        }

        String label = requestPackageName;
        int userId = UserHandleCompat.getUserId(requestUid);
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = HiddenApiBridge.PackageManager_getApplicationInfoAsUser(pm, requestPackageName, 0x00002000 /*MATCH_UNINSTALLED_PACKAGES*/, userId);
            label = ai.loadLabel(pm).toString();
        } catch (Throwable e) {
            LOGGER.e("getApplicationInfoAsUser");
        }

        DialogInterface.OnClickListener listener = (dialog, which) -> {
            Bundle data = new Bundle();
            data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, which == AlertDialog.BUTTON_POSITIVE || which == AlertDialog.BUTTON_NEUTRAL);
            data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, which == AlertDialog.BUTTON_NEUTRAL);

            try {
                BridgeServiceClient.getService().onPermissionConfirmationResult(requestUid, requestPid, requestCode, data);
            } catch (Throwable e) {
                LOGGER.e("onPermissionConfirmationResult");
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Superuser request")
                .setMessage("Grant full access of your device to " + label + ".")
                .setPositiveButton("Always allow", listener)
                .setNegativeButton("Always deny", listener)
                .setNeutralButton("Allow only this time", listener)
                .setCancelable(false)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().token = TOKEN;
            window.getAttributes().type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
        }

        dialog.show();
    }
}
