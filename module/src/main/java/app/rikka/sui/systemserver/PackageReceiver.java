package app.rikka.sui.systemserver;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static app.rikka.sui.systemserver.SystemServerConstants.LOGGER;

public class PackageReceiver {

    private static final BroadcastReceiver RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            String pkgName = (uri != null) ? uri.getSchemeSpecificPart() : null;
            int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            LOGGER.d("%s: %s (%d)", intent.getAction(), pkgName, uid);
            Bridge.onPackageChanged(intent);
        }
    };

    @SuppressLint("DiscouragedPrivateApi")
    public static void register() {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            LOGGER.w("ActivityThread is null");
            return;
        }
        Context context = null;
        try {
            //noinspection JavaReflectionMemberAccess
            Method method = ActivityThread.class.getDeclaredMethod("getSystemContext");
            context = (Context) method.invoke(activityThread);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
        if (context == null) {
            LOGGER.w("context is null");
            return;
        }

        UserHandle userHandleAll;
        try {
            //noinspection JavaReflectionMemberAccess
            Field field = UserHandle.class.getDeclaredField("ALL");
            userHandleAll = (UserHandle) field.get(null);
        } catch (Throwable e) {
            LOGGER.w("UserHandle.ALL", e);
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        Handler handler = new Handler(Looper.getMainLooper());

        try {
            //noinspection JavaReflectionMemberAccess
            Method method = Context.class.getDeclaredMethod("registerReceiverAsUser", BroadcastReceiver.class, UserHandle.class, IntentFilter.class, String.class, Handler.class);
            method.invoke(context, RECEIVER, userHandleAll, intentFilter, null, handler);
            LOGGER.d("register package receiver");
        } catch (Throwable e) {
            LOGGER.w("registerReceiver failed", e);
        }
    }
}
