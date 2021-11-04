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

#pragma once

class Buffer {

protected:
    uint8_t *bytes_ = nullptr;
    size_t size_ = 0;
private:
    bool is_mmap_ = 0;

public:
    Buffer() = default;

    Buffer(const char *path);

    Buffer(int fd, size_t size);

    ~Buffer();

    uint8_t *data() const;

    size_t size() const;

    int writeToFile(const char *path, mode_t mode);
};

class Dex {

private:
    Buffer buffer_;
    char *pre26DexPath_ = nullptr;
    char *pre26OptDir_ = nullptr;

    jclass dexClassLoaderClass = nullptr;
    jmethodID findClassMethod = nullptr;
    jobject dexClassLoader = nullptr;

public:
    Dex(int fd, size_t size);

    Dex(const char *path);

    ~Dex();

    void destroy(JNIEnv *env);

    void createClassLoader(JNIEnv *env);

    jclass findClass(JNIEnv *env, const char *name);

    void setPre26Paths(const char *dexPath, const char *optDir);

    bool valid();

private:

    void createInMemoryDexClassLoader(JNIEnv *env);

    void createDexClassLoader(JNIEnv *env, const char *path, const char *optDir);

    void copyDexToFile(const char *dexPath);

};
