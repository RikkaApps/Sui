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

package rikka.sui.systemserver;

import android.content.Intent;

import moe.shizuku.server.IShizukuService;

import static rikka.sui.systemserver.SystemServerConstants.LOGGER;

public class Bridge {

    public static void dispatchPackageChanged(Intent intent) {
        IShizukuService service = BridgeService.get();
        if (service == null) {
            LOGGER.d("binder is null");
            return;
        }

        try {
            service.dispatchPackageChanged(intent);
        } catch (Throwable e) {
            LOGGER.w(e, "dispatchPackageChanged");
        }
    }

    public static boolean isHidden(int uid) {
        IShizukuService service = BridgeService.get();
        if (service == null) {
            LOGGER.d("binder is null");
            return false;
        }

        try {
            return service.isHidden(uid);
        } catch (Throwable e) {
            LOGGER.w(e, "isHidden");
            return false;
        }
    }
}
