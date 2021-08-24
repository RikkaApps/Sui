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

package rikka.sui.shortcut;

import static rikka.sui.shortcut.ShortcutConstants.LOGGER;
import static rikka.sui.shortcut.ShortcutConstants.SHORTCUT_EXTRA;
import static rikka.sui.shortcut.ShortcutConstants.SHORTCUT_ID;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

import rikka.sui.ktx.ResourcesKt;
import rikka.sui.resource.Res;
import rikka.sui.resource.Xml;

public class SuiShortcut {

    public static Intent getIntent(Context context, boolean requiresStandardLaunchMode) {
        String[] actions = new String[]{
                Settings.ACTION_WIFI_SETTINGS,
                Settings.ACTION_NETWORK_OPERATOR_SETTINGS,
                Settings.ACTION_DEVICE_INFO_SETTINGS,
                Settings.ACTION_DISPLAY_SETTINGS,
                Settings.ACTION_SOUND_SETTINGS,
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
                Settings.ACTION_SECURITY_SETTINGS,
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        };

        Intent intent = new Intent("null").setPackage(context.getPackageName());
        PackageManager pm = context.getPackageManager();

        for (String action : actions) {
            intent.setAction(action);

            try {
                ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
                if (resolveInfo != null
                        && resolveInfo.activityInfo != null
                        && resolveInfo.activityInfo.exported
                        && (!requiresStandardLaunchMode || resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_MULTIPLE)) {
                    if (requiresStandardLaunchMode) {
                        LOGGER.i("Found action for Sui shortcut (standard launch mode): %s", action);
                        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    } else {
                        LOGGER.w("Found action for Sui shortcut: %s", action);
                    }
                    break;
                }
            } catch (Throwable e) {
                LOGGER.w(e, "resolveActivity %s", intent);
            }
        }

        if ("null".equals(intent.getAction())) {
            if (requiresStandardLaunchMode) {
                intent = getIntent(context, false);
            } else {
                LOGGER.w("Use launch intent for Sui shortcut");
                intent = pm.getLaunchIntentForPackage(context.getPackageName());
            }
        }

        intent.putExtra(SHORTCUT_EXTRA, 1);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static ShortcutInfo createShortcut(Context context, Resources resources) {
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
            paint.setColor(0xfff5f5f5 /* packages/apps/Settings/res/values/colors.xml shortcut_background */);
            canvas.drawRect(0, 0, size, size, paint);

            int id = resources.getIdentifier("ic_shortcut_24", "drawable", "rikka.sui");
            if (id == 0) {
                throw new IllegalStateException("Cannot find drawable resource ic_shortcut_24");
            }
            Drawable drawable = resources.getDrawable(id, themedContext.getTheme());
            drawable.setBounds(extraInsetsSize, extraInsetsSize, size - extraInsetsSize, size - extraInsetsSize);
            drawable.setTint(ResourcesKt.resolveColor(themedContext.getTheme(), android.R.attr.colorAccent));
            drawable.draw(canvas);

            icon = Icon.createWithAdaptiveBitmap(bitmap);
        } catch (Throwable e) {
            LOGGER.e(e, "create icon");
            icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info);
        }

        return new ShortcutInfo.Builder(context, SHORTCUT_ID)
                .setShortLabel("Sui")
                .setLongLabel("Sui")
                .setIcon(icon)
                .setIntent(getIntent(context, true))
                .build();
    }

    public static void addDynamicShortcut(Context context, Resources resources) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        LOGGER.d("addDynamicShortcut");

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> existing = shortcutManager.getDynamicShortcuts();
        boolean exists = false;

        for (ShortcutInfo shortcutInfo : existing) {
            if (SHORTCUT_ID.equals(shortcutInfo.getId())) {
                exists = true;
                break;
            }
        }

        if (exists) {
            LOGGER.i("Dynamic shortcut exists and up to date");
            return;
        }

        int maxCount = shortcutManager.getMaxShortcutCountPerActivity();
        LOGGER.d("Max dynamic shortcuts: %d", maxCount);

        if (existing.size() >= maxCount) {
            LOGGER.w("Cannot add more dynamic shortcuts");
            return;
        }

        ShortcutInfo shortcut = createShortcut(context, resources);
        List<ShortcutInfo> list = new ArrayList<>();
        list.add(shortcut);

        shortcutManager.addDynamicShortcuts(list);
    }

    public static void requestPinnedShortcut(Context context, Resources resources) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        LOGGER.d("requestPinnedShortcut");

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        boolean hasPinned = false;

        for (ShortcutInfo shortcutInfo : shortcutManager.getPinnedShortcuts()) {
            if (SHORTCUT_ID.equals(shortcutInfo.getId())) {
                hasPinned = true;
                LOGGER.i("Pinned shortcut exists");
                break;
            }
        }

        if (hasPinned) {
            return;
        }

        ShortcutInfo shortcut = createShortcut(context, resources);

        if (shortcutManager.isRequestPinShortcutSupported()) {
            shortcutManager.requestPinShortcut(shortcut, null);
        }
    }
}
