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
#include <sys/types.h>
#include <riru.h>
#include <malloc.h>
#include <cstring>
#include <nativehelper/scoped_utf_chars.h>
#include <climits>
#include <vector>
#include "dex_file.h"
#include "logging.h"
#include "system_server.h"
#include "config.h"
#include "manager_process.h"
#include "settings_process.h"

static uid_t manager_uid = -1, settings_uid = -1;
static char manager_process[128], settings_uid_process[128];

static void ReadApplicationInfo(const char *package, uid_t &uid, char *process) {
    char buf[PATH_MAX];
    snprintf(buf, PATH_MAX, "%s/%s", riru_get_magisk_module_path(), package);
    auto file = File(buf);
    auto bytes = file.getBytes();
    auto size = file.getSize();
    for (int i = 0; i < size; ++i) {
        if (bytes[i] == '\n') {
            memset(process, 0, 128);
            memcpy(process, bytes + i + 1, size - i - 1);
            bytes[i] = 0;
            uid = atoi((char *) bytes);
            break;
        }
    }
}

static DexFile *dexFile = nullptr;
static std::vector<File *> *resources_files = nullptr;

static void PrepareFiles() {
    if (dexFile && dexFile->getBytes()) return;

    char path[PATH_MAX]{0};
    snprintf(path, PATH_MAX, "%s/%s", riru_get_magisk_module_path(), DEX_NAME);
    dexFile = new DexFile(path);

    resources_files = new std::vector<File *>();

    snprintf(path, PATH_MAX, "%s/res/layout/confirmation_dialog.xml", riru_get_magisk_module_path());
    resources_files->emplace_back(new File(path));

    snprintf(path, PATH_MAX, "%s/res/layout/management_dialog.xml", riru_get_magisk_module_path());
    resources_files->emplace_back(new File(path));

    snprintf(path, PATH_MAX, "%s/res/layout/management_app_item.xml", riru_get_magisk_module_path());
    resources_files->emplace_back(new File(path));

    snprintf(path, PATH_MAX, "%s/res/drawable/ic_su_24.xml", riru_get_magisk_module_path());
    resources_files->emplace_back(new File(path));

    snprintf(path, PATH_MAX, "%s/res/drawable/ic_close_24.xml", riru_get_magisk_module_path());
    resources_files->emplace_back(new File(path));

    snprintf(path, PATH_MAX, "%s/res/drawable/ic_shortcut_24.xml", riru_get_magisk_module_path());
    resources_files->emplace_back(new File(path));

    ReadApplicationInfo(MANAGER_APPLICATION_ID, manager_uid, manager_process);
    ReadApplicationInfo(SETTINGS_APPLICATION_ID, settings_uid, settings_uid_process);
}

static void DestroyFiles(JNIEnv *env) {
    if (dexFile) {
        dexFile->destroy(env);
        delete dexFile;
        dexFile = nullptr;
    }
    if (resources_files) {
        for (auto *file : *resources_files) {
            delete file;
        }
        delete resources_files;
        resources_files = nullptr;
    }
}

static char saved_package_name[256] = {0};
static char saved_app_data_dir[PATH_MAX] = {0};
static char saved_process_name[PATH_MAX] = {0};
static int saved_uid;

static void appProcessPre(JNIEnv *env, const jint *uid, jstring *jAppDataDir, jstring *jNiceName) {

    PrepareFiles();

    saved_uid = *uid;

    memset(saved_package_name, 0, 256);
    memset(saved_app_data_dir, 0, 256);
    memset(saved_process_name, 0, 256);

    if (*jNiceName) {
        ScopedUtfChars niceName{env, *jNiceName};
        strcpy(saved_process_name, niceName.c_str());
    }

    if (*jAppDataDir) {
        ScopedUtfChars appDataDir{env, *jAppDataDir};
        strcpy(saved_app_data_dir, appDataDir.c_str());

        int user = 0;

        // /data/user/<user_id>/<package>
        if (sscanf(appDataDir.c_str(), "/data/%*[^/]/%d/%s", &user, saved_package_name) == 2)
            goto found;

        // /mnt/expand/<id>/user/<user_id>/<package>
        if (sscanf(appDataDir.c_str(), "/mnt/expand/%*[^/]/%*[^/]/%d/%s", &user, saved_package_name) == 2)
            goto found;

        // /data/data/<package>
        if (sscanf(appDataDir.c_str(), "/data/%*[^/]/%s", saved_package_name) == 1)
            goto found;

        // nothing found
        saved_package_name[0] = '\0';

        found:;
    }
}

