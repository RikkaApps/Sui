#include <zygisk.hpp>

static void companion_handler(int socket) {

}

class SuiModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override {
    }

    void preServerSpecialize(zygisk::ServerSpecializeArgs *args) override {
    }

    void postServerSpecialize(const zygisk::ServerSpecializeArgs *args) override {
    }

private:
    zygisk::Api *api{};
    JNIEnv *env{};
};

REGISTER_ZYGISK_MODULE(SuiModule)

REGISTER_ZYGISK_COMPANION(companion_handler)
