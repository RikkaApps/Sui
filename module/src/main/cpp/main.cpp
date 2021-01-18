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

static DexFile *dexFile = nullptr;
static std::vector<File *> *resources_files = nullptr;

static void PrepareFiles() {
    if (dexFile && dexFile->getBytes()) return;
    dexFile = new DexFile(DEX_PATH);

    resources_files = new std::vector<File *>();
    resources_files->emplace_back(new File(RES_PATH "/layout/confirmation_dialog.xml"));
    resources_files->emplace_back(new File(RES_PATH "/layout/management_dialog.xml"));
    resources_files->emplace_back(new File(RES_PATH "/layout/management_app_item.xml"));
    resources_files->emplace_back(new File(RES_PATH "/drawable/ic_su_24.xml"));
    resources_files->emplace_back(new File(RES_PATH "/drawable/ic_close_24.xml"));
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
static int saved_uid;

static void appProcessPre(JNIEnv *env, const jint *uid, jstring *jAppDataDir) {

    PrepareFiles();

    saved_uid = *uid;

    memset(saved_package_name, 0, 256);
    memset(saved_app_data_dir, 0, 256);

    if (*jAppDataDir) {
        auto appDataDir = ScopedUtfChars(env, *jAppDataDir).c_str();
        strcpy(saved_app_data_dir, appDataDir);

        int user = 0;

        // /data/user/<user_id>/<package>
        if (sscanf(appDataDir, "/data/%*[^/]/%d/%s", &user, saved_package_name) == 2)
            goto found;

        // /mnt/expand/<id>/user/<user_id>/<package>
        if (sscanf(appDataDir, "/mnt/expand/%*[^/]/%*[^/]/%d/%s", &user, saved_package_name) == 2)
            goto found;

        // /data/data/<package>
        if (sscanf(appDataDir, "/data/%*[^/]/%s", saved_package_name) == 1)
            goto found;

        // nothing found
        saved_package_name[0] = '\0';

        found:;
    }
}

static void appProcessPost(
        JNIEnv *env, const char *from, const char *package_name, const char *app_data_dir, jint uid) {

    if (strcmp(saved_package_name, MANAGER_APPLICATION_ID) == 0) {
        LOGV("%s: manager process, uid=%d, package=%s, dir=%s", from, uid, package_name, app_data_dir);
        Manager::main(env, app_data_dir, dexFile, resources_files);
    } else if (strcmp(saved_package_name, SETTINGS_APPLICATION_ID) == 0)  {
        LOGV("%s: settings process, uid=%d, package=%s, dir=%s", from, uid, package_name, app_data_dir);
        Settings::main(env, app_data_dir, dexFile, resources_files);
    } else {
        DestroyFiles(env);
    }
}

static void forkAndSpecializePre(
        JNIEnv *env, jclass clazz, jint *uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jintArray *fdsToClose, jintArray *fdsToIgnore, jboolean *is_child_zygote,
        jstring *instructionSet, jstring *appDataDir, jboolean *isTopApp, jobjectArray *pkgDataInfoList,
        jobjectArray *whitelistedDataInfoList, jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {

    appProcessPre(env, uid, appDataDir);
}

static void forkAndSpecializePost(JNIEnv *env, jclass clazz, jint res) {
    if (res == 0) {
        appProcessPost(env, "forkAndSpecialize", saved_package_name, saved_app_data_dir, saved_uid);
    }
}

static void specializeAppProcessPre(
        JNIEnv *env, jclass clazz, jint *uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jboolean *startChildZygote, jstring *instructionSet, jstring *appDataDir,
        jboolean *isTopApp, jobjectArray *pkgDataInfoList, jobjectArray *whitelistedDataInfoList,
        jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {

    appProcessPre(env, uid, appDataDir);
}

static void specializeAppProcessPost(
        JNIEnv *env, jclass clazz) {

    appProcessPost(env, "specializeAppProcess", saved_package_name, saved_app_data_dir, saved_uid);
}

static void forkSystemServerPost(JNIEnv *env, jclass clazz, jint res) {
    if (res == 0) {
        LOGV("nativeForkSystemServerPost");

        SystemServer::main(env, dexFile);
    }
}

static int shouldSkipUid(int uid) {
    return false;
}

static void onModuleLoaded() {
    PrepareFiles();
}

extern "C" {

int riru_api_version;
RiruApiV9 *riru_api_v9;

void *init(void *arg) {
    static int step = 0;
    step += 1;

    static void *_module;

    switch (step) {
        case 1: {
            auto core_max_api_version = *(int *) arg;
            riru_api_version = core_max_api_version <= RIRU_MODULE_API_VERSION ? core_max_api_version : RIRU_MODULE_API_VERSION;
            return &riru_api_version;
        }
        case 2: {
            switch (riru_api_version) {
                // RiruApiV10 and RiruModuleInfoV10 are equal to V9
                case 10:
                case 9: {
                    riru_api_v9 = (RiruApiV9 *) arg;

                    auto module = (RiruModuleInfoV9 *) malloc(sizeof(RiruModuleInfoV9));
                    memset(module, 0, sizeof(RiruModuleInfoV9));
                    _module = module;

                    module->supportHide = true;

                    module->version = RIRU_MODULE_VERSION;
                    module->versionName = RIRU_MODULE_VERSION_NAME;
                    module->onModuleLoaded = onModuleLoaded;
                    module->shouldSkipUid = shouldSkipUid;
                    module->forkAndSpecializePre = forkAndSpecializePre;
                    module->forkAndSpecializePost = forkAndSpecializePost;
                    module->specializeAppProcessPre = specializeAppProcessPre;
                    module->specializeAppProcessPost = specializeAppProcessPost;
                    module->forkSystemServerPre = nullptr;
                    module->forkSystemServerPost = forkSystemServerPost;
                    return module;
                }
                default: {
                    return nullptr;
                }
            }
        }
        case 3: {
            free(_module);
            return nullptr;
        }
        default: {
            return nullptr;
        }
    }
}
}