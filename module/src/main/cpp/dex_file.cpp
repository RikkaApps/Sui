#include <fcntl.h>
#include <zconf.h>
#include <malloc.h>
#include <jni.h>
#include "dex_file.h"
#include "misc.h"
#include "logging.h"
#include "rirud.h"

void DexFile::destroy(JNIEnv *env) {
    if (bytes) free(bytes);
    if (dexClassLoaderClass) env->DeleteGlobalRef(dexClassLoaderClass);
    if (dexClassLoader) env->DeleteGlobalRef(dexClassLoader);
}

DexFile::DexFile(const char *path) {
    if (!path) return;

    filepath = strdup(path);
    bytes = nullptr;
    size = 0;

    if (rirud::ReadFile(path, (char *&) bytes, size)) {
        LOGI("read dex from from rirud");
    } else {
        LOGE("failed to read from rirud");

        auto fd = open(path, O_RDONLY);
        if (fd == -1) {
            close(fd);
            return;
        }
        size = lseek(fd, 0, SEEK_END);
        if (size == -1) {
            close(fd);
            return;
        }
        lseek(fd, 0, SEEK_SET);

        bytes = static_cast<uint8_t *>(malloc(size));
        if (read_full(fd, bytes, size) == -1) {
            size = 0;
            free(bytes);
            bytes = nullptr;
        }
        close(fd);
    }
}

uint8_t *DexFile::getBytes() const {
    return bytes;
}

size_t DexFile::getSize() const {
    return size;
}

void DexFile::createInMemoryDexClassLoader(JNIEnv *env) {
    if (!bytes) return;

    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID getSystemClassLoaderMethod = env->GetStaticMethodID(
            classLoaderClass, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
    jobject systemClassLoader = env->CallStaticObjectMethod(classLoaderClass, getSystemClassLoaderMethod);

    dexClassLoaderClass = env->FindClass("dalvik/system/InMemoryDexClassLoader");
    findClassMethod = env->GetMethodID(dexClassLoaderClass, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    dexClassLoaderClass = (jclass) env->NewGlobalRef(dexClassLoaderClass);

    jmethodID initDexClassLoaderMethod = env->GetMethodID(
            dexClassLoaderClass, "<init>", "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
    jobject buffer = env->NewDirectByteBuffer(bytes, size);
    dexClassLoader = env->NewObject(dexClassLoaderClass, initDexClassLoaderMethod, buffer, systemClassLoader);
    if (!dexClassLoader) goto clean;
    dexClassLoader = env->NewGlobalRef(dexClassLoader);

    clean:
    if (env->ExceptionCheck()) env->ExceptionClear();
    env->DeleteLocalRef(systemClassLoader);
    env->DeleteLocalRef(classLoaderClass);
}

void DexFile::createDexClassLoader(JNIEnv *env, const char *dexDir, const char *dexName, const char *optDir) {
    if (!bytes) return;

    mkdirs(dexDir, 0700);
    mkdirs(optDir, 0700);

    char dexPath[PATH_MAX];
    sprintf(dexPath, "%s/%s", dexDir, dexName);

    int fd = open(dexPath, O_CREAT | O_WRONLY | O_TRUNC, 0700);
    if (fd == -1) {
        PLOGE("open %s", dexPath);
        return;
    }
    if (write_full(fd, bytes, size) == -1) {
        close(fd);
        PLOGE("write");
        return;
    }
    close(fd);

    jstring jDexPath = env->NewStringUTF(dexPath);
    jstring jOptDir = optDir ? env->NewStringUTF(optDir) : nullptr;

    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID getSystemClassLoaderMethod = env->GetStaticMethodID(
            classLoaderClass, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
    jobject systemClassLoader = env->CallStaticObjectMethod(classLoaderClass, getSystemClassLoaderMethod);

    dexClassLoaderClass = env->FindClass("dalvik/system/DexClassLoader");
    findClassMethod = env->GetMethodID(dexClassLoaderClass, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    dexClassLoaderClass = (jclass) env->NewGlobalRef(dexClassLoaderClass);

    jmethodID initDexClassLoaderMethod = env->GetMethodID(
            dexClassLoaderClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");
    dexClassLoader = env->NewObject(dexClassLoaderClass, initDexClassLoaderMethod, jDexPath, jOptDir, nullptr, systemClassLoader);
    if (!dexClassLoader) goto clean;
    dexClassLoader = env->NewGlobalRef(dexClassLoader);

    clean:
    if (env->ExceptionCheck()) env->ExceptionClear();
    env->DeleteLocalRef(classLoaderClass);
    if (jOptDir) env->DeleteLocalRef(jOptDir);
    env->DeleteLocalRef(jDexPath);
}

jclass DexFile::findClass(JNIEnv *env, const char *name) {
    if (!dexClassLoader) return nullptr;

    jstring jName = env->NewStringUTF(name);
    auto cls = (jclass) env->CallObjectMethod(dexClassLoader, findClassMethod, jName);
    if (env->ExceptionCheck()) env->ExceptionClear();
    return cls;
}
