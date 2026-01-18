#pragma once

#include <native_jni.h>
#include <pag.h>

class JPAGLayer {
public:
    explicit JPAGLayer(std::shared_ptr<pag::PAGLayer> pagLayer) : pagLayer(pagLayer) { }

    std::shared_ptr<pag::PAGLayer> get() {
        return pagLayer;
    }
private:
    std::shared_ptr<pag::PAGLayer> pagLayer;
};