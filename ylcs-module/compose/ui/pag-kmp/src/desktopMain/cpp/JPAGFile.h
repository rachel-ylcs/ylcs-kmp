#pragma once

#include <native_jni.h>
#include <pag.h>

class JPAGFile {
public:
    explicit JPAGFile(std::shared_ptr<pag::PAGFile> pagLayer) : pagLayer(pagLayer) { }

    std::shared_ptr<pag::PAGFile> get() {
        return pagLayer;
    }
private:
    std::shared_ptr<pag::PAGFile> pagLayer;
};