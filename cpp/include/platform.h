#ifndef CPP_PLATFORM_H
#define CPP_PLATFORM_H

#include <jni.h>
#include <string>

#if defined(_WIN32)
#include "platform_win.h"
#elif defined(__APPLE__)
#include "platform_mac.h"
#else
#include "platform_linux.h"
#endif

#endif