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
 * Copyright (c) 2022 Sui Contributors
 */

package rikka.sui.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IAppTask;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;

import com.android.internal.content.ReferrerIntent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint({"SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class InstrumentationUtil {

    private static Method callActivityOnNewIntentMethod = null;
    private static Method execStartActivityMethod = null;
    private static Method execStartActivityMethod2 = null;
    private static Method execStartActivityMethod3 = null;
    private static Method execStartActivitiesMethod = null;
    private static Method execStartActivitiesAsUserMethod = null;
    private static Method execStartActivityAsCallerMethod = null;
    private static Method execStartActivityFromAppTaskMethod = null;

    public static void callActivityOnNewIntent(Instrumentation instrumentation, Activity activity, ReferrerIntent intent) {
        if (callActivityOnNewIntentMethod == null) {
            try {
                callActivityOnNewIntentMethod = Instrumentation.class.getDeclaredMethod("callActivityOnNewIntent", Activity.class, ReferrerIntent.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (callActivityOnNewIntentMethod != null) {
            try {
                callActivityOnNewIntentMethod.invoke(instrumentation, activity, intent);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static Instrumentation.ActivityResult execStartActivity(Instrumentation instrumentation, Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options) {
        if (execStartActivityMethod == null) {
            try {
                execStartActivityMethod = Instrumentation.class.getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (execStartActivityMethod != null) {
            try {
                return (Instrumentation.ActivityResult) execStartActivityMethod.invoke(instrumentation, who, contextThread, token, target, intent, requestCode, options);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Instrumentation.ActivityResult execStartActivity(
            Instrumentation instrumentation, Context who, IBinder contextThread, IBinder token, String target,
            Intent intent, int requestCode, Bundle options) {
        if (execStartActivityMethod2 == null) {
            try {
                execStartActivityMethod2 = Instrumentation.class.getDeclaredMethod("execStartActivity",
                        Context.class, IBinder.class, IBinder.class, String.class, Intent.class, int.class, Bundle.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (execStartActivityMethod2 != null) {
            try {
                return (Instrumentation.ActivityResult) execStartActivityMethod2.invoke(instrumentation, who, contextThread, token, target, intent, requestCode, options);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Instrumentation.ActivityResult execStartActivity(
            Instrumentation instrumentation, Context who, IBinder contextThread, IBinder token, String resultWho, Intent intent, int requestCode, Bundle options, UserHandle user) {
        if (execStartActivityMethod3 == null) {
            try {
                execStartActivityMethod3 = Instrumentation.class.getDeclaredMethod("execStartActivity",
                        Context.class, IBinder.class, IBinder.class, String.class, Intent.class, int.class, Bundle.class, UserHandle.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (execStartActivityMethod3 != null) {
            try {
                return (Instrumentation.ActivityResult) execStartActivityMethod3.invoke(instrumentation, who, contextThread, token, resultWho, intent, requestCode, options, user);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void execStartActivities(Instrumentation instrumentation, Context who, IBinder contextThread, IBinder token, Activity target, Intent[] intents, Bundle options) {
        if (execStartActivitiesMethod == null) {
            try {
                execStartActivitiesMethod = Instrumentation.class.getDeclaredMethod("execStartActivities", Context.class, IBinder.class, IBinder.class, Activity.class, Intent[].class, Bundle.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (execStartActivitiesMethod != null) {
            try {
                execStartActivitiesMethod.invoke(instrumentation, who, contextThread, token, target, intents, options);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static int execStartActivitiesAsUser(Instrumentation instrumentation, Context who, IBinder contextThread, IBinder token, Activity target, Intent[] intents, Bundle options, int userId) {
        if (execStartActivitiesAsUserMethod == null) {
            try {
                execStartActivitiesAsUserMethod = Instrumentation.class.getDeclaredMethod("execStartActivitiesAsUser", Context.class, IBinder.class, IBinder.class, Activity.class, Intent[].class, Bundle.class, int.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (execStartActivitiesAsUserMethod != null) {
            try {
                //noinspection ConstantConditions
                return (int) execStartActivitiesAsUserMethod.invoke(instrumentation, who, contextThread, token, target, intents, options, userId);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static Instrumentation.ActivityResult execStartActivityAsCaller(
            Instrumentation instrumentation, Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options, boolean ignoreTargetSecurity, int userId) {
        if (execStartActivityAsCallerMethod == null) {
            try {
                execStartActivityAsCallerMethod = Instrumentation.class.getDeclaredMethod("execStartActivityAsCaller",
                        Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class, boolean.class, int.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (execStartActivityAsCallerMethod != null) {
            try {
                return (Instrumentation.ActivityResult) execStartActivityAsCallerMethod.invoke(instrumentation, who, contextThread, token, target, intent, requestCode, options, ignoreTargetSecurity, userId);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void execStartActivityFromAppTask(Instrumentation original, Context who, IBinder contextThread, IAppTask appTask, Intent intent, Bundle options) {
        if (execStartActivityFromAppTaskMethod == null) {
            try {
                execStartActivityFromAppTaskMethod = Instrumentation.class.getDeclaredMethod("execStartActivityFromAppTask", Context.class, IBinder.class, IAppTask.class, Intent.class, Bundle.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (execStartActivityFromAppTaskMethod != null) {
            try {
                execStartActivityFromAppTaskMethod.invoke(original, who, contextThread, appTask, intent, options);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
