#pragma once

#include <jni.h>

namespace BridgeService {

    const jint BRIDGE_TRANSACTION_CODE = 1599296841; // ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I'

    void init(JNIEnv *env);
    void insertFileMonitor(JNIEnv *env, const char* packageName, const char *func, const char *path);
    jobjectArray requestCheckProcess(JNIEnv *env, const char* packageName);
}