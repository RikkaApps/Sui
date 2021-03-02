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

#include <jni.h>
#include "logging.h"
#include "bridge_service.h"

namespace BridgeService {

// sync with SuiBridgeService.java
#define BRIDGE_SERVICE_DESCRIPTOR "android.app.IActivityManager"
#define BRIDGE_SERVICE_NAME "activity"
#define BRIDGE_ACTION_GET_BINDER 2

// sync with IShizukuService.aidl
#define DESCRIPTOR "moe.shizuku.server.IShizukuService"

    static jclass serviceManagerClass;
    static jmethodID getServiceMethod;

    static jmethodID transactMethod;

    static jclass parcelClass;
    static jmethodID obtainMethod;
    static jmethodID recycleMethod;
    static jmethodID writeInterfaceTokenMethod;
    static jmethodID writeIntMethod;
    static jmethodID writeStringMethod;
    static jmethodID readExceptionMethod;
    static jmethodID readStrongBinderMethod;
    static jmethodID createStringArray;

    static jclass deadObjectExceptionClass;

    void init(JNIEnv *env) {
        static bool init = false;
        if (init) return;
        init = true;

        // ServiceManager
        auto serviceManagerClass_ = env->FindClass("android/os/ServiceManager");
        if (serviceManagerClass_) {
            serviceManagerClass = (jclass) env->NewGlobalRef(serviceManagerClass_);
        } else {
            env->ExceptionClear();
            return;
        }
        getServiceMethod = env->GetStaticMethodID(serviceManagerClass, "getService", "(Ljava/lang/String;)Landroid/os/IBinder;");
        if (!getServiceMethod) {
            env->ExceptionClear();
            return;
        }

        // IBinder
        jclass iBinderClass = env->FindClass("android/os/IBinder");
        transactMethod = env->GetMethodID(iBinderClass, "transact", "(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z");

        // Parcel
        auto parcelClass_ = env->FindClass("android/os/Parcel");
        if (parcelClass_) parcelClass = (jclass) env->NewGlobalRef(parcelClass_);
        obtainMethod = env->GetStaticMethodID(parcelClass, "obtain", "()Landroid/os/Parcel;");
        recycleMethod = env->GetMethodID(parcelClass, "recycle", "()V");
        writeInterfaceTokenMethod = env->GetMethodID(parcelClass, "writeInterfaceToken", "(Ljava/lang/String;)V");
        writeIntMethod = env->GetMethodID(parcelClass, "writeInt", "(I)V");
        writeStringMethod = env->GetMethodID(parcelClass, "writeString", "(Ljava/lang/String;)V");
        readExceptionMethod = env->GetMethodID(parcelClass, "readException", "()V");
        readStrongBinderMethod = env->GetMethodID(parcelClass, "readStrongBinder", "()Landroid/os/IBinder;");
        createStringArray = env->GetMethodID(parcelClass, "createStringArray", "()[Ljava/lang/String;");

        auto deadObjectExceptionClass_ = env->FindClass("android/os/DeadObjectException");
        if (deadObjectExceptionClass_) deadObjectExceptionClass = (jclass) env->NewGlobalRef(deadObjectExceptionClass_);
    }

    static jobject serviceBinder = nullptr;

    static jobject requireBinder(JNIEnv *env, bool force = false) {
        /*
         * private static IBinder requireBinder() {
            if (storageIsolationBinder != null) return storageIsolationBinder;

            IBinder binder = ServiceManager.getService(BRIDGE_SERVICE_NAME);
            if (binder == null) return null;

            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(BRIDGE_SERVICE_DESCRIPTOR);
                data.writeInt(ACTION_GET_BINDER);
                binder.transact(BRIDGE_TRANSACTION_CODE, data, reply, 0);
                reply.readException();
                IBinder received = reply.readStrongBinder();
                if (received != null) {
                    storageIsolationBinder = received;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                data.recycle();
                reply.recycle();
            }
            return storageIsolationBinder;
        }
        */

        if (serviceBinder && !force) return serviceBinder;

        jstring bridgeServiceName, descriptor;
        jobject bridgeService, data, reply, service;
        jboolean res;

        bridgeServiceName = env->NewStringUTF(BRIDGE_SERVICE_NAME);
        bridgeService = env->CallStaticObjectMethod(serviceManagerClass, getServiceMethod, bridgeServiceName);
        if (!bridgeService) {
            LOGD("can't get %s", BRIDGE_SERVICE_NAME);
            goto clean_bridgeServiceName;
        }

        data = env->CallStaticObjectMethod(parcelClass, obtainMethod);
        reply = env->CallStaticObjectMethod(parcelClass, obtainMethod);

        descriptor = env->NewStringUTF(BRIDGE_SERVICE_DESCRIPTOR);
        env->CallVoidMethod(data, writeInterfaceTokenMethod, descriptor);
        env->CallVoidMethod(data, writeIntMethod, BRIDGE_ACTION_GET_BINDER);

        res = env->CallBooleanMethod(bridgeService, transactMethod, BRIDGE_TRANSACTION_CODE, data, reply, 0);

        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            goto clean;
        }

        if (res) {
            env->CallVoidMethod(reply, readExceptionMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
                goto clean;
            }
            service = env->CallObjectMethod(reply, readStrongBinderMethod);
            if (service != nullptr) {
                if (serviceBinder) env->DeleteGlobalRef(serviceBinder);
                serviceBinder = env->NewGlobalRef(service);
            }
        } else {
            LOGD("no reply");
        }

        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            goto clean;
        }

        clean:
        env->CallVoidMethod(data, recycleMethod);
        env->CallVoidMethod(reply, recycleMethod);

        env->DeleteLocalRef(descriptor);
        env->DeleteLocalRef(reply);
        env->DeleteLocalRef(data);
        env->DeleteLocalRef(bridgeService);

        clean_bridgeServiceName:
        env->DeleteLocalRef(bridgeServiceName);

        return serviceBinder;
    }

    static jboolean tryTransact(JNIEnv *env, jint code, jobject data, jobject reply, jint flags, bool retry = true) {
        auto binder = requireBinder(env);
        if (!binder) {
            LOGE("binder is null");
            return JNI_FALSE;
        }

        auto res = env->CallBooleanMethod(binder, transactMethod, code, data, reply, flags);

        auto exception = env->ExceptionOccurred();
        if (exception) {
            env->ExceptionClear();
            auto isDeadObjectException = env->IsInstanceOf(exception, deadObjectExceptionClass);
            env->DeleteLocalRef(exception);

            if (isDeadObjectException && retry) {
                res = tryTransact(env, code, data, reply, flags, false);
            }
        }

        return res;
    }
}