#pragma once

#include <native_jni.h>
#include <pag.h>

class JPAGShapeLayer {
public:
    explicit JPAGShapeLayer(std::shared_ptr<pag::PAGShapeLayer> pagLayer) : pagLayer(pagLayer) { }

    std::shared_ptr<pag::PAGShapeLayer> get() {
        return pagLayer;
    }
private:
    std::shared_ptr<pag::PAGShapeLayer> pagLayer;
};