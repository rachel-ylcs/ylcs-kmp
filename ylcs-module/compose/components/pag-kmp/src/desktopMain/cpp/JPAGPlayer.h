#pragma once

#include <native_jni.h>
#include <pag.h>

class JPAGPlayer {
public:
    explicit JPAGPlayer(std::shared_ptr<pag::PAGPlayer> pagPlayer) : pagPlayer(pagPlayer) { }

    std::shared_ptr<pag::PAGPlayer> get() {
        std::lock_guard<std::mutex> autoLock(locker);
        return pagPlayer;
    }

    void clear() {
        std::lock_guard<std::mutex> autoLock(locker);
        pagPlayer = nullptr;
    }

private:
    std::shared_ptr<pag::PAGPlayer> pagPlayer;
    std::mutex locker;
};