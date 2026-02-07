#pragma once

#include <native_jni.h>
#include <pag.h>

class JPAGDecoder {
public:
    explicit JPAGDecoder(std::shared_ptr<pag::PAGDecoder> pagDecoder) : pagDecoder(pagDecoder) { }

    std::shared_ptr<pag::PAGDecoder> get() {
        std::lock_guard<std::mutex> autoLock(locker);
        return pagDecoder;
    }

    void clear() {
        std::lock_guard<std::mutex> autoLock(locker);
        pagDecoder = nullptr;
    }

private:
    std::shared_ptr<pag::PAGDecoder> pagDecoder;
    std::mutex locker;
};