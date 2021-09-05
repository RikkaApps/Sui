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

#include <jni.h>

namespace BinderHook {

    using ExecTransact_t = bool(jboolean *, JNIEnv *, jobject, va_list);

    void Install(JavaVM *javaVm, JNIEnv *env, ExecTransact_t *callback);

    void Uninstall(JavaVM *javaVm);

    void Uninstall(JNIEnv *env);
}