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

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.Objects;

@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint("DiscouragedPrivateApi")
public class ActivityThreadUtil {

    private static Field hField;
    private static Field instrumentationField;
    private static int bindApplicationCode;
    private static Class<?> appBindDataClass;
    private static Field instrumentationNameField;
    private static Field appInfoField;

    public static void init() throws ReflectiveOperationException {
        hField = ActivityThread.class.getDeclaredField("mH");
        hField.setAccessible(true);

        instrumentationField = ActivityThread.class.getDeclaredField("mInstrumentation");
        instrumentationField.setAccessible(true);

        Field bindApplicationField = Class.forName(ActivityThread.class.getName() + "$H").getDeclaredField("BIND_APPLICATION");
        bindApplicationField.setAccessible(true);
        //noinspection ConstantConditions
        bindApplicationCode = (int) bindApplicationField.get(null);

        appBindDataClass = Class.forName(ActivityThread.class.getName() + "$AppBindData");
        instrumentationNameField = appBindDataClass.getDeclaredField("instrumentationName");
        instrumentationNameField.setAccessible(true);

        appInfoField = appBindDataClass.getDeclaredField("appInfo");
        appInfoField.setAccessible(true);
    }

    @NonNull
    public static Handler getH(@NonNull ActivityThread activityThread) {
        try {
            return (Handler) Objects.requireNonNull(hField.get(activityThread));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Instrumentation getInstrumentation(@NonNull ActivityThread activityThread) {
        try {
            return (Instrumentation) instrumentationField.get(activityThread);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setInstrumentation(@NonNull ActivityThread activityThread, Instrumentation instrumentation) {
        try {
            instrumentationField.set(activityThread, instrumentation);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getBindApplication() {
        return bindApplicationCode;
    }

    public static boolean isAppBindData(Object object) {
        return object != null && appBindDataClass.isAssignableFrom(object.getClass());
    }

    public static ComponentName getInstrumentationNameFromAppBindData(@NonNull Object appBindData) {
        try {
            return (ComponentName) instrumentationNameField.get(appBindData);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static ApplicationInfo getAppInfoFromAppBindData(@NonNull Object appBindData) {
        try {
            return (ApplicationInfo) Objects.requireNonNull(appInfoField.get(appBindData));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
