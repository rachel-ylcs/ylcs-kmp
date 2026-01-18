#include <native_jni.h>
#include "JPAGUtils.h"

class JPAGTextLayer {
public:
    explicit JPAGTextLayer(std::shared_ptr<pag::PAGTextLayer> pagLayer) : pagLayer(pagLayer) { }

    std::shared_ptr<pag::PAGTextLayer> get() {
        return pagLayer;
    }
private:
    std::shared_ptr<pag::PAGTextLayer> pagLayer;
};