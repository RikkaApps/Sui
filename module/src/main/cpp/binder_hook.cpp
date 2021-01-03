#include <cstring>
#include <nativehelper/scoped_local_ref.h>
#include <pthread.h>
#include <unistd.h>
#include <dlfcn.h>
#include <riru.h>
#include "binder_hook.h"
#include "logging.h"
#include "android.h"
#include "plt.h"

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

/*static JavaVM *debug_javaVm;
static JNIEnv *debug_env;

[[noreturn]] static void *DebugPrint(void *) {
    while (true) {
        sleep(10);
        if (debug_javaVm->functions != new_JNIInvokeInterface) {
            LOGW("JavaVM->functions is changed: current=%p, my=%p", debug_javaVm->functions, new_JNIInvokeInterface);
        } else {
            LOGV("JavaVM->functions=%p", debug_javaVm->functions);
        }

        if (debug_env->functions != new_JNINativeInterface) {
            LOGW("JNIEnv->functions is changed: current=%p, my=%p", debug_env->functions, new_JNINativeInterface);
        } else {
            LOGV("JNIEnv->functions=%p", debug_env->functions);
        }
    }
}*/

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

    /*pthread_t thread;
    int res = pthread_create(&thread, nullptr, DebugPrint, nullptr);
    if (res == 0)pthread_detach(thread);*/
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
