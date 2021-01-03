#pragma once

#include <jni.h>

namespace BinderHook {

    using ExecTransact_t = bool(jboolean *, JNIEnv *, jobject, va_list);

    void Install(JavaVM *javaVm, JNIEnv *env, ExecTransact_t *callback);

    void Uninstall(JavaVM *javaVm);

    void Uninstall(JNIEnv *env);
}