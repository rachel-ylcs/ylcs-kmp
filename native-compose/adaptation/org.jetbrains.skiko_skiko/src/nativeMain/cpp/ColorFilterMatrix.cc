#include "common.h"
#include "SkColorFilter.h"

SKIKO_EXPORT KBoolean org_jetbrains_skia_ColorFilter__1nGetColorMatrix(KNativePointer pointer, KFloat* matrix) {
    if (pointer == 0 || matrix == nullptr) return false;
    return reinterpret_cast<SkColorFilter*>(pointer)->asAColorMatrix(matrix);
}
