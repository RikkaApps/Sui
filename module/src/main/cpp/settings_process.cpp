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

namespace Settings {

    static jclass mainClass = nullptr;

    static bool installDex(JNIEnv *env, const char *appDataDir, DexFile *dexFile, std::vector<File *> *files) {
        if (android::GetApiLevel() >= 26) {
            dexFile->createInMemoryDexClassLoader(env);
        } else {
            char dexDir[PATH_MAX], oatDir[PATH_MAX];
            snprintf(dexDir, PATH_MAX, "%s/sui", appDataDir);
            snprintf(oatDir, PATH_MAX, "%s/sui/oat", appDataDir);
            dexFile->createDexClassLoader(env, dexDir, DEX_NAME, oatDir);
        }

        mainClass = dexFile->findClass(env, SETTINGS_PROCESS_CLASSNAME);
        if (!mainClass) {
            LOGE("unable to find main class");
            return false;
        }
        mainClass = (jclass) env->NewGlobalRef(mainClass);

        auto mainMethod = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;[Ljava/nio/ByteBuffer;)V");
        if (!mainMethod) {
            LOGE("unable to find main method");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return false;
        }

        auto args = env->NewObjectArray(1, env->FindClass("java/lang/String"), nullptr);
        char buf[64];
        sprintf(buf, "--version-code=%d", RIRU_MODULE_VERSION);
        env->SetObjectArrayElement(args, 0, env->NewStringUTF(buf));

        auto buffers = env->NewObjectArray(files->size(), env->FindClass("java/nio/ByteBuffer"), nullptr);
        for (auto i = 0; i < files->size(); ++i) {
            auto file = files->at(i);
            env->SetObjectArrayElement(buffers, i, env->NewDirectByteBuffer(file->getBytes(), file->getSize()));
        }

        env->CallStaticVoidMethod(mainClass, mainMethod, args, buffers);
        if (env->ExceptionCheck()) {
            LOGE("unable to call main method");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return false;
        }

        return true;
    }

    void main(JNIEnv *env, const char *appDataDir, DexFile *dexFile, std::vector<File *> *files) {
        LOGD("dex size=%" PRIdPTR, dexFile->getSize());

        if (!dexFile->getBytes()) {
            LOGE("no dex");
            return;
        }

        LOGV("main: manager");

        LOGV("install dex");

        if (!installDex(env, appDataDir, dexFile, files)) {
            LOGE("can't install dex");
            return;
        }

        LOGV("install dex finished");
    }
}