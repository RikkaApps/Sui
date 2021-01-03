#pragma once

class DexFile {

private:
    const char *filepath = nullptr;
    uint8_t *bytes = nullptr;
    size_t size = 0;
    jclass dexClassLoaderClass = nullptr;
    jmethodID findClassMethod = nullptr;
    jobject dexClassLoader = nullptr;

public:
    DexFile(const char *path);

    void destroy(JNIEnv *env);

    uint8_t *getBytes() const;

    size_t getSize() const;

    void createInMemoryDexClassLoader(JNIEnv *env);

    void createDexClassLoader(JNIEnv *env, const char *dexDir, const char *dexName, const char *optDir);

    jclass findClass(JNIEnv *env, const char *name);
};