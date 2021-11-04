#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <logging.h>
#include <unistd.h>

/*
 * ashmem_create_region - creates a new ashmem region and returns the file
 * descriptor, or <0 on error
 *
 * `name' is an optional label to give the region (visible in /proc/pid/maps)
 * `size' is the size of the region, in page-aligned bytes
 */
using ashmem_create_region_t = int(const char *name, size_t size);

static ashmem_create_region_t *ashmem_create_region = nullptr;

using ashmem_set_prot_region_t = int(int fd, int prot);

static ashmem_set_prot_region_t *ashmem_set_prot_region = nullptr;

static void Init() {
    static bool init = false;
    if (init) return;

#ifdef __LP64__
    auto handle = dlopen("/system/lib64/libcutils.so", 0);
#else
    auto handle = dlopen("/system/lib/libcutils.so", 0);
#endif

    if (handle) {
        ashmem_create_region = (ashmem_create_region_t *) dlsym(handle, "ashmem_create_region");
        ashmem_set_prot_region = (ashmem_set_prot_region_t *) dlsym(handle, "ashmem_set_prot_region");
    }

    init = true;
}

int CreateSharedMem(const char *name, size_t size) {
    Init();
    if (!ashmem_create_region) return -1;

    int ret;
    if ((ret = ashmem_create_region(name, size)) >= 0) {
        return ret;
    }
    PLOGE("ashmem_create_region %s", name);
    return ret;
}

int SetSharedMemProt(int fd, int prot) {
    Init();
    if (!ashmem_create_region) return 0;

    int ret;
    if ((ret = ashmem_set_prot_region(fd, prot)) == -1) {
        PLOGE("ashmem_set_prot_region");
    }
    return ret;
}
