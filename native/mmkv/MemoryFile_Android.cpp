/*
 * Tencent is pleased to support the open source community by making
 * MMKV available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *       https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "MemoryFile.h"

#ifdef MMKV_ANDROID

#    include "MMBuffer.h"
#    include "MMKVLog.h"
#    include <cerrno>
#    include <fcntl.h>
#    include <sys/mman.h>
#    include <sys/stat.h>
#    include <unistd.h>

using namespace std;

constexpr char ASHMEM_NAME_DEF[] = "/dev/ashmem";

namespace mmkv {

// for Android Q limiting ashmem access
extern int ASharedMemory_create(const char *name, size_t size);
extern size_t ASharedMemory_getSize(int fd);
extern string ASharedMemory_getName(int fd);

File::File(MMKVPath_t path, OpenFlag flag, size_t size, FileType fileType)
    : m_path(std::move(path)), m_fd(-1), m_flag(flag), m_size(0), m_fileType(fileType) {
    if (m_fileType == MMFILE_TYPE_FILE) {
        open();
    } else {
        // round up to (n * pagesize)
        if (size < DEFAULT_MMAP_SIZE || (size % DEFAULT_MMAP_SIZE != 0)) {
            size = ((size / DEFAULT_MMAP_SIZE) + 1) * DEFAULT_MMAP_SIZE;
        }
        auto filename = m_path.c_str();
        auto ptr = strstr(filename, ASHMEM_NAME_DEF);
        if (ptr && ptr[sizeof(ASHMEM_NAME_DEF) - 1] == '/') {
            filename = ptr + sizeof(ASHMEM_NAME_DEF);
        }
        m_fd = ASharedMemory_create(filename, size);
        if (isFileValid()) {
            m_size = size;
        }
    }
}

File::File(MMKVFileHandle_t ashmemFD)
    : m_path(), m_fd(ashmemFD), m_flag(OpenFlag::ReadWrite), m_size(0), m_fileType(MMFILE_TYPE_ASHMEM) {
    if (isFileValid()) {
        m_path = ASharedMemory_getName(m_fd);
        m_size = ASharedMemory_getSize(m_fd);
    }
}

MemoryFile::MemoryFile(string path, size_t size, FileType fileType, size_t expectedCapacity, bool isReadOnly, bool mayflyFD)
    : m_diskFile(std::move(path), isReadOnly ? OpenFlag::ReadOnly : (OpenFlag::ReadWrite | OpenFlag::Create), size, fileType),
    m_ptr(nullptr), m_size(0), m_fileType(fileType), m_readOnly(isReadOnly), m_isMayflyFD(mayflyFD) {
    if (m_fileType == MMFILE_TYPE_FILE) {
        reloadFromFile(expectedCapacity);
    } else {
        if (m_diskFile.isFileValid()) {
            m_size = m_diskFile.m_size;
            mmapOrCleanup(nullptr);
        }
    }
}

MemoryFile::MemoryFile(int ashmemFD)
    : m_diskFile(ashmemFD), m_ptr(nullptr), m_size(0), m_fileType(MMFILE_TYPE_ASHMEM), m_readOnly(false),
    m_isMayflyFD(false) {
    if (!m_diskFile.isFileValid()) {
        MMKVError("fd %d invalid", ashmemFD);
    } else {
        m_size = m_diskFile.m_size;
        MMKVInfo("ashmem name:%s, size:%zu", m_diskFile.m_path.c_str(), m_size);
        mmapOrCleanup(nullptr);
    }
}

} // namespace mmkv

#    pragma mark - ashmem
#    include <dlfcn.h>
#    include <sys/ioctl.h>

namespace mmkv {

constexpr auto ASHMEM_NAME_LEN = 256;
constexpr auto ASHMEM_IOC = 0x77;
#    define ASHMEM_SET_NAME _IOW(ASHMEM_IOC, 1, char[ASHMEM_NAME_LEN])
#    define ASHMEM_GET_NAME _IOR(ASHMEM_IOC, 2, char[ASHMEM_NAME_LEN])
#    define ASHMEM_SET_SIZE _IOW(ASHMEM_IOC, 3, size_t)
#    define ASHMEM_GET_SIZE _IO(ASHMEM_IOC, 4)

#ifndef MMKV_OHOS
int g_android_api = __ANDROID_API_L__;
#endif
std::string g_android_tmpDir = "/data/local/tmp/";

#ifndef MMKV_OHOS
void *loadLibrary() {
    auto name = "libandroid.so";
    static auto handle = dlopen(name, RTLD_LAZY | RTLD_LOCAL);
    if (handle == RTLD_DEFAULT) {
        MMKVError("unable to load library %s", name);
    }
    return handle;
}

typedef int (*AShmem_create_t)(const char *name, size_t size);
typedef size_t (*AShmem_getSize_t)(int fd);

#endif

int ASharedMemory_create(const char *name, size_t size) {
#ifndef MMKV_OHOS
    if (g_android_api >= __ANDROID_API_O__ || g_android_api >= __ANDROID_API_M__) {
        static auto handle = loadLibrary();
        static AShmem_create_t funcPtr =
            (handle != nullptr) ? reinterpret_cast<AShmem_create_t>(dlsym(handle, "ASharedMemory_create")) : nullptr;
        if (funcPtr) {
            int fd = funcPtr(name, size);
            if (fd < 0) {
                MMKVError("fail to ASharedMemory_create %s with size %zu, errno:%s", name, size, strerror(errno));
            } else {
                MMKVInfo("ASharedMemory_create %s with size %zu, fd:%d", name, size, fd);
                return fd;
            }
        } else if (g_android_api >= __ANDROID_API_O__) {
            MMKVWarning("fail to locate ASharedMemory_create() from loading libandroid.so");
        }

        static AShmem_create_t regionFuncPtr =
            (handle != nullptr) ? reinterpret_cast<AShmem_create_t>(dlsym(handle, "ashmem_create_region")) : nullptr;
        if (regionFuncPtr) {
            int fd = regionFuncPtr(name, size);
            if (fd < 0) {
                MMKVError("fail to ashmem_create_region %s with size %zu, errno:%s", name, size, strerror(errno));
            } else {
                MMKVInfo("ashmem_create_region %s with size %zu, fd:%d", name, size, fd);
                return fd;
            }
        } else {
            MMKVWarning("fail to locate ashmem_create_region() from loading libandroid.so");
        }
    }
#endif
    int fd = open(ASHMEM_NAME_DEF, O_RDWR | O_CLOEXEC);
    if (fd < 0) {
        MMKVError("fail to open ashmem:%s, %s", name, strerror(errno));
    } else {
        if (ioctl(fd, ASHMEM_SET_NAME, name) != 0) {
            MMKVError("fail to set ashmem name:%s, %s", name, strerror(errno));
        } else if (ioctl(fd, ASHMEM_SET_SIZE, size) != 0) {
            MMKVError("fail to set ashmem:%s, size %zu, %s", name, size, strerror(errno));
        }
    }
    return fd;
}

size_t ASharedMemory_getSize(int fd) {
    size_t size = 0;
#ifndef MMKV_OHOS
    if (g_android_api >= __ANDROID_API_O__) {
        static auto handle = loadLibrary();
        static AShmem_getSize_t funcPtr =
            (handle != nullptr) ? reinterpret_cast<AShmem_getSize_t>(dlsym(handle, "ASharedMemory_getSize")) : nullptr;
        if (funcPtr) {
            size = funcPtr(fd);
            if (size == 0) {
                MMKVError("fail to ASharedMemory_getSize:%d, %s", fd, strerror(errno));
            }
        } else {
            MMKVWarning("fail to locate ASharedMemory_create() from loading libandroid.so");
        }
    }
#endif
    if (size == 0) {
        int tmp = ioctl(fd, ASHMEM_GET_SIZE, nullptr);
        if (tmp < 0) {
            MMKVError("fail to get ashmem size:%d, %s", fd, strerror(errno));
        } else {
            size = static_cast<size_t>(tmp);
        }
    }
    return size;
}

string ASharedMemory_getName(int fd) {
    // Android Q doesn't have ASharedMemory_getName()
    // I've make a request to Google, https://issuetracker.google.com/issues/130741665
    // There's nothing we can do before it's supported officially by Google
#ifndef MMKV_OHOS
    if (g_android_api >= __ANDROID_API_O__) {
        return "";
    }
#endif

    char name[ASHMEM_NAME_LEN] = {0};
    if (ioctl(fd, ASHMEM_GET_NAME, name) != 0) {
        MMKVError("fail to get ashmem name:%d, %s", fd, strerror(errno));
        return "";
    }
    return {name};
}

} // namespace mmkv

MMKVPath_t ashmemMMKVPathWithID(const MMKVPath_t &mmapID) {
    return MMKVPath_t(ASHMEM_NAME_DEF) + MMKV_PATH_SLASH + mmapID;
}

static long long timespec_to_ms(struct timespec ts) {
    return (long long)ts.tv_sec * 1000LL + ts.tv_nsec / 1000000LL;
}

long long getFileModifyTimeInMS(const char *path) {
    if (!path) {
        return -1;
    }
    struct stat soStat = {};
    if (::stat(path, &soStat) < 0) {
        MMKVError("fail to stat %s: %d(%s)", path, errno, strerror(errno));
        return -1;
    }
    return timespec_to_ms(soStat.st_mtim);
}

#endif // MMKV_ANDROID
