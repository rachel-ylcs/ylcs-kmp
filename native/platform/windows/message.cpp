#include "platform.h"

#include <Windows.h>

static HANDLE appEvent = NULL;

bool ylcs_single_instance_try_lock() {
    const LPCSTR name = "ylcs-desktop";
    appEvent = OpenEventA(EVENT_ALL_ACCESS, FALSE, name);
    if (appEvent == NULL) {
        appEvent = CreateEventA(NULL, FALSE, FALSE, name);
        return true;
    }
    return false;
}

void ylcs_single_instance_unlock() {
    if (appEvent != NULL) {
        CloseHandle(appEvent);
        appEvent = NULL;
    }
}
