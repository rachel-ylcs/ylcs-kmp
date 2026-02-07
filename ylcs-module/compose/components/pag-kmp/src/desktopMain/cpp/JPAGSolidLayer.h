#pragma once

#include "JPAGUtils.h"

class JPAGSolidLayer {
public:
    explicit JPAGSolidLayer(std::shared_ptr<pag::PAGSolidLayer> pagLayer) : pagLayer(pagLayer) { }

    std::shared_ptr<pag::PAGSolidLayer> get() {
        return pagLayer;
    }
private:
    std::shared_ptr<pag::PAGSolidLayer> pagLayer;
};