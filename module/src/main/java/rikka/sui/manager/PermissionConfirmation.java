package rikka.sui.manager;

import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import rikka.sui.ktx.HandlerKt;
import rikka.sui.util.UserHandleCompat;
import hidden.HiddenApiBridge;

import static rikka.sui.manager.ManagerConstants.LOGGER;
import static rikka.sui.server.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.sui.server.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;

public class PermissionConfirmation {

    private static final IBinder TOKEN = new Binder();

    public static void show(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        HandlerKt.getMainHandler().post(() -> showInternal(requestUid, requestPid, requestPackageName, requestCode));
    }

    private static void setResult(int requestUid, int requestPid, int requestCode, boolean allowed, boolean onetime) {
        Bundle data = new Bundle();
        data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed);
        data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime);

        try {
            BridgeServiceClient.getService().onPermissionConfirmationResult(requestUid, requestPid, requestCode, data);
        } catch (Throwable e) {
            LOGGER.e("onPermissionConfirmationResult");
        }
    }

    private static void showInternal(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        Context application = ActivityThread.currentActivityThread().getApplication();
        if (application == null) {
            return;
        }
        boolean isNight = (application.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) != 0;

        Context context = new ContextThemeWrapper(application, isNight ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert);

        String label = requestPackageName;
        int userId = UserHandleCompat.getUserId(requestUid);
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = HiddenApiBridge.PackageManager_getApplicationInfoAsUser(pm, requestPackageName, 0x00002000 /*MATCH_UNINSTALLED_PACKAGES*/, userId);
            label = ai.loadLabel(pm).toString();
        } catch (Throwable e) {
            LOGGER.e("getApplicationInfoAsUser");
        }

        PermissionConfirmationLayout layout = new PermissionConfirmationLayout(context, label);
        View root = layout.getRoot();

        Dialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);

        layout.getAllowButton().setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, true, false);
            dialog.dismiss();
        });
        layout.getOnetimeButton().setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, true, true);
            dialog.dismiss();
        });
        layout.getDenyButton().setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, false, false);
            dialog.dismiss();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.flags = 0;
            lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
            lp.token = TOKEN;
            window.setAttributes(lp);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            /*View decorView = window.getDecorView();
            if (decorView != null) {
                window.setBackgroundDrawable(layout.getBackground());
            }*/
        }

        dialog.show();
    }
}
