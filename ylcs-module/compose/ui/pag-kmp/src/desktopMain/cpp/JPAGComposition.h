#pragma once

#include "JPAGUtils.h"

class JPAGComposition {
public:
    explicit JPAGComposition(std::shared_ptr<pag::PAGComposition> pagLayer) : pagLayer(pagLayer) { }

    std::shared_ptr<pag::PAGComposition> get() {
        return pagLayer;
    }
private:
    std::shared_ptr<pag::PAGComposition> pagLayer;
};