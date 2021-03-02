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

#include "android.h"
#include "logging.h"
#include "riru.h"
#include "misc.h"
#include "dex_file.h"
#include "bridge_service.h"
#include "binder_hook.h"
#include "config.h"

namespace SystemServer {

    static jclass mainClass = nullptr;
    static jmethodID my_execTransactMethodID;

    static bool installDex(JNIEnv *env, DexFile *dexFile) {
        if (android::GetApiLevel() >= 26) {
            dexFile->createInMemoryDexClassLoader(env);
        } else {
            dexFile->createDexClassLoader(env, FALLBACK_DEX_DIR, DEX_NAME, FALLBACK_DEX_DIR "/oat");
        }

        mainClass = dexFile->findClass(env, SYSTEM_PROCESS_CLASSNAME);
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

        my_execTransactMethodID = env->GetStaticMethodID(mainClass, "execTransact", "(IJJI)Z");
        if (!my_execTransactMethodID) {
            LOGE("unable to find execTransact");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return false;
        }

        auto args = env->NewObjectArray(1, env->FindClass("java/lang/String"), nullptr);

        char buf[64];
        sprintf(buf, "--version-code=%d", RIRU_MODULE_VERSION);
        env->SetObjectArrayElement(args, 0, env->NewStringUTF(buf));

        env->CallStaticVoidMethod(mainClass, mainMethod, args);
        if (env->ExceptionCheck()) {
            LOGE("unable to call main method");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return false;
        }

        return true;
    }

    static bool ExecTransact(jboolean *res, JNIEnv *env, jobject obj, va_list args) {
        jint code;

        va_list copy;
        va_copy(copy, args);
        code = va_arg(copy, jint);
        va_end(copy);

        if (code == BridgeService::BRIDGE_TRANSACTION_CODE) {
            *res = env->CallStaticBooleanMethodV(mainClass, my_execTransactMethodID, args);
            return true;
        }

        return false;
    }

    void main(JNIEnv *env, DexFile *dexFile) {
        LOGD("dex size=%" PRIdPTR, dexFile->getSize());

        if (!dexFile->getBytes()) {
            LOGE("no dex");
            return;
        }

        LOGV("main: system server");

        LOGV("install dex");

        if (!installDex(env, dexFile)) {
            LOGE("can't install dex");
            return;
        }

        LOGV("install dex finished");

        JavaVM *javaVm;
        env->GetJavaVM(&javaVm);

        BinderHook::Install(javaVm, env, ExecTransact);
    }
}