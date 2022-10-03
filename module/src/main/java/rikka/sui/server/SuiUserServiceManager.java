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

package rikka.sui.server;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.ArrayMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rikka.shizuku.server.UserServiceManager;
import rikka.shizuku.server.UserServiceRecord;

public class SuiUserServiceManager extends UserServiceManager {

    public static final String USER_SERVICE_CMD_DEBUG;

    private static final String USER_SERVICE_CMD_FORMAT = "(CLASSPATH='%s' %s%s /system/bin " +
            "--nice-name='%s' %s " +
            "--token='%s' --package='%s' --class='%s' --uid=%d%s)&";

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
        SuiUserServiceManager.dexPath = path;
    }

    @Override
    public String getUserServiceStartCmd(rikka.shizuku.server.UserServiceRecord record, String key, String token, String packageName, String classname, String processNameSuffix, int callingUid, boolean use32Bits, boolean debug) {
        String appProcess = "/system/bin/app_process";
        if (use32Bits && new File("/system/bin/app_process32").exists()) {
            appProcess = "/system/bin/app_process32";
        }
        String processName = String.format("%s:%s", packageName, processNameSuffix);
        return String.format(Locale.ENGLISH, USER_SERVICE_CMD_FORMAT, dexPath, appProcess,
                debug ? (" " + SuiUserServiceManager.USER_SERVICE_CMD_DEBUG) : "",
                processName, "rikka.sui.server.userservice.Starter",
                token, packageName, classname, callingUid, debug ? (" " + "--debug-name=" + processName) : "");
    }

    private final Map<String, List<UserServiceRecord>> userServiceRecords = Collections.synchronizedMap(new ArrayMap<>());

    @Override
    public void onUserServiceRecordCreated(UserServiceRecord record, PackageInfo packageInfo) {
        String packageName = packageInfo.packageName;
        List<UserServiceRecord> list = userServiceRecords.get(packageName);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            userServiceRecords.put(packageName, list);
        }
        list.add(record);
    }

    public void onPackageRemoved(String packageName) {
        List<UserServiceRecord> list = userServiceRecords.get(packageName);
        if (list != null) {
            for (UserServiceRecord record : list) {
                record.removeSelf();
                LOGGER.i("remove user service %s since package %s has been removed", record.token, packageName);
            }
            userServiceRecords.remove(packageName);
        }
    }
}
