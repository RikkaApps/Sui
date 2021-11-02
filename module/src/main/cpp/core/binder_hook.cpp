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

#include <cstring>
#include <nativehelper/scoped_local_ref.h>
#include <pthread.h>
#include <unistd.h>
#include <dlfcn.h>
#include "binder_hook.h"
#include "logging.h"
#include <android.h>
#include <plt.h>

static jmethodID original_execTransactMethodID;

static const JNIInvokeInterface *old_JNIInvokeInterface = nullptr;
static const JNINativeInterface *old_JNINativeInterface = nullptr;

static JNIInvokeInterface *new_JNIInvokeInterface = nullptr;
static JNINativeInterface *new_JNINativeInterface = nullptr;

using CallBooleanMethodV_t = jboolean(JNIEnv *, jobject, jmethodID, va_list);
using GetEnv_t = jint(JavaVM *, void **, jint);

static GetEnv_t *old_GetEnv;
static CallBooleanMethodV_t *old_CallBooleanMethodV;
static BinderHook::ExecTransact_t *my_ExecTransact;

using SetTableOverride_t = void(JNINativeInterface *);

static jboolean new_CallBooleanMethodV(JNIEnv *env, jobject obj, jmethodID methodId, va_list args) {
    if (methodId == original_execTransactMethodID) {
        jboolean res = false;
        if (my_ExecTransact(&res, env, obj, args)) return res;
    }

    return old_CallBooleanMethodV(env, obj, methodId, args);
}

static jint new_GetEnv(JavaVM *vm, void **env, jint version) {
    jint res = old_GetEnv(vm, env, version);
    if (res == JNI_OK && env && *env) {
        ((JNIEnv *) *env)->functions = new_JNINativeInterface;
    }
    return res;
}

static void InstallDirectly(JavaVM *javaVm, JNIEnv *env) {
    // JavaVM
    old_JNIInvokeInterface = javaVm->functions;
    old_GetEnv = javaVm->functions->GetEnv;
    new_JNIInvokeInterface = new JNIInvokeInterface();
    memcpy(new_JNIInvokeInterface, javaVm->functions, sizeof(JNIInvokeInterface));
    new_JNIInvokeInterface->GetEnv = new_GetEnv;

    javaVm->functions = new_JNIInvokeInterface;

    // JNIEnv
    env->functions = new_JNINativeInterface;
}

static bool InstallOverrideTable() {
    if (android::GetApiLevel() < 26) return false;

    auto setTableOverride = (SetTableOverride_t *) plt_dlsym("_ZN3art9JNIEnvExt16SetTableOverrideEPK18JNINativeInterface", nullptr);
    if (setTableOverride != nullptr) {
        setTableOverride(new_JNINativeInterface);
        return true;
    }
    return false;
}

void BinderHook::Install(JavaVM *javaVm, JNIEnv *env, ExecTransact_t *callback) {
    my_ExecTransact = callback;

    // Binder
    ScopedLocalRef<jclass> binderClass(env, env->FindClass("android/os/Binder"));
    original_execTransactMethodID = env->GetMethodID(binderClass.get(), "execTransact", "(IJJI)Z");

    // JNIEnv
    old_JNINativeInterface = env->functions;
    old_CallBooleanMethodV = env->functions->CallBooleanMethodV;
    new_JNINativeInterface = new JNINativeInterface();
    memcpy(new_JNINativeInterface, env->functions, sizeof(JNINativeInterface));
    new_JNINativeInterface->CallBooleanMethodV = new_CallBooleanMethodV;

    if (InstallOverrideTable()) {
        LOGI("installed override table");
    } else {
        LOGW("can't install override table");
        InstallDirectly(javaVm, env);
    }
}

void BinderHook::Uninstall(JavaVM *javaVm) {
    javaVm->functions = old_JNIInvokeInterface;
}

void BinderHook::Uninstall(JNIEnv *env) {
    env->functions = old_JNINativeInterface;
}
