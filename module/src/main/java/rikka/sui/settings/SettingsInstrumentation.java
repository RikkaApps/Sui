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
import static rikka.sui.shortcut.ShortcutConstants.SHORTCUT_EXTRA;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.TestLooperManager;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import rikka.sui.resource.SuiApk;

public class SettingsInstrumentation extends Instrumentation {

    private final Instrumentation original;
    private final Application application;

    @SuppressWarnings("FieldCanBeLocal")
    private final ClassLoader classLoader;
    private final Class<?> suiActivityClass;
    private final Constructor<?> suiActivityConstructor;
    private final Resources resources;

    public SettingsInstrumentation(Instrumentation original, SuiApk suiApk) {
        this.original = original;

        application = ActivityThread.currentActivityThread().getApplication();
        classLoader = suiApk.getClassLoader();
        suiActivityClass = suiApk.getSuiActivityClass();
        suiActivityConstructor = suiApk.getSuiActivityConstructor();
        resources = suiApk.getResources();

        application.registerComponentCallbacks(new ComponentCallbacks2() {

            @Override
            public void onConfigurationChanged(@NonNull Configuration newConfig) {
                resources.updateConfiguration(newConfig, resources.getDisplayMetrics());
            }

            @Override
            public void onLowMemory() {

            }

            @Override
            public void onTrimMemory(int level) {

            }
        });
    }

    public Resources getResources() {
        return resources;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        LOGGER.v("newActivity: %s", className);
        if (suiActivityConstructor == null) {
            return original.newActivity(cl, className, intent);
        }

        // Don't call Intent#getXXX directly since it will lead to the deserialization of
        // the extras Bundle. At this time, classloader hasn't been set to app's.
        // If the Bundle contains app's Parcelable, a BadParcelableException will happen.
        // Intent#getExtras makes a copy of extras inside.
        Bundle extras = intent.getExtras();
        if (extras != null) {
            extras.setClassLoader(cl);
            if (extras.getInt(SHORTCUT_EXTRA, -1) != -1) {
                LOGGER.v("creating SuiActivity");
                try {
                    return (Activity) suiActivityConstructor.newInstance(application, resources);
                } catch (InvocationTargetException e) {
                    LOGGER.e(e, "Cannot create activity");
                }
            }
        }
        return original.newActivity(cl, className, intent);
    }

    @Override
    public void onCreate(Bundle arguments) {
        original.onCreate(arguments);
    }

    @Override
    public void start() {
        original.start();
    }

    @Override
    public void onStart() {
        original.onStart();
    }

    @Override
    public boolean onException(Object obj, Throwable e) {
        return original.onException(obj, e);
    }

