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