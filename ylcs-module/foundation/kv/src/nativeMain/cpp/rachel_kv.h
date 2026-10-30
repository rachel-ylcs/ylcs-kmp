#ifndef RACHEL_KV_H
#define RACHEL_KV_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum { RACHEL_KV_BYTES, RACHEL_KV_UTF8 };

typedef struct RachelKVValue {
    const void *data;
    size_t size;
    void *owner;
    int kind;
} RachelKVValue;

void *rachel_kv_open(const char *path);
void rachel_kv_close(void *handle);
bool rachel_kv_set_bool(void *handle, const char *key, size_t key_size, bool value, uint32_t expire);
bool rachel_kv_get_bool(void *handle, const char *key, size_t key_size, bool fallback);
bool rachel_kv_set_int(void *handle, const char *key, size_t key_size, int32_t value, uint32_t expire);
int32_t rachel_kv_get_int(void *handle, const char *key, size_t key_size, int32_t fallback);
bool rachel_kv_set_long(void *handle, const char *key, size_t key_size, int64_t value, uint32_t expire);
int64_t rachel_kv_get_long(void *handle, const char *key, size_t key_size, int64_t fallback);
bool rachel_kv_set_float(void *handle, const char *key, size_t key_size, float value, uint32_t expire);
float rachel_kv_get_float(void *handle, const char *key, size_t key_size, float fallback);
bool rachel_kv_set_double(void *handle, const char *key, size_t key_size, double value, uint32_t expire);
double rachel_kv_get_double(void *handle, const char *key, size_t key_size, double fallback);
bool rachel_kv_set_string(void *handle, const char *key, size_t key_size, const uint8_t *value, size_t size, uint32_t expire);
bool rachel_kv_set_bytes(void *handle, const char *key, size_t key_size, const uint8_t *value, size_t size, uint32_t expire);
bool rachel_kv_get_string(void *handle, const char *key, size_t key_size, RachelKVValue *value);
bool rachel_kv_get_bytes(void *handle, const char *key, size_t key_size, RachelKVValue *value);
void rachel_kv_release_value(RachelKVValue *value);
bool rachel_kv_contains(void *handle, const char *key, size_t key_size);
void rachel_kv_remove(void *handle, const char *key, size_t key_size);

#ifdef __cplusplus
}
#endif

#endif
