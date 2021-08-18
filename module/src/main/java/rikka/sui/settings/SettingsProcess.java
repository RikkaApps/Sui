/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

package rikka.sui.settings;

import static rikka.sui.settings.SettingsConstants.LOGGER;
import static rikka.sui.settings.SettingsConstants.SHORTCUT_ID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import rikka.sui.ktx.DrawableKt;
import rikka.sui.ktx.ResourcesKt;
import rikka.sui.resource.Res;
import rikka.sui.resource.Xml;

@TargetApi(Build.VERSION_CODES.O)
public class SettingsProcess {

    private static void onEnterDeveloperOptions(Context context) {
        LOGGER.d("onEnterDeveloperOptions");

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (!shortcutManager.isRequestPinShortcutSupported()) {
            return;
        }

        for (ShortcutInfo shortcutInfo : shortcutManager.getPinnedShortcuts()) {
            if (SHORTCUT_ID.equals(shortcutInfo.getId())) {
                LOGGER.i("Sui shortcut exists");
                return;
            }
        }

        Icon icon;

        try {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
            configuration.uiMode |= Configuration.UI_MODE_NIGHT_NO;
            Context themedContext = context.createConfigurationContext(configuration);

            int size = Math.round(Resources.getSystem().getDisplayMetrics().density * 108);
            int extraInsetsSize = Math.round(size * AdaptiveIconDrawable.getExtraInsetFraction());

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, size, size, paint);

            Drawable drawable = VectorDrawable.createFromXml(themedContext.getResources(), Xml.get(Res.drawable.ic_shortcut_24));
            drawable.setBounds(extraInsetsSize, extraInsetsSize, size - extraInsetsSize, size - extraInsetsSize);
            drawable.setTint(ResourcesKt.resolveColor(themedContext.getTheme(), android.R.attr.colorAccent));
            drawable.draw(canvas);

            icon = Icon.createWithAdaptiveBitmap(bitmap);
        } catch (Throwable e) {
            LOGGER.e(e, "create icon");
            icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info);
        }

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, SHORTCUT_ID)
                .setShortLabel("Sui")
                .setLongLabel("Sui")
                .setIcon(icon)
                .setIntent(intent)
                .build();

        shortcutManager.requestPinShortcut(shortcut, null);
    }

    private static void init() {
        Application application;
        try {
            application = ActivityThread.currentActivityThread().getApplication();
        } catch (Throwable e) {
            LOGGER.w(e, "getApplication is failed, wait 1s");
            WorkerHandler.get().postDelayed(SettingsProcess::init, 1000);
            return;
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
                    WorkerHandler.get().post(() -> onEnterDeveloperOptions(activity));
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
