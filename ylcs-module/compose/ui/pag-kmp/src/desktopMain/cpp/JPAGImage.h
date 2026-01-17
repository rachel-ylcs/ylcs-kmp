#include <native_jni.h>
#include <pag.h>

class JPAGImage {
public:
    explicit JPAGImage(std::shared_ptr<pag::PAGImage> pagImage) : pagImage(pagImage) {}

    std::shared_ptr<pag::PAGImage> get() {
        std::lock_guard<std::mutex> autoLock(locker);
        return pagImage;
    }

    void clear() {
        std::lock_guard<std::mutex> autoLock(locker);
        pagImage = nullptr;
    }

private:
    std::shared_ptr<pag::PAGImage> pagImage;
    std::mutex locker;
};