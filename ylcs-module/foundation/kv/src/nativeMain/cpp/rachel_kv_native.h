#ifndef RACHEL_KV_NATIVE_H
#define RACHEL_KV_NATIVE_H

#include <string>

namespace mmkv {
    class MMBuffer;
}

bool rachel_kv_read_string(void *handle, const char *key, size_t key_size, std::string &value);
bool rachel_kv_read_bytes(void *handle, const char *key, size_t key_size, mmkv::MMBuffer &value);

#endif
