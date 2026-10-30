#include "rachel_kv.h"
#include <MMKV.h>
#include <MMBuffer.h>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include <unordered_map>

using namespace mmkv;

namespace {
    std::mutex handles_mutex;
    std::unordered_map<MMKV *, size_t> handles;
    bool initialized = false;

    template <typename R, typename F>
    R guarded(R fallback, F action) noexcept {
        try {
            return action();
        }
        catch (...) {
            return fallback;
        }
    }
}

extern "C" {
    void *rachel_kv_open(const char *path) {
        return guarded<void *>(nullptr, [&]() -> void * {
            if (!path) return nullptr;
            std::lock_guard<std::mutex> lock(handles_mutex);
            auto directory = string2MMKVPath_t(std::string(path));
            if (!initialized) {
                MMKV::initializeMMKV(directory, MMKVLogLevel::MMKVLogNone);
                initialized = true;
            }
            MMKVConfig config;
            config.rootPath = &directory;
            auto *kv = MMKV::defaultMMKV(config);
            if (!kv) return nullptr;
            if (!kv->enableAutoKeyExpire(MMKV::ExpireNever)) {
                if (handles.find(kv) == handles.end()) kv->close();
                return nullptr;
            }
            ++handles[kv];
            return kv;
        });
    }

    void rachel_kv_close(void *handle) {
        guarded(false, [&]() {
            std::lock_guard<std::mutex> lock(handles_mutex);
            auto *kv = static_cast<MMKV *>(handle);
            auto entry = handles.find(kv);
            if (entry != handles.end() && --entry->second == 0) {
                handles.erase(entry);
                kv->close();
            }
            return true;
        });
    }

    bool rachel_kv_set_bool(void *handle, const char *key, bool value, uint32_t expire) {
        return guarded(false, [&]() { return handle && key && static_cast<MMKV *>(handle)->set(value, key, expire); });
    }

    bool rachel_kv_get_bool(void *handle, const char *key, bool fallback) {
        return guarded(fallback, [&]() { return handle && key ? static_cast<MMKV *>(handle)->getBool(key, fallback) : fallback; });
    }

    bool rachel_kv_set_int(void *handle, const char *key, int32_t value, uint32_t expire) {
        return guarded(false, [&]() { return handle && key && static_cast<MMKV *>(handle)->set(value, key, expire); });
    }

    int32_t rachel_kv_get_int(void *handle, const char *key, int32_t fallback) {
        return guarded(fallback, [&]() { return handle && key ? static_cast<MMKV *>(handle)->getInt32(key, fallback) : fallback; });
    }

    bool rachel_kv_set_long(void *handle, const char *key, int64_t value, uint32_t expire) {
        return guarded(false, [&]() { return handle && key && static_cast<MMKV *>(handle)->set(value, key, expire); });
    }

    int64_t rachel_kv_get_long(void *handle, const char *key, int64_t fallback) {
        return guarded(fallback, [&]() { return handle && key ? static_cast<MMKV *>(handle)->getInt64(key, fallback) : fallback; });
    }

    bool rachel_kv_set_float(void *handle, const char *key, float value, uint32_t expire) {
        return guarded(false, [&]() { return handle && key && static_cast<MMKV *>(handle)->set(value, key, expire); });
    }

    float rachel_kv_get_float(void *handle, const char *key, float fallback) {
        return guarded(fallback, [&]() { return handle && key ? static_cast<MMKV *>(handle)->getFloat(key, fallback) : fallback; });
    }

    bool rachel_kv_set_double(void *handle, const char *key, double value, uint32_t expire) {
        return guarded(false, [&]() { return handle && key && static_cast<MMKV *>(handle)->set(value, key, expire); });
    }

    double rachel_kv_get_double(void *handle, const char *key, double fallback) {
        return guarded(fallback, [&]() { return handle && key ? static_cast<MMKV *>(handle)->getDouble(key, fallback) : fallback; });
    }

    bool rachel_kv_set_data(void *handle, const char *key, const uint8_t *value, size_t size, uint32_t expire, bool string_value) {
        return guarded(false, [&]() {
            if (!handle || !key || (!value && size)) return false;
            auto *kv = static_cast<MMKV *>(handle);
            if (string_value) return kv->set(size ? std::string(reinterpret_cast<const char *>(value), size) : std::string(), key, expire);
            MMBuffer buffer(const_cast<uint8_t *>(value), size, MMBufferNoCopy);
            return kv->set(buffer, key, expire);
        });
    }

    uint8_t *rachel_kv_get_data(void *handle, const char *key, size_t *size, bool string_value) {
        if (size) *size = 0;
        return guarded<uint8_t *>(nullptr, [&]() -> uint8_t * {
            if (!handle || !key || !size) return nullptr;
            auto *kv = static_cast<MMKV *>(handle);
            if (!kv->containsKey(key)) return nullptr;
            std::string text;
            MMBuffer buffer;
            const void *data;
            size_t length;
            if (string_value) {
                if (!kv->getString(key, text)) return nullptr;
                data = text.data();
                length = text.size();
            }
            else {
                if (!kv->getBytes(key, buffer)) return nullptr;
                data = buffer.getPtr();
                length = buffer.length();
            }
            auto *result = static_cast<uint8_t *>(std::malloc(length ? length : 1));
            if (!result) return nullptr;
            if (length) std::memcpy(result, data, length);
            *size = length;
            return result;
        });
    }

    void rachel_kv_free(void *data) {
        std::free(data);
    }

    bool rachel_kv_contains(void *handle, const char *key) {
        return guarded(false, [&]() { return handle && key && static_cast<MMKV *>(handle)->containsKey(key); });
    }

    void rachel_kv_remove(void *handle, const char *key) {
        guarded(false, [&]() {
            if (handle && key) static_cast<MMKV *>(handle)->removeValueForKey(key);
            return true;
        });
    }
}