static void appProcessPost(
        JNIEnv *env, const char *from, const char *package_name, const char *app_data_dir, const char *process_name, jint uid) {

    LOGD("%s:uid=%d, proc=%s, dir=%s", from, uid, saved_process_name, app_data_dir);
    LOGD("systemui %d %s", manager_uid, manager_process);
    LOGD("settings %d %s", settings_uid, settings_uid_process);

    if (manager_uid == -1 || settings_uid == -1) {
        // files are missing, assume package name is reliable and process name is package name
        if (strcmp(package_name, MANAGER_APPLICATION_ID) == 0
            && strcmp(process_name, MANAGER_APPLICATION_ID) == 0) {
            LOGV("%s: manager process, uid=%d, package=%s, dir=%s", from, uid, package_name, app_data_dir);
            Manager::main(env, app_data_dir, dexFile, resources_files);
        } else if (strcmp(package_name, SETTINGS_APPLICATION_ID) == 0
                   && strcmp(process_name, MANAGER_APPLICATION_ID) == 0) {
            LOGV("%s: settings process, uid=%d, package=%s, dir=%s", from, uid, package_name, app_data_dir);
            Settings::main(env, app_data_dir, dexFile, resources_files);
        } else {
            riru_set_unload_allowed(true);
            DestroyFiles(env);
        }
    } else {
        if (uid == manager_uid && strcmp(process_name, manager_process) == 0) {
            LOGV("%s: manager process, uid=%d, package=%s, proc=%s, dir=%s", from, uid, package_name, saved_process_name,
                 app_data_dir);
            Manager::main(env, app_data_dir, dexFile, resources_files);
        } else if (uid == settings_uid && strcmp(process_name, settings_uid_process) == 0) {
            LOGV("%s: settings process, uid=%d, package=%s, proc=%s, dir=%s", from, uid, package_name, saved_process_name,
                 app_data_dir);
            Settings::main(env, app_data_dir, dexFile, resources_files);
        } else {
            riru_set_unload_allowed(true);
            DestroyFiles(env);
        }
    }
}

static void forkAndSpecializePre(
        JNIEnv *env, jclass clazz, jint *uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jintArray *fdsToClose, jintArray *fdsToIgnore, jboolean *is_child_zygote,
        jstring *instructionSet, jstring *appDataDir, jboolean *isTopApp, jobjectArray *pkgDataInfoList,
        jobjectArray *whitelistedDataInfoList, jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {

    appProcessPre(env, uid, appDataDir, niceName);
}

static void forkAndSpecializePost(JNIEnv *env, jclass clazz, jint res) {
    if (res == 0) {
        appProcessPost(env, "forkAndSpecialize", saved_package_name, saved_app_data_dir, saved_process_name, saved_uid);
    }
}

static void specializeAppProcessPre(
        JNIEnv *env, jclass clazz, jint *uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jboolean *startChildZygote, jstring *instructionSet, jstring *appDataDir,
        jboolean *isTopApp, jobjectArray *pkgDataInfoList, jobjectArray *whitelistedDataInfoList,
        jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {

    appProcessPre(env, uid, appDataDir, niceName);
}

static void specializeAppProcessPost(
        JNIEnv *env, jclass clazz) {

    appProcessPost(env, "specializeAppProcess", saved_package_name, saved_app_data_dir, saved_process_name, saved_uid);
}

static void forkSystemServerPost(JNIEnv *env, jclass clazz, jint res) {
    if (res == 0) {
        LOGV("nativeForkSystemServerPost");

        SystemServer::main(env, dexFile);
    }
}

static void onModuleLoaded() {
    PrepareFiles();
}

extern "C" {

int riru_api_version;
const char *riru_magisk_module_path;
int *riru_allow_unload;

static auto module = RiruVersionedModuleInfo{
        .moduleApiVersion = RIRU_MODULE_API_VERSION,
        .moduleInfo= RiruModuleInfo{
                .supportHide = true,
                .version = RIRU_MODULE_VERSION,
                .versionName = RIRU_MODULE_VERSION_NAME,
                .onModuleLoaded = onModuleLoaded,
                .forkAndSpecializePre = forkAndSpecializePre,
                .forkAndSpecializePost = forkAndSpecializePost,
                .forkSystemServerPre = nullptr,
                .forkSystemServerPost = forkSystemServerPost,
                .specializeAppProcessPre = specializeAppProcessPre,
                .specializeAppProcessPost = specializeAppProcessPost
        }
};

RiruVersionedModuleInfo *init(Riru *riru) {
    auto core_max_api_version = riru->riruApiVersion;
    riru_api_version = core_max_api_version <= RIRU_MODULE_API_VERSION ? core_max_api_version : RIRU_MODULE_API_VERSION;
    module.moduleApiVersion = riru_api_version;

    riru_magisk_module_path = strdup(riru->magiskModulePath);
    if (riru_api_version >= 25) {
        riru_allow_unload = riru->allowUnload;
    }

    LOGD("Supported Riru API version: %d", riru_api_version);
    LOGD("Magisk module path: %s", riru_magisk_module_path);
    return &module;
}
}
