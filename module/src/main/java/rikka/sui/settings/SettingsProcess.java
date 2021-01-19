package rikka.sui.settings;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import rikka.sui.ktx.DrawableKt;
import rikka.sui.ktx.HandlerKt;
import rikka.sui.resource.Res;
import rikka.sui.resource.Strings;
import rikka.sui.resource.Xml;
import rikka.sui.util.BridgeServiceClient;

import static rikka.sui.settings.SettingsConstants.LOGGER;

public class SettingsProcess {

    private static final String SHOW_MANAGEMENT_ACTION = "rikka.sui.SHOW_MANAGEMENT";
    private static final String CHANNEL_SHOW_MANAGEMENT_ID = "rikka.sui:show_management";
    private static final String CHANNEL_GROUP_ID = "rikka.sui";
    private static final int NOTIFICATION_ID = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';
    private static final String NOTIFICATION_TAG = "rikka.sui";

    private static final BroadcastReceiver SHOW_MANAGEMENT_RECEIVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LOGGER.i("showManagement");
            BridgeServiceClient.showManagement();
        }
    };

    private static void cancelNotification(Context context) {
        LOGGER.i("cancelNotification");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel("rikka.sui", NOTIFICATION_ID);

        try {
            context.unregisterReceiver(SHOW_MANAGEMENT_RECEIVER);
            LOGGER.i("unregisterReceiver");
        } catch (Throwable e) {
            LOGGER.w(e, "unregisterReceiver");
        }
    }

    private static void showNotification(Context context) {
        LOGGER.i("showNotification");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelGroup group = new NotificationChannelGroup(CHANNEL_GROUP_ID, "Sui");
            nm.createNotificationChannelGroup(group);

            NotificationChannel channel = new NotificationChannel(CHANNEL_SHOW_MANAGEMENT_ID, Strings.get(Res.string.notification_channel_group_name), NotificationManager.IMPORTANCE_MIN);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            channel.setBypassDnd(true);
            channel.setGroup(CHANNEL_GROUP_ID);
            channel.setSound(null, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(false);
            }
            nm.createNotificationChannel(channel);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_SHOW_MANAGEMENT_ID);
        } else {
            builder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setSound(Uri.EMPTY)
                    .setVibrate(new long[0]);
        }

        Bitmap bitmap;
        try {
            Drawable drawable = VectorDrawable.createFromXml(context.getResources(), Xml.get(Res.drawable.ic_su_24));
            int size = Math.round(Resources.getSystem().getDisplayMetrics().density * 24);
            bitmap = DrawableKt.toBitmap(drawable, size, size, null);
            builder.setSmallIcon(Icon.createWithBitmap(bitmap));
        } catch (Throwable e) {
            LOGGER.e(e, "create icon");
            builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        }

        Intent intent = new Intent(SHOW_MANAGEMENT_ACTION)
                .setPackage(context.getPackageName());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentTitle("Sui")
                .setContentText(Strings.get(Res.string.notification_show_management_text))
                .setContentIntent(pendingIntent);

        Notification notification = builder.build();

        nm.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SHOW_MANAGEMENT_ACTION);

        try {
            context.registerReceiver(SHOW_MANAGEMENT_RECEIVER, intentFilter,
                    "android.permission.MANAGE_DEVICE_ADMINS", null);
            LOGGER.i("registerReceiver");
        } catch (Throwable e) {
            LOGGER.w(e, "registerReceiver");
        }
    }

    private static void init() {
        Application application = null;
        try {
            application = ActivityThread.currentActivityThread().getApplication();
        } catch (Throwable e) {
            LOGGER.w(e, "getApplication");
        }

        if (application == null) {
            LOGGER.w("application is null, wait 1s");
            WorkerHandler.get().postDelayed(SettingsProcess::init, 1000);
            return;
        }

        ResolveInfo ri = application.getPackageManager().resolveActivity(
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).setPackage(application.getPackageName()), 0);

        if (ri == null) {
            LOGGER.e("cannot find activity for action %s", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            return;
        }

        String developmentActivityName = ri.activityInfo.name;
        LOGGER.d("activity for %s is %s", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, developmentActivityName);

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                Intent intent = activity.getIntent();
                String fragment = intent.getStringExtra(":settings:show_fragment");

                LOGGER.d("onActivityStarted: %s, action=%s, fragment=%s",
                        activity.getLocalClassName(), activity.getIntent().getAction(), fragment);

                if (fragment != null && fragment.contains("Development")
                        || activity.getComponentName().getClassName().contains(developmentActivityName)) {
                    WorkerHandler.get().post(() -> showNotification(activity));
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (activity.isChangingConfigurations()) return;

                Intent intent = activity.getIntent();
                String fragment = intent.getStringExtra(":settings:show_fragment");

                LOGGER.d("onActivityStopped: %s, action=%s, fragment=%s",
                        activity.getLocalClassName(), activity.getIntent().getAction(), fragment);

                if (fragment != null && fragment.contains("Development")
                        || activity.getComponentName().getClassName().contains(developmentActivityName)) {
                    WorkerHandler.get().post(() -> cancelNotification(activity));
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });

        LOGGER.d("registerActivityLifecycleCallbacks");
    }

    public static void main(String[] args, ByteBuffer[] buffers) {
        LOGGER.d("main: %s", Arrays.toString(args));
        WorkerHandler.get().postDelayed(SettingsProcess::init, 1000);

        Xml.setBuffers(buffers);
    }
}
