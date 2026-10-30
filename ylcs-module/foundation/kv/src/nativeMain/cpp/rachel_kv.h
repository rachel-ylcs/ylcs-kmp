#ifndef RACHEL_KV_H
#define RACHEL_KV_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void *rachel_kv_open(const char *path);
void rachel_kv_close(void *handle);
bool rachel_kv_set_bool(void *handle, const char *key, bool value, uint32_t expire);
bool rachel_kv_set_int(void *handle, const char *key, int32_t value, uint32_t expire);
bool rachel_kv_set_long(void *handle, const char *key, int64_t value, uint32_t expire);
bool rachel_kv_set_float(void *handle, const char *key, float value, uint32_t expire);
bool rachel_kv_set_double(void *handle, const char *key, double value, uint32_t expire);
bool rachel_kv_set_data(void *handle, const char *key, const uint8_t *value, size_t size, uint32_t expire, bool string_value);
bool rachel_kv_get_bool(void *handle, const char *key, bool fallback);
int32_t rachel_kv_get_int(void *handle, const char *key, int32_t fallback);
int64_t rachel_kv_get_long(void *handle, const char *key, int64_t fallback);
float rachel_kv_get_float(void *handle, const char *key, float fallback);
double rachel_kv_get_double(void *handle, const char *key, double fallback);
uint8_t *rachel_kv_get_data(void *handle, const char *key, size_t *size, bool string_value);
void rachel_kv_free(void *data);
bool rachel_kv_contains(void *handle, const char *key);
void rachel_kv_remove(void *handle, const char *key);

#ifdef __cplusplus
}
#endif

#endif
