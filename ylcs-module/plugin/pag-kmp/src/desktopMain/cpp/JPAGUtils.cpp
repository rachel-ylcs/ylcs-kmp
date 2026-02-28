#include "JPAGUtils.h"
#include "JPAGShapeLayer.h"
#include "JPAGSolidLayer.h"
#include "JPAGFile.h"
#include "JPAGComposition.h"
#include "JPAGTextLayer.h"
#include "JPAGImageLayer.h"
#include "JPAGLayer.h"

using namespace pag;

LayerInfo JPAGLayerInstance(std::shared_ptr<PAGLayer> pagLayer) {
    if (pagLayer == nullptr) return LayerInfo();
    switch (auto type = pagLayer->layerType()) {
        case LayerType::Shape: return LayerInfo(new JPAGShapeLayer(std::static_pointer_cast<PAGShapeLayer>(pagLayer)), static_cast<jlong>(type));
        case LayerType::Solid: return LayerInfo(new JPAGSolidLayer(std::static_pointer_cast<PAGSolidLayer>(pagLayer)), static_cast<jlong>(type));
        case LayerType::PreCompose: {
            if (std::static_pointer_cast<PAGComposition>(pagLayer)->isPAGFile()) return LayerInfo(new JPAGFile(std::static_pointer_cast<PAGFile>(pagLayer)), true);
            else return LayerInfo(new JPAGComposition(std::static_pointer_cast<PAGComposition>(pagLayer)), static_cast<jlong>(type));
        }
        case LayerType::Text: return LayerInfo(new JPAGTextLayer(std::static_pointer_cast<PAGTextLayer>(pagLayer)), static_cast<jlong>(type));
        case LayerType::Image: return LayerInfo(new JPAGImageLayer(std::static_pointer_cast<PAGImageLayer>(pagLayer)), static_cast<jlong>(type));
        default: return LayerInfo(new JPAGLayer(pagLayer), static_cast<jlong>(LayerType::Unknown));
    }
}

std::shared_ptr<pag::PAGLayer> PAGLayerInstance(LayerInfo info) {
    if (info.empty()) return nullptr;
    if (info.file()) return info.handle<JPAGFile>()->get();
    switch (info.rawType()) {
        case LayerType::Shape: return info.handle<JPAGShapeLayer>()->get();
        case LayerType::Solid: return info.handle<JPAGSolidLayer>()->get();
        case LayerType::PreCompose: return info.handle<JPAGComposition>()->get();
        case LayerType::Text: return info.handle<JPAGTextLayer>()->get();
        case LayerType::Image: return info.handle<JPAGImageLayer>()->get();
        default: return info.handle<JPAGLayer>()->get();
    }
}