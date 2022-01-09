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

#include <cstdlib>
#include <cstring>
#include <logging.h>
#include <unistd.h>
#include <sched.h>
#include <app_process.h>

/*
 * argv[1]: path of the module, such as /data/adb/modules/zygisk-sui
 */
static int uninstall_main(int argc, char **argv) {
    LOGI("Sui uninstaller begin: %s", argv[1]);

    auto root_path = argv[1];

    char dex_path[PATH_MAX]{0};
    strcpy(dex_path, root_path);
    strcat(dex_path, "/sui.dex");

    if (copyfile(dex_path, "/dev/sui.dex") != 0) {
        PLOGE("copyfile");
        return EXIT_FAILURE;
    }

    if (daemon(false, false) != 0) {
        PLOGE("daemon");
        return EXIT_FAILURE;
    }

    wait_for_zygote();

    app_process("/dev/sui.dex", "/dev", "rikka.sui.installer.Uninstaller", "sui_uninstaller");
    unlink("/dev/sui.dex");

    return EXIT_SUCCESS;
}
