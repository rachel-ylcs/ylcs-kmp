#pragma once

#include <native_jni.h>
#include <pag.h>

class JPAGSurface {
public:
    explicit JPAGSurface(std::shared_ptr<pag::PAGSurface> pagSurface) : pagSurface(pagSurface) { }

    std::shared_ptr<pag::PAGSurface> get() {
        std::lock_guard<std::mutex> autoLock(locker);
        return pagSurface;
    }

    void clear() {
        std::lock_guard<std::mutex> autoLock(locker);
        pagSurface = nullptr;
    }

private:
    std::shared_ptr<pag::PAGSurface> pagSurface;
    std::mutex locker;
};