#include <native_jni.h>
#include <pag.h>

class JPAGImageLayer {
public:
    explicit JPAGImageLayer(std::shared_ptr<pag::PAGImageLayer> pagLayer) : pagLayer(pagLayer) { }

    std::shared_ptr<pag::PAGImageLayer> get() {
        return pagLayer;
    }
private:
    std::shared_ptr<pag::PAGImageLayer> pagLayer;
};