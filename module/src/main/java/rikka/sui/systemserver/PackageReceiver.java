package rikka.sui.systemserver;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import rikka.sui.util.Unsafe;

import static rikka.sui.systemserver.SystemServerConstants.LOGGER;

public class PackageReceiver {

    private static final BroadcastReceiver RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            String pkgName = (uri != null) ? uri.getSchemeSpecificPart() : null;
            int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            LOGGER.d("%s: %s (%d)", intent.getAction(), pkgName, uid);
            Bridge.dispatchPackageChanged(intent);
        }
    };

    public static void register() {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            LOGGER.w("ActivityThread is null");
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        Handler handler = new Handler(Looper.getMainLooper());

        try {
            Unsafe.<$android.content.Context>unsafeCast(ActivityThread.currentActivityThread().getSystemContext())
                    .registerReceiverAsUser(
                            RECEIVER,
                            Unsafe.unsafeCast($android.os.UserHandle.ALL),
                            intentFilter,
                            null,
                            handler
                    );
            LOGGER.d("register package receiver");
        } catch (Throwable e) {
            LOGGER.w("registerReceiver failed", e);
        }
    }
}
