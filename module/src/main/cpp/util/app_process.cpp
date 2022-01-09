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

#include <cstdlib>
#include <cstring>
#include <logging.h>
#include <unistd.h>
#include <sched.h>
#include <android.h>
#include <misc.h>
#include <fcntl.h>

#ifdef DEBUG
#define JAVA_DEBUGGABLE
#endif

void app_process(const char *dex_path, const char *files_path, const char *main_class, const char *process_name) {
    if (setenv("CLASSPATH", dex_path, true)) {
        LOGE("can't set CLASSPATH");
        exit(EXIT_FAILURE);
    }

#define ARG(v) char **v = nullptr; \
    char buf_##v[PATH_MAX]; \
    size_t v_size = 0; \
    uintptr_t v_current = 0;
#define ARG_PUSH(v, arg) v_size += sizeof(char *); \
if (v == nullptr) { \
    v = (char **) malloc(v_size); \
} else { \
    v = (char **) realloc(v, v_size);\
} \
v_current = (uintptr_t) v + v_size - sizeof(char *); \
*((char **) v_current) = arg ? strdup(arg) : nullptr;

#define ARG_END(v) ARG_PUSH(v, nullptr)

#define ARG_PUSH_FMT(v, fmt, ...) snprintf(buf_##v, PATH_MAX, fmt, __VA_ARGS__); \
    ARG_PUSH(v, buf_##v)

#ifdef JAVA_DEBUGGABLE
        #define ARG_PUSH_DEBUG_ONLY(v, arg) ARG_PUSH(v, arg)
#define ARG_PUSH_DEBUG_VM_PARAMS(v) \
    if (android::GetApiLevel() >= 30) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:adbconnection"); \
        ARG_PUSH(v, "-XjdwpOptions:suspend=n,server=y"); \
    } else if (android::GetApiLevel() >= 28) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:internal"); \
        ARG_PUSH(v, "-XjdwpOptions:transport=dt_android_adb,suspend=n,server=y"); \
    } else { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y"); \
    }
#else
#define ARG_PUSH_DEBUG_VM_PARAMS(v)
#define ARG_PUSH_DEBUG_ONLY(v, arg)
#endif

    ARG(argv)
    ARG_PUSH(argv, "/system/bin/app_process")
    ARG_PUSH_FMT(argv, "-Djava.class.path=%s", dex_path)
    ARG_PUSH_FMT(argv, "-Dsui.library.path=%s", files_path)
    ARG_PUSH_DEBUG_VM_PARAMS(argv)
    ARG_PUSH(argv, "/system/bin")
    ARG_PUSH_FMT(argv, "--nice-name=%s", process_name)
    ARG_PUSH(argv, main_class)
    ARG_PUSH_DEBUG_ONLY(argv, "--debug")
    ARG_PUSH_FMT(argv, "--files-path=%s", files_path)
    ARG_END(argv)

    LOGI("exec app_process...");
    if (execvp((const char *) argv[0], argv)) {
        PLOGE("execvp %s", argv[0]);
        exit(EXIT_FAILURE);
    }
}

void wait_for_zygote() {
    while (true) {
        static pid_t zygote_pid;

        zygote_pid = -1;
        foreach_proc([](pid_t pid) -> bool {
            if (pid == getpid()) return false;

#ifdef __LP64__
            const char* zygote_name = "zygote64";
#else
            const char *zygote_name = "zygote";
#endif
            char buf[64];
            snprintf(buf, 64, "/proc/%d/cmdline", pid);

            int fd = open(buf, O_RDONLY);
            if (fd > 0) {
                memset(buf, 0, 64);
                if (read(fd, buf, 64) > 0 && strcmp(zygote_name, buf) == 0) {
                    zygote_pid = pid;
                }
                close(fd);
            }
            return zygote_pid != -1;
        });

        if (zygote_pid != -1) {
            LOGI("found zygote %d", zygote_pid);
            break;
        }

        LOGV("zygote not started, wait 1s...");
        sleep(1);
    }
}
