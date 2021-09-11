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
#include <sched.h>
#include <unistd.h>
#include <sys/mount.h>
#include <logging.h>
#include <string_view>

using namespace std::literals::string_view_literals;

#ifdef __LP64__
constexpr char adbd_ld_preload[] = "/system/lib64/libsui_adbd_preload.so";
#else
constexpr char adbd_ld_preload[] = "/system/lib/libsui_adbd_preload.so";
#endif

int main(int argc, char **argv) {
    static char adbd_real[PATH_MAX]{0};
    strcpy(adbd_real, argv[0]);
    strcat(adbd_real, "_real");

    LOGI("adbd_main");
    LOGD("adbd_real=%s", adbd_real);
    LOGD("adbd_ld_preload=%s", adbd_ld_preload);

    auto ld_preload = getenv("LD_PRELOAD");
    char new_ld_preload[PATH_MAX]{};
    if (ld_preload) {
        setenv("SUI_LD_PRELOAD_BACKUP", ld_preload, 1);
        snprintf(new_ld_preload, PATH_MAX, "%s:%s", adbd_ld_preload, ld_preload);
    } else {
        strcpy(new_ld_preload, adbd_ld_preload);
    }
    setenv("LD_PRELOAD", new_ld_preload, 1);
    LOGD("LD_PRELOAD=%s", new_ld_preload);

    auto root_seclabel = "--root_seclabel"sv;
    for (int i = 1; i < argc; ++i) {
        std::string_view argv_i{argv[i]};
        if (argv_i.length() > root_seclabel.length() && argv_i.substr(0, root_seclabel.length()) == root_seclabel) {
            argv[i] = strdup("--root_seclabel=u:r:magisk:s0");
            LOGD("root_seclabel -> u:r:magisk:s0");
        }
    }

    return execv(adbd_real, argv);
}