    @Override
    public void sendStatus(int resultCode, Bundle results) {
        original.sendStatus(resultCode, results);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void addResults(Bundle results) {
        original.addResults(results);
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        original.finish(resultCode, results);
    }

    @Override
    public void setAutomaticPerformanceSnapshots() {
        original.setAutomaticPerformanceSnapshots();
    }

    @Override
    public void startPerformanceSnapshot() {
        original.startPerformanceSnapshot();
    }

    @Override
    public void endPerformanceSnapshot() {
        original.endPerformanceSnapshot();
    }

    @Override
    public void onDestroy() {
        original.onDestroy();
    }

    @Override
    public Context getContext() {
        return original.getContext();
    }

    @Override
    public ComponentName getComponentName() {
        return original.getComponentName();
    }

    @Override
    public Context getTargetContext() {
        return original.getTargetContext();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public String getProcessName() {
        return original.getProcessName();
    }

    @Override
    public boolean isProfiling() {
        return original.isProfiling();
    }

    @Override
    public void startProfiling() {
        original.startProfiling();
    }

    @Override
    public void stopProfiling() {
        original.stopProfiling();
    }

    @Override
    public void setInTouchMode(boolean inTouch) {
        original.setInTouchMode(inTouch);
    }

    @Override
    public void waitForIdle(Runnable recipient) {
        original.waitForIdle(recipient);
    }

    @Override
    public void waitForIdleSync() {
        original.waitForIdleSync();
    }

    @Override
    public void runOnMainSync(Runnable runner) {
        original.runOnMainSync(runner);
    }

    @Override
    public Activity startActivitySync(Intent intent) {
        return original.startActivitySync(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @NonNull
    @Override
    public Activity startActivitySync(@NonNull Intent intent, @Nullable Bundle options) {
        return original.startActivitySync(intent, options);
    }

    @Override
    public void addMonitor(ActivityMonitor monitor) {
        original.addMonitor(monitor);
    }

    @Override
    public ActivityMonitor addMonitor(IntentFilter filter, ActivityResult result, boolean block) {
        return original.addMonitor(filter, result, block);
    }

    @Override
    public ActivityMonitor addMonitor(String cls, ActivityResult result, boolean block) {
        return original.addMonitor(cls, result, block);
    }

    @Override
    public boolean checkMonitorHit(ActivityMonitor monitor, int minHits) {
        return original.checkMonitorHit(monitor, minHits);
    }

    @Override
    public Activity waitForMonitor(ActivityMonitor monitor) {
        return original.waitForMonitor(monitor);
    }

    @Override
    public Activity waitForMonitorWithTimeout(ActivityMonitor monitor, long timeOut) {
        return original.waitForMonitorWithTimeout(monitor, timeOut);
    }

    @Override
    public void removeMonitor(ActivityMonitor monitor) {
        original.removeMonitor(monitor);
    }

    @Override
    public boolean invokeMenuActionSync(Activity targetActivity, int id, int flag) {
        return original.invokeMenuActionSync(targetActivity, id, flag);
    }

    @Override
    public boolean invokeContextMenuAction(Activity targetActivity, int id, int flag) {
        return original.invokeContextMenuAction(targetActivity, id, flag);
    }

    @Override
    public void sendStringSync(String text) {
        original.sendStringSync(text);
    }

    @Override
    public void sendKeySync(KeyEvent event) {
        original.sendKeySync(event);
    }

    @Override
    public void sendKeyDownUpSync(int key) {
        original.sendKeyDownUpSync(key);
    }

    @Override
    public void sendCharacterSync(int keyCode) {
        original.sendCharacterSync(keyCode);
    }

    @Override
    public void sendPointerSync(MotionEvent event) {
        original.sendPointerSync(event);
    }

    @Override
    public void sendTrackballEventSync(MotionEvent event) {
        original.sendTrackballEventSync(event);
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return original.newApplication(cl, className, context);
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        original.callApplicationOnCreate(app);
    }

    @Override
    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id, Object lastNonConfigurationInstance) throws IllegalAccessException, InstantiationException {
        return original.newActivity(clazz, context, token, application, intent, info, title, parent, id, lastNonConfigurationInstance);
    }

    @Override
    public void callActivityOnCreate(Activity activity, @Nullable Bundle icicle) {
        LOGGER.d("callActivityOnCreate: %s", activity);
        if (icicle != null && suiActivityClass.isAssignableFrom(activity.getClass())) {
            icicle.setClassLoader(classLoader);
        }
        original.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnCreate(Activity activity, @Nullable Bundle icicle, PersistableBundle persistentState) {
        LOGGER.d("callActivityOnCreate: %s", activity);
        if (icicle != null && suiActivityClass.isAssignableFrom(activity.getClass())) {
            icicle.setClassLoader(classLoader);
        }
        original.callActivityOnCreate(activity, icicle, persistentState);
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        original.callActivityOnDestroy(activity);
    }

    @Override
    public void callActivityOnRestoreInstanceState(@NonNull Activity activity, @NonNull Bundle savedInstanceState) {
        LOGGER.d("callActivityOnRestoreInstanceState: %s", activity);
        if (suiActivityClass.isAssignableFrom(activity.getClass())) {
            savedInstanceState.setClassLoader(classLoader);
        }
        original.callActivityOnRestoreInstanceState(activity, savedInstanceState);
    }

    @Override
    public void callActivityOnRestoreInstanceState(@NonNull Activity activity, @Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        LOGGER.d("callActivityOnRestoreInstanceState: %s", activity);
        if (savedInstanceState != null && suiActivityClass.isAssignableFrom(activity.getClass())) {
            savedInstanceState.setClassLoader(classLoader);
        }
        original.callActivityOnRestoreInstanceState(activity, savedInstanceState, persistentState);
    }

    @Override
    public void callActivityOnPostCreate(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        LOGGER.d("callActivityOnPostCreate: %s", activity);
        if (savedInstanceState != null && suiActivityClass.isAssignableFrom(activity.getClass())) {
            savedInstanceState.setClassLoader(classLoader);
        }
        original.callActivityOnPostCreate(activity, savedInstanceState);
    }

    @Override
    public void callActivityOnPostCreate(@NonNull Activity activity, @Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        LOGGER.d("callActivityOnPostCreate: %s", activity);
        if (savedInstanceState != null && suiActivityClass.isAssignableFrom(activity.getClass())) {
            savedInstanceState.setClassLoader(classLoader);
        }
        original.callActivityOnPostCreate(activity, savedInstanceState, persistentState);
    }

    @Override
    public void callActivityOnNewIntent(Activity activity, Intent intent) {
        original.callActivityOnNewIntent(activity, intent);
    }

    @Override
    public void callActivityOnStart(Activity activity) {
        original.callActivityOnStart(activity);
    }

    @Override
    public void callActivityOnRestart(Activity activity) {
        original.callActivityOnRestart(activity);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        original.callActivityOnResume(activity);
    }

    @Override
    public void callActivityOnStop(Activity activity) {
        original.callActivityOnStop(activity);
    }

    @Override
    public void callActivityOnSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        LOGGER.d("callActivityOnSaveInstanceState: %s", activity);
        if (suiActivityClass.isAssignableFrom(activity.getClass())) {
            outState.setClassLoader(classLoader);
        }
        original.callActivityOnSaveInstanceState(activity, outState);
    }

    @Override
    public void callActivityOnSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        LOGGER.d("callActivityOnSaveInstanceState: %s", activity);
        if (suiActivityClass.isAssignableFrom(activity.getClass())) {
            outState.setClassLoader(classLoader);
        }
        original.callActivityOnSaveInstanceState(activity, outState, outPersistentState);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        original.callActivityOnPause(activity);
    }

    @Override
    public void callActivityOnUserLeaving(Activity activity) {
        original.callActivityOnUserLeaving(activity);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void callActivityOnPictureInPictureRequested(@NonNull Activity activity) {
        original.callActivityOnPictureInPictureRequested(activity);
    }

    @Override
    @Deprecated
    public void startAllocCounting() {
        original.startAllocCounting();
    }

    @Override
    @Deprecated
    public void stopAllocCounting() {
        original.stopAllocCounting();
    }

    @Override
    public Bundle getAllocCounts() {
        return original.getAllocCounts();
    }

    @Override
    public Bundle getBinderCounts() {
        return original.getBinderCounts();
    }

    @Override
    public UiAutomation getUiAutomation() {
        return original.getUiAutomation();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public UiAutomation getUiAutomation(int flags) {
        return original.getUiAutomation(flags);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public TestLooperManager acquireLooperManager(Looper looper) {
        return original.acquireLooperManager(looper);
    }
}
