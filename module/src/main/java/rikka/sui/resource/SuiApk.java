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

package rikka.sui.resource;

import static rikka.sui.settings.SettingsConstants.LOGGER;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import dalvik.system.PathClassLoader;
import rikka.sui.util.BridgeServiceClient;

@SuppressLint({"DiscouragedPrivateApi", "BlockedPrivateApi"})
@SuppressWarnings("JavaReflectionMemberAccess")
public class SuiApk {

    @SuppressWarnings("FieldCanBeLocal")
    private ClassLoader classLoader;
    private Class<?> suiActivityClass;
    private Class<?> suiRequestPermissionDialogClass;
    private Constructor<?> suiActivityConstructor;
    private Constructor<?> suiRequestPermissionDialogConstructor;
    private Resources resources;

    public SuiApk() {
        String apkPath;
        try {
            ParcelFileDescriptor pfd = Objects.requireNonNull(BridgeServiceClient.openApk());
            int fd = pfd.detachFd();
            apkPath = "/proc/self/fd/" + fd;

        } catch (Throwable e) {
            LOGGER.e(e, "Cannot open apk");
            return;
        }

        try {
            classLoader = new PathClassLoader(apkPath, ClassLoader.getSystemClassLoader());
        } catch (Throwable e) {
            LOGGER.e(e, "Create PathClassLoader");
            return;
        }

        try {
            AssetManager am = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(am, apkPath);

            Application application = ActivityThread.currentActivityThread().getApplication();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Method addOverlayPath = AssetManager.class.getDeclaredMethod("addOverlayPath", String.class);
                addOverlayPath.setAccessible(true);

                ApplicationInfo ai = application.getApplicationInfo();
                Field field = ApplicationInfo.class.getDeclaredField("overlayPaths");
                String[] overlayPaths = (String[]) field.get(ai);

                if (overlayPaths != null) {
                    for (String overlayPath : overlayPaths) {
                        addOverlayPath.invoke(am, overlayPath);
                    }
                }
            }

            resources = new Resources(am, application.getResources().getDisplayMetrics(), application.getResources().getConfiguration());
        } catch (Throwable e) {
            LOGGER.e(e, "Cannot create resource");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Field classLoaderField = Resources.class.getDeclaredField("mClassLoader");
                classLoaderField.setAccessible(true);
                classLoaderField.set(resources, classLoader);
            } catch (Throwable e) {
                LOGGER.e(e, "Cannot set classloader for resource");
            }
        }
    }

    public void loadSuiActivity() {
        try {
            suiActivityClass = classLoader.loadClass("rikka.sui.SuiActivity");
            suiActivityConstructor = suiActivityClass.getDeclaredConstructor(Application.class, Resources.class);
        } catch (Throwable e) {
            LOGGER.e(e, "Cannot load SuiActivity class");
        }
    }

    public void loadSuiRequestPermissionDialog() {
        try {
            suiRequestPermissionDialogClass = classLoader.loadClass("rikka.sui.SuiRequestPermissionDialog");
            suiRequestPermissionDialogConstructor = suiRequestPermissionDialogClass.getDeclaredConstructor(
                    Application.class, Resources.class,
                    int.class, int.class, String.class, int.class);
        } catch (Throwable e) {
            LOGGER.e(e, "Cannot load SuiRequestPermissionDialog class");
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Class<?> getSuiActivityClass() {
        return suiActivityClass;
    }

    public Constructor<?> getSuiActivityConstructor() {
        return suiActivityConstructor;
    }

    public Class<?> getSuiRequestPermissionDialogClass() {
        return suiRequestPermissionDialogClass;
    }

    public Constructor<?> getSuiRequestPermissionDialogConstructor() {
        return suiRequestPermissionDialogConstructor;
    }

    public Resources getResources() {
        return resources;
    }
}
