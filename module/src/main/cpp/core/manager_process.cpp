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

#include <cstdio>
#include <cstring>
#include <chrono>
#include <fcntl.h>
#include <unistd.h>
#include <sys/vfs.h>
#include <sys/stat.h>
#include <dirent.h>
#include <jni.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/uio.h>
#include <mntent.h>
#include <sys/mount.h>
#include <sys/sendfile.h>
#include <dlfcn.h>
#include <cinttypes>
#include <vector>

#include "android.h"
#include "logging.h"
#include "riru.h"
#include "misc.h"
#include "dex_file.h"
#include "bridge_service.h"
#include "binder_hook.h"
#include "config.h"

namespace Manager {

    static jclass mainClass = nullptr;

    static bool installDex(JNIEnv *env, const char *appDataDir, Dex *dexFile) {
        if (true/*android::GetApiLevel() < 26*/) {
            char dexPath[PATH_MAX], oatDir[PATH_MAX];
            snprintf(dexPath, PATH_MAX, "%s/sui/%s", appDataDir, DEX_NAME);
            snprintf(oatDir, PATH_MAX, "%s/sui/oat", appDataDir);
            dexFile->setPre26Paths(dexPath, oatDir);
        }
        dexFile->createClassLoader(env);

        mainClass = dexFile->findClass(env, MANAGER_PROCESS_CLASSNAME);
        if (!mainClass) {
            LOGE("unable to find main class");
            return false;
        }
        mainClass = (jclass) env->NewGlobalRef(mainClass);

        auto mainMethod = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
        if (!mainMethod) {
            LOGE("unable to find main method");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return false;
        }

        auto args = env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);

        env->CallStaticVoidMethod(mainClass, mainMethod, args);
        if (env->ExceptionCheck()) {
            LOGE("unable to call main method");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return false;
        }

        return true;
    }

    void main(JNIEnv *env, const char *appDataDir, Dex *dexFile) {
        if (!dexFile->valid()) {
            LOGE("no dex");
            return;
        }

        LOGV("main: manager");

        LOGV("install dex");

        if (!installDex(env, appDataDir, dexFile)) {
            LOGE("can't install dex");
            return;
        }

        LOGV("install dex finished");
    }
}
