package rikka.sui.shell;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.system.Os;
import android.text.TextUtils;

import rikka.rish.Rish;
import rikka.rish.RishConfig;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuApiConstants;
import rikka.sui.Sui;

public class Shell extends Rish {

    @Override
    public void requestPermission(Runnable onGrantedRunnable) {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            onGrantedRunnable.run();
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            System.err.println("Permission denied");
            System.err.flush();
            System.exit(1);
        } else {
            Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    Shizuku.removeRequestPermissionResultListener(this);

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        onGrantedRunnable.run();
                    } else {
                        System.err.println("Permission denied");
                        System.err.flush();
                        System.exit(1);
                    }
                }
            });
            Shizuku.requestPermission(0);
        }
    }

    public static void main(String[] args) {
        String packageName;
        if (Os.getuid() == 2000) {
            packageName = "com.android.shell";
        } else {
            packageName = System.getenv("RISH_APPLICATION_ID");
            if (TextUtils.isEmpty(packageName) || "PKG".equals(packageName)) {
                System.err.println("RISH_APPLICATION_ID is not set, set this environment variable to the id of current application (package name)");
                System.err.flush();
                System.exit(1);
            }
        }

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        Handler handler = new Handler(Looper.getMainLooper());

        try {
            if (!Sui.init(packageName)) {
                System.err.println("Unable to acquire the binder of Sui. Make sure Sui is installed and the current application is not hidden in Sui.");
                System.err.flush();
                System.exit(1);
            }
        } catch (Throwable tr) {
            tr.printStackTrace(System.err);
            System.err.flush();
            System.exit(1);
        }

        IBinder binder = Shizuku.getBinder();

        handler.post(() -> {
            RishConfig.init(binder, ShizukuApiConstants.BINDER_DESCRIPTOR, 30000);
            Shizuku.onBinderReceived(binder, packageName);
            Shizuku.addBinderReceivedListenerSticky(() -> {
                int version = Shizuku.getVersion();
                if (version < 12) {
                    System.err.println("Rish requires server 12 (running " + version + ")");
                    System.err.flush();
                    System.exit(1);
                }
                new Shell().start(args);
            });
        });

        Looper.loop();
        System.exit(0);
    }
}
