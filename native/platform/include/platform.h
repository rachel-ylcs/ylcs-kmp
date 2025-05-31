#ifndef PLATFORM_H
#define PLATFORM_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>
#include <string>

#ifdef __cplusplus
extern "C" {
#endif

// 窗口相关操作
void ylcs_window_set_click_through(void *handle, bool enable);

// 限制单实例运行
bool ylcs_single_instance_try_lock();
void ylcs_single_instance_unlock();

#ifdef __cplusplus
}
#endif

#endif // PLATFORM_H
