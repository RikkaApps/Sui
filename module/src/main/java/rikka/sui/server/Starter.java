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

import android.content.Context;
import android.ddm.DdmHandleAppName;
import android.os.ServiceManager;

import static rikka.sui.server.ServerConstants.LOGGER;

public class Starter {

    private static void waitSystemService(String name) {
        while (ServiceManager.getService(name) == null) {
            try {
                LOGGER.i("service " + name + " is not started, wait 1s.");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.w(e.getMessage(), e);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            for (String arg : args) {
                if (arg.equals("--debug")) {
                    DdmHandleAppName.setAppName("sui", 0);
                } else if (arg.startsWith("--dex-path=")) {
                    SuiUserServiceManager.setStartDex(arg.substring("--dex-path=".length()));
                }
            }
        }

        waitSystemService("package");
        waitSystemService("activity");
        waitSystemService(Context.USER_SERVICE);
        waitSystemService(Context.APP_OPS_SERVICE);

        SuiService.main();
    }
}
