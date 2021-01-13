#pragma once

class File {

protected:
    uint8_t *bytes = nullptr;
    size_t size = 0;

public:
    File(const char *path);

    ~File();

    uint8_t *getBytes() const;

    size_t getSize() const;
};

class DexFile : public File {

private:
    jclass dexClassLoaderClass = nullptr;
    jmethodID findClassMethod = nullptr;
    jobject dexClassLoader = nullptr;

public:

    DexFile(const char *path);

    void destroy(JNIEnv *env);

    void createInMemoryDexClassLoader(JNIEnv *env);

    void createDexClassLoader(JNIEnv *env, const char *dexDir, const char *dexName, const char *optDir);

    jclass findClass(JNIEnv *env, const char *name);
};