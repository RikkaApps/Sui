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

package rikka.sui.util;

import android.os.SELinux;

public class OsUtils {

    private static final int UID = android.system.Os.getuid();
    private static final int PID = android.system.Os.getpid();
    private static final String SELINUX_CONTEXT;

    static {
        String context;
        try {
            context = SELinux.getContext();
        } catch (Throwable tr) {
            context =null;
        }
        SELINUX_CONTEXT = context;
    }


    public static int getUid() {
        return UID;
    }
    
    public static int getPid() {
        return PID;
    }

    public static String getSELinuxContext() {
        return SELINUX_CONTEXT;
    }
}

