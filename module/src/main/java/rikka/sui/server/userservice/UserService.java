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

package rikka.sui.server.userservice;

import android.os.Build;

import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static rikka.sui.server.ServerConstants.LOGGER;

public class UserService {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    public static final String USER_SERVICE_CMD_DEBUG;

    private static final String USER_SERVICE_CMD_FORMAT = "(CLASSPATH=%s /system/bin/app_process%s /system/bin " +
            "--nice-name=%s %s " +
            "--token=%s --package=%s --class=%s --uid=%d%s)&";

    static {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 30) {
            USER_SERVICE_CMD_DEBUG = "-Xcompiler-option" + " --debuggable" +
                    " -XjdwpProvider:adbconnection" +
                    " -XjdwpOptions:suspend=n,server=y";
        } else if (sdk >= 28) {
            USER_SERVICE_CMD_DEBUG = "-Xcompiler-option" + " --debuggable" +
                    " -XjdwpProvider:internal" +
                    " -XjdwpOptions:transport=dt_android_adb,suspend=n,server=y";
        } else {
            USER_SERVICE_CMD_DEBUG = "-Xcompiler-option" + " --debuggable" +
                    " -agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y";
        }
    }

    private static String dexPath;

    public static void setStartDex(String path) {
        UserService.dexPath = path;
    }

    private static void start(UserServiceRecord record, String key, String token, String packageName, String classname, String processNameSuffix, int callingUid, boolean debug) {
        LOGGER.v("starting process for service record %s (%s)...", key, token);

        String processName = String.format("%s:%s", packageName, processNameSuffix);
        String cmd = String.format(Locale.ENGLISH, USER_SERVICE_CMD_FORMAT, dexPath,
                debug ? (" " + UserService.USER_SERVICE_CMD_DEBUG) : "",
                processName, "rikka.sui.server.userservice.Starter",
                token, packageName, classname, callingUid, debug ? (" " + "--debug-name=" + processName) : "");

        Process process;
        int exitCode;
        try {
            process = Runtime.getRuntime().exec("sh");
            OutputStream os = process.getOutputStream();
            os.write(cmd.getBytes());
            os.flush();
            os.close();

            exitCode = process.waitFor();
        } catch (Throwable e) {
            LOGGER.w(e, "exec");
            return;
        }

        if (exitCode != 0) {
            LOGGER.w("sh exited with " + exitCode);
        }
    }

    public static void schedule(UserServiceRecord record, String key, String token, String packageName, String classname, String processNameSuffix, int callingUid, boolean debug) {
        Runnable runnable = () -> UserService.start(record, key, token, packageName, classname, processNameSuffix, callingUid, debug);
        EXECUTOR.execute(runnable);
    }

}
