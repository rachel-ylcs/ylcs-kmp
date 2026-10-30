#include "rachel_kv.h"
#include "rachel_kv_native.h"
#include <MMKV.h>
#include <MMBuffer.h>
#include <memory>
#include <stdexcept>
#include <string_view>
#include <mutex>
#include <unordered_map>

using namespace mmkv;

namespace {
    std::mutex handles_mutex;
    std::unordered_map<MMKV *, size_t> handles;
    bool initialized = false;

    std::string key_string(const char *key, size_t size) {
        if (!key && size) throw std::invalid_argument("Invalid KV key");
        return std::string(key ? key : "", size);
    }

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

bool rachel_kv_read_string(void *handle, const char *key, size_t key_size, std::string &value) {
    return guarded(false, [&]() {
        auto *kv = static_cast<MMKV *>(handle);
        return kv && kv->getString(key_string(key, key_size), value);
    });
}

bool rachel_kv_read_bytes(void *handle, const char *key, size_t key_size, MMBuffer &value) {
    return guarded(false, [&]() {
        auto *kv = static_cast<MMKV *>(handle);
        return kv && kv->getBytes(key_string(key, key_size), value);
    });
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

    bool rachel_kv_set_bool(void *handle, const char *key, size_t key_size, bool value, uint32_t expire) {
        return guarded(false, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv && kv->set(value, key_string(key, key_size), expire);
        });
    }

    bool rachel_kv_get_bool(void *handle, const char *key, size_t key_size, bool fallback) {
        return guarded(fallback, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv ? kv->getBool(key_string(key, key_size), fallback) : fallback;
        });
    }

    bool rachel_kv_set_int(void *handle, const char *key, size_t key_size, int32_t value, uint32_t expire) {
        return guarded(false, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv && kv->set(value, key_string(key, key_size), expire);
        });
    }

    int32_t rachel_kv_get_int(void *handle, const char *key, size_t key_size, int32_t fallback) {
        return guarded(fallback, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv ? kv->getInt32(key_string(key, key_size), fallback) : fallback;
        });
    }

    bool rachel_kv_set_long(void *handle, const char *key, size_t key_size, int64_t value, uint32_t expire) {
        return guarded(false, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv && kv->set(value, key_string(key, key_size), expire);
        });
    }

    int64_t rachel_kv_get_long(void *handle, const char *key, size_t key_size, int64_t fallback) {
        return guarded(fallback, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv ? kv->getInt64(key_string(key, key_size), fallback) : fallback;
        });
    }

    bool rachel_kv_set_float(void *handle, const char *key, size_t key_size, float value, uint32_t expire) {
        return guarded(false, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv && kv->set(value, key_string(key, key_size), expire);
        });
    }

    float rachel_kv_get_float(void *handle, const char *key, size_t key_size, float fallback) {
        return guarded(fallback, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv ? kv->getFloat(key_string(key, key_size), fallback) : fallback;
        });
    }

    bool rachel_kv_set_double(void *handle, const char *key, size_t key_size, double value, uint32_t expire) {
        return guarded(false, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv && kv->set(value, key_string(key, key_size), expire);
        });
    }

    double rachel_kv_get_double(void *handle, const char *key, size_t key_size, double fallback) {
        return guarded(fallback, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv ? kv->getDouble(key_string(key, key_size), fallback) : fallback;
        });
    }

    bool rachel_kv_set_string(void *handle, const char *key, size_t key_size, const uint8_t *value, size_t size, uint32_t expire) {
        return guarded(false, [&]() {
            if (!handle || (!value && size)) return false;
            auto *kv = static_cast<MMKV *>(handle);
            auto text = std::string_view(size ? reinterpret_cast<const char *>(value) : "", size);
            return kv->set(text, key_string(key, key_size), expire);
        });
    }

    bool rachel_kv_set_bytes(void *handle, const char *key, size_t key_size, const uint8_t *value, size_t size, uint32_t expire) {
        return guarded(false, [&]() {
            if (!handle || (!value && size)) return false;
            auto *kv = static_cast<MMKV *>(handle);
            uint8_t empty = 0;
            MMBuffer buffer(size ? const_cast<uint8_t *>(value) : &empty, size, MMBufferNoCopy);
            return kv->set(buffer, key_string(key, key_size), expire);
        });
    }

    bool rachel_kv_get_string(void *handle, const char *key, size_t key_size, RachelKVValue *value) {
        if (!value) return false;
        *value = {};
        return guarded(false, [&]() {
            if (!handle) return false;
            std::string text;
            if (!rachel_kv_read_string(handle, key, key_size, text)) return false;
            auto owner = std::make_unique<std::string>(std::move(text));
            value->data = owner->c_str();
            value->size = owner->size();
            value->kind = RACHEL_KV_UTF8;
            value->owner = owner.release();
            return true;
        });
    }

    bool rachel_kv_get_bytes(void *handle, const char *key, size_t key_size, RachelKVValue *value) {
        if (!value) return false;
        *value = {};
        return guarded(false, [&]() {
            if (!handle) return false;
            MMBuffer buffer;
            if (!rachel_kv_read_bytes(handle, key, key_size, buffer)) return false;
            auto owner = std::make_unique<MMBuffer>(std::move(buffer));
            value->data = owner->getPtr();
            value->size = owner->length();
            value->kind = RACHEL_KV_BYTES;
            value->owner = owner.release();
            return true;
        });
    }

    void rachel_kv_release_value(RachelKVValue *value) {
        if (!value) return;
        if (value->kind == RACHEL_KV_UTF8) delete static_cast<std::string *>(value->owner);
        else delete static_cast<MMBuffer *>(value->owner);
        *value = {};
    }

    bool rachel_kv_contains(void *handle, const char *key, size_t key_size) {
        return guarded(false, [&]() {
            auto *kv = static_cast<MMKV *>(handle);
            return kv && kv->containsKey(key_string(key, key_size));
        });
    }

    void rachel_kv_remove(void *handle, const char *key, size_t key_size) {
        guarded(false, [&]() {
            if (!handle) return false;
            auto *kv = static_cast<MMKV *>(handle);
            kv->removeValueForKey(key_string(key, key_size));
            return true;
        });
    }
}
