/*
 * Copyright 2026 Rachel contributors.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Toolkit-independent Ganesh/Vulkan WSI. The host supplies the instance extensions
 * and a surface factory; Skiko owns the Vulkan objects and swapchain afterwards.
 */
#if defined(__linux__) && defined(SK_VULKAN)

#define VK_NO_PROTOTYPES
#include <vulkan/vulkan_core.h>
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <limits>
#include <memory>
#include <new>
#include <vector>

#include "types.h"
#define SKIKO_EXPORT extern "C"
#include "VulkanMemoryAllocators.hh"
#include "include/core/SkCanvas.h"
#include "include/core/SkColorSpace.h"
#include "include/core/SkImage.h"
#include "include/core/SkSurface.h"
#include "include/gpu/ganesh/GrBackendSurface.h"
#include "include/gpu/ganesh/GrDirectContext.h"
#include "include/gpu/ganesh/SkSurfaceGanesh.h"
#include "include/gpu/ganesh/vk/GrVkBackendSemaphore.h"
#include "include/gpu/ganesh/vk/GrVkBackendSurface.h"
#include "include/gpu/ganesh/vk/GrVkDirectContext.h"
#include "include/gpu/ganesh/vk/GrVkTypes.h"
#include "include/gpu/vk/VulkanBackendContext.h"
#include "include/gpu/vk/VulkanExtensions.h"

namespace {
thread_local char lastError[1024] = {};
bool fail(const char* operation, VkResult result = VK_SUCCESS) {
    std::snprintf(lastError, sizeof(lastError), "%s (VkResult %d)", operation, int(result));
    return false;
}

// Keep the loader alive while Skia's process-wide Vulkan entry points can reference it.
void* vulkanLoader() {
    static void* loader = dlopen("libvulkan.so.1", RTLD_NOW | RTLD_LOCAL);
    return loader;
}

using SurfaceFactory = void* (*)(void* userData, void* instance);

#define INSTANCE_FUNCTIONS(X) \
    X(vkDestroyInstance) X(vkEnumeratePhysicalDevices) \
    X(vkGetPhysicalDeviceProperties) X(vkGetPhysicalDeviceFeatures) \
    X(vkGetPhysicalDeviceQueueFamilyProperties) X(vkEnumerateDeviceExtensionProperties) \
    X(vkGetPhysicalDeviceSurfaceSupportKHR) X(vkGetPhysicalDeviceSurfaceCapabilitiesKHR) \
    X(vkGetPhysicalDeviceSurfaceFormatsKHR) X(vkGetPhysicalDeviceSurfacePresentModesKHR) \
    X(vkDestroySurfaceKHR) X(vkCreateDevice) X(vkGetDeviceProcAddr)
#define DEVICE_FUNCTIONS(X) \
    X(vkDestroyDevice) X(vkGetDeviceQueue) X(vkDeviceWaitIdle) \
    X(vkCreateSwapchainKHR) X(vkDestroySwapchainKHR) X(vkGetSwapchainImagesKHR) \
    X(vkAcquireNextImageKHR) X(vkQueuePresentKHR) \
    X(vkCreateSemaphore) X(vkDestroySemaphore) X(vkCreateFence) X(vkDestroyFence) \
    X(vkWaitForFences) X(vkResetFences)

class VulkanWindow {
public:
    PFN_vkGetInstanceProcAddr getInstanceProc = nullptr;
#define DECLARE_FUNCTION(name) PFN_##name name = nullptr;
    INSTANCE_FUNCTIONS(DECLARE_FUNCTION)
    DEVICE_FUNCTIONS(DECLARE_FUNCTION)
#undef DECLARE_FUNCTION
    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    VkQueue queue = VK_NULL_HANDLE;
    uint32_t queueFamily = 0;
    VkSurfaceKHR windowSurface = VK_NULL_HANDLE;
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkFence acquireFence = VK_NULL_HANDLE;
    VkPhysicalDeviceFeatures features = {};
    skgpu::VulkanExtensions extensions;
    sk_sp<GrDirectContext> context;
    sk_sp<SkSurface> contentSurface;
    std::vector<sk_sp<SkSurface>> surfaces;
    // A semaphore belongs to a swapchain image, not to a CPU frame. Reacquiring the
    // image guarantees that its previous presentation has finished waiting on it.
    std::vector<VkSemaphore> presentSemaphores;
    uint32_t imageIndex = UINT32_MAX;
    uint32_t width = 0, height = 0;
    int requestedWidth = 0, requestedHeight = 0;
    int preferredBuffers = 0;
    bool vsync = true;
    bool dirty = true;
    bool lost = false;
    char deviceName[VK_MAX_PHYSICAL_DEVICE_NAME_SIZE] = {};

    ~VulkanWindow() {
        if (device && vkDeviceWaitIdle) vkDeviceWaitIdle(device);
        releaseSwapchain();
        context.reset();
        if (acquireFence && vkDestroyFence) vkDestroyFence(device, acquireFence, nullptr);
        if (device && vkDestroyDevice) vkDestroyDevice(device, nullptr);
        if (windowSurface && vkDestroySurfaceKHR) vkDestroySurfaceKHR(instance, windowSurface, nullptr);
        if (instance && vkDestroyInstance) vkDestroyInstance(instance, nullptr);
    }

    bool init(const char* const* instanceExtensions, int extensionCount,
              SurfaceFactory createSurface, void* userData, int priority, int buffers,
              int64_t cacheLimit) {
        if (!createSurface || !instanceExtensions || extensionCount < 1 || extensionCount > 64)
            return fail("Invalid Vulkan surface factory or instance extensions");
        void* loader = vulkanLoader();
        if (!loader) return fail("libvulkan.so.1 is unavailable");
        getInstanceProc = reinterpret_cast<PFN_vkGetInstanceProcAddr>(dlsym(loader, "vkGetInstanceProcAddr"));
        if (!getInstanceProc) return fail("vkGetInstanceProcAddr is unavailable");
        auto createInstance = reinterpret_cast<PFN_vkCreateInstance>(getInstanceProc(nullptr, "vkCreateInstance"));
        if (!createInstance) return fail("vkCreateInstance is unavailable");
        VkApplicationInfo app = {VK_STRUCTURE_TYPE_APPLICATION_INFO};
        app.pApplicationName = "Skiko Native";
        app.apiVersion = VK_API_VERSION_1_1;
        VkInstanceCreateInfo info = {VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
        info.pApplicationInfo = &app;
        info.enabledExtensionCount = uint32_t(extensionCount);
        info.ppEnabledExtensionNames = instanceExtensions;
        VkResult result = createInstance(&info, nullptr, &instance);
        if (result != VK_SUCCESS) return fail("vkCreateInstance requires Vulkan 1.1 and the window extensions", result);
#define LOAD_INSTANCE(name) \
        name = reinterpret_cast<PFN_##name>(getInstanceProc(instance, #name)); \
        if (!name) return fail("Missing Vulkan entry point: " #name);
        INSTANCE_FUNCTIONS(LOAD_INSTANCE)
#undef LOAD_INSTANCE
        windowSurface = reinterpret_cast<VkSurfaceKHR>(createSurface(userData, instance));
        if (!windowSurface) return fail("The window toolkit could not create a Vulkan surface");
        if (!selectDevice(priority)) return false;
        vkGetPhysicalDeviceFeatures(physicalDevice, &features);
        float queuePriority = 1.f;
        VkDeviceQueueCreateInfo queueInfo = {VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
        queueInfo.queueFamilyIndex = queueFamily;
        queueInfo.queueCount = 1;
        queueInfo.pQueuePriorities = &queuePriority;
        const char* deviceExtensions[] = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};
        VkDeviceCreateInfo deviceInfo = {VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
        deviceInfo.queueCreateInfoCount = 1;
        deviceInfo.pQueueCreateInfos = &queueInfo;
        deviceInfo.enabledExtensionCount = 1;
        deviceInfo.ppEnabledExtensionNames = deviceExtensions;
        deviceInfo.pEnabledFeatures = &features;
        result = vkCreateDevice(physicalDevice, &deviceInfo, nullptr, &device);
        if (result != VK_SUCCESS) return fail("vkCreateDevice", result);
#define LOAD_DEVICE(name) \
        name = reinterpret_cast<PFN_##name>(vkGetDeviceProcAddr(device, #name)); \
        if (!name) return fail("Missing Vulkan device entry point: " #name);
        DEVICE_FUNCTIONS(LOAD_DEVICE)
#undef LOAD_DEVICE
        vkGetDeviceQueue(device, queueFamily, 0, &queue);
        VkFenceCreateInfo fenceInfo = {VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};
        result = vkCreateFence(device, &fenceInfo, nullptr, &acquireFence);
        if (result != VK_SUCCESS) return fail("vkCreateFence", result);

        skgpu::VulkanBackendContext backend = {};
        backend.fInstance = instance;
        backend.fPhysicalDevice = physicalDevice;
        backend.fDevice = device;
        backend.fQueue = queue;
        backend.fGraphicsQueueIndex = queueFamily;
        backend.fMaxAPIVersion = VK_API_VERSION_1_1;
        backend.fDeviceFeatures = &features;
        backend.fGetProc = [this](const char* name, VkInstance inst, VkDevice dev) {
            return dev ? vkGetDeviceProcAddr(dev, name) : getInstanceProc(inst, name);
        };
        extensions.init(backend.fGetProc, instance, physicalDevice,
                        uint32_t(extensionCount), instanceExtensions, 1, deviceExtensions);
        backend.fVkExtensions = &extensions;
        backend.fMemoryAllocator = skgpu::VulkanMemoryAllocators::Make(backend, skgpu::ThreadSafe::kYes);
        if (!backend.fMemoryAllocator) return fail("Could not create the Skia Vulkan memory allocator");
        context = GrDirectContexts::MakeVulkan(backend);
        if (!context) return fail("Could not create the Skia Ganesh Vulkan context");
        if (cacheLimit >= 0) context->setResourceCacheLimit(size_t(cacheLimit));
        preferredBuffers = buffers;
        return true;
    }

    bool selectDevice(int priority) {
        uint32_t count = 0;
        VkResult result = vkEnumeratePhysicalDevices(instance, &count, nullptr);
        if (result != VK_SUCCESS || count == 0 || count > 128) return fail("No Vulkan physical devices", result);
        std::vector<VkPhysicalDevice> devices(count);
        result = vkEnumeratePhysicalDevices(instance, &count, devices.data());
        if (result != VK_SUCCESS) return fail("vkEnumeratePhysicalDevices", result);
        int bestScore = -1;
        for (auto candidate : devices) {
            VkPhysicalDeviceProperties props;
            vkGetPhysicalDeviceProperties(candidate, &props);
            if (props.apiVersion < VK_API_VERSION_1_1) continue;
            uint32_t extensionCount = 0;
            if (vkEnumerateDeviceExtensionProperties(candidate, nullptr, &extensionCount, nullptr) != VK_SUCCESS || extensionCount > 4096) continue;
            std::vector<VkExtensionProperties> available(extensionCount);
            if (vkEnumerateDeviceExtensionProperties(candidate, nullptr, &extensionCount, available.data()) != VK_SUCCESS) continue;
            if (std::none_of(available.begin(), available.end(), [](const auto& entry) {
                return std::strcmp(entry.extensionName, VK_KHR_SWAPCHAIN_EXTENSION_NAME) == 0;
            })) continue;
            uint32_t familyCount = 0;
            vkGetPhysicalDeviceQueueFamilyProperties(candidate, &familyCount, nullptr);
            if (familyCount == 0 || familyCount > 4096) continue;
            std::vector<VkQueueFamilyProperties> families(familyCount);
            vkGetPhysicalDeviceQueueFamilyProperties(candidate, &familyCount, families.data());
            for (uint32_t family = 0; family < familyCount; ++family) {
                VkBool32 supportsPresent = false;
                if (vkGetPhysicalDeviceSurfaceSupportKHR(candidate, family, windowSurface, &supportsPresent) != VK_SUCCESS) continue;
                if (!supportsPresent || !families[family].queueCount || !(families[family].queueFlags & VK_QUEUE_GRAPHICS_BIT)) continue;
                // Preference, never a requirement. Virtual Vulkan devices remain useful in CI.
                int score = 1;
                if (props.deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) score += priority == 1 ? 10 : 100;
                if (props.deviceType == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) score += priority == 1 ? 100 : 10;
                if (score > bestScore) {
                    physicalDevice = candidate;
                    queueFamily = family;
                    bestScore = score;
                    std::memcpy(deviceName, props.deviceName, sizeof(deviceName));
                    deviceName[sizeof(deviceName) - 1] = '\0';
                }
                break;
            }
        }
        return physicalDevice ? true : fail("No Vulkan 1.1 graphics queue can present to this window");
    }

    void releaseSwapchain() {
        contentSurface.reset();
        surfaces.clear();
        for (auto semaphore : presentSemaphores) {
            if (semaphore && vkDestroySemaphore) vkDestroySemaphore(device, semaphore, nullptr);
        }
        presentSemaphores.clear();
        if (swapchain && vkDestroySwapchainKHR) vkDestroySwapchainKHR(device, swapchain, nullptr);
        swapchain = VK_NULL_HANDLE;
        imageIndex = UINT32_MAX;
        width = height = 0;
    }

    bool recreate(int newWidth, int newHeight, bool waitForVsync) {
        VkResult result = vkDeviceWaitIdle(device);
        if (result != VK_SUCCESS) { lost = true; return fail("vkDeviceWaitIdle before resize", result); }
        releaseSwapchain();
        VkSurfaceCapabilitiesKHR caps;
        result = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, windowSurface, &caps);
        if (result != VK_SUCCESS) return fail("vkGetPhysicalDeviceSurfaceCapabilitiesKHR", result);
        VkExtent2D extent = caps.currentExtent;
        if (extent.width == UINT32_MAX) {
            extent.width = std::clamp(uint32_t(newWidth), caps.minImageExtent.width, caps.maxImageExtent.width);
            extent.height = std::clamp(uint32_t(newHeight), caps.minImageExtent.height, caps.maxImageExtent.height);
        }
        if (!extent.width || !extent.height || extent.width > 16384 || extent.height > 16384 ||
            uint64_t(extent.width) * extent.height > 67108864) return fail("Invalid Vulkan drawable extent");
        uint32_t formatCount = 0;
        result = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, windowSurface, &formatCount, nullptr);
        if (result != VK_SUCCESS || formatCount == 0 || formatCount > 1024) return fail("No Vulkan surface formats", result);
        std::vector<VkSurfaceFormatKHR> formats(formatCount);
        result = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, windowSurface, &formatCount, formats.data());
        if (result != VK_SUCCESS) return fail("vkGetPhysicalDeviceSurfaceFormatsKHR", result);
        VkSurfaceFormatKHR format = {VK_FORMAT_UNDEFINED, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR};
        for (const auto& available : formats) {
            if (available.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR &&
                (available.format == VK_FORMAT_B8G8R8A8_UNORM || available.format == VK_FORMAT_R8G8B8A8_UNORM)) {
                format = available;
                if (format.format == VK_FORMAT_B8G8R8A8_UNORM) break;
            }
        }
        if (formats.size() == 1 && formats[0].format == VK_FORMAT_UNDEFINED) format.format = VK_FORMAT_B8G8R8A8_UNORM;
        if (format.format == VK_FORMAT_UNDEFINED) return fail("Window has no supported UNORM Vulkan surface format");
        uint32_t modeCount = 0;
        result = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, windowSurface, &modeCount, nullptr);
        if (result != VK_SUCCESS || modeCount == 0 || modeCount > 1024) return fail("No Vulkan present modes", result);
        std::vector<VkPresentModeKHR> modes(modeCount);
        result = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, windowSurface, &modeCount, modes.data());
        if (result != VK_SUCCESS) return fail("vkGetPhysicalDeviceSurfacePresentModesKHR", result);
        VkPresentModeKHR mode = VK_PRESENT_MODE_FIFO_KHR;
        if (!waitForVsync) {
            if (std::find(modes.begin(), modes.end(), VK_PRESENT_MODE_MAILBOX_KHR) != modes.end()) mode = VK_PRESENT_MODE_MAILBOX_KHR;
            else if (std::find(modes.begin(), modes.end(), VK_PRESENT_MODE_IMMEDIATE_KHR) != modes.end()) mode = VK_PRESENT_MODE_IMMEDIATE_KHR;
        }
        uint32_t imageCount = std::max(caps.minImageCount, uint32_t(preferredBuffers > 0 ? preferredBuffers : 3));
        if (caps.maxImageCount) imageCount = std::min(imageCount, caps.maxImageCount);
        VkImageUsageFlags usage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        usage |= caps.supportedUsageFlags & (VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
        if (!(caps.supportedUsageFlags & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)) return fail("Vulkan surface cannot be a color attachment");
        VkCompositeAlphaFlagBitsKHR alpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
        if (!(caps.supportedCompositeAlpha & alpha)) {
            alpha = VkCompositeAlphaFlagBitsKHR(caps.supportedCompositeAlpha & (~caps.supportedCompositeAlpha + 1));
        }
        VkSwapchainCreateInfoKHR info = {VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR};
        info.surface = windowSurface;
        info.minImageCount = imageCount;
        info.imageFormat = format.format;
        info.imageColorSpace = format.colorSpace;
        info.imageExtent = extent;
        info.imageArrayLayers = 1;
        info.imageUsage = usage;
        info.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
        info.preTransform = caps.currentTransform;
        info.compositeAlpha = alpha;
        info.presentMode = mode;
        info.clipped = VK_TRUE;
        result = vkCreateSwapchainKHR(device, &info, nullptr, &swapchain);
        if (result != VK_SUCCESS) return fail("vkCreateSwapchainKHR", result);
        result = vkGetSwapchainImagesKHR(device, swapchain, &imageCount, nullptr);
        if (result != VK_SUCCESS || imageCount == 0 || imageCount > 64) return fail("Invalid Vulkan swapchain image count", result);
        std::vector<VkImage> images(imageCount);
        result = vkGetSwapchainImagesKHR(device, swapchain, &imageCount, images.data());
        if (result != VK_SUCCESS) return fail("vkGetSwapchainImagesKHR", result);
        presentSemaphores.resize(imageCount, VK_NULL_HANDLE);
        for (uint32_t i = 0; i < imageCount; ++i) {
            VkSemaphoreCreateInfo semaphoreInfo = {VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
            result = vkCreateSemaphore(device, &semaphoreInfo, nullptr, &presentSemaphores[i]);
            if (result != VK_SUCCESS) return fail("vkCreateSemaphore", result);
            GrVkImageInfo imageInfo;
            imageInfo.fImage = images[i];
            imageInfo.fImageTiling = VK_IMAGE_TILING_OPTIMAL;
            imageInfo.fImageLayout = VK_IMAGE_LAYOUT_UNDEFINED;
            imageInfo.fFormat = format.format;
            imageInfo.fImageUsageFlags = usage;
            imageInfo.fSampleCount = 1;
            imageInfo.fLevelCount = 1;
            imageInfo.fCurrentQueueFamily = queueFamily;
            auto target = GrBackendRenderTargets::MakeVk(int(extent.width), int(extent.height), imageInfo);
            auto surface = SkSurfaces::WrapBackendRenderTarget(context.get(), target, kTopLeft_GrSurfaceOrigin,
                format.format == VK_FORMAT_B8G8R8A8_UNORM ? kBGRA_8888_SkColorType : kRGBA_8888_SkColorType,
                SkColorSpace::MakeSRGB(), nullptr);
            if (!surface) return fail("Skia cannot wrap a Vulkan swapchain image");
            surfaces.push_back(std::move(surface));
        }
        width = extent.width;
        height = extent.height;
        // Keep the scene in a GPU surface owned by Skiko. Snapshots can then read it
        // safely even after the window system has taken ownership of a presented image.
        contentSurface = SkSurfaces::RenderTarget(context.get(), skgpu::Budgeted::kYes,
            SkImageInfo::MakeN32Premul(int(width), int(height), SkColorSpace::MakeSRGB()));
        if (!contentSurface) return fail("Could not allocate the Vulkan scene surface");
        requestedWidth = newWidth;
        requestedHeight = newHeight;
        vsync = waitForVsync;
        dirty = false;
        return true;
    }

    SkSurface* acquire(int newWidth, int newHeight, bool waitForVsync) {
        if (lost || !context || context->abandoned()) { fail("Vulkan context was lost"); return nullptr; }
        if (newWidth <= 0 || newHeight <= 0 || newWidth > 16384 || newHeight > 16384 ||
            int64_t(newWidth) * newHeight > 67108864) { fail("Invalid drawable size"); return nullptr; }
        if (imageIndex != UINT32_MAX) { fail("Previous Vulkan frame has not been presented"); return nullptr; }
        for (int attempt = 0; attempt < 2; ++attempt) {
            if ((dirty || requestedWidth != newWidth || requestedHeight != newHeight || vsync != waitForVsync) &&
                !recreate(newWidth, newHeight, waitForVsync)) return nullptr;
            // A CPU fence removes the need for a separate image-available semaphore in Skia.
            uint32_t acquired = UINT32_MAX;
            VkResult result = vkAcquireNextImageKHR(device, swapchain, 5000000000ULL, VK_NULL_HANDLE, acquireFence, &acquired);
            if (result == VK_ERROR_OUT_OF_DATE_KHR) { dirty = true; continue; }
            if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                lost = result == VK_ERROR_DEVICE_LOST;
                fail("vkAcquireNextImageKHR", result); return nullptr;
            }
            dirty = result == VK_SUBOPTIMAL_KHR;
            result = vkWaitForFences(device, 1, &acquireFence, VK_TRUE, 5000000000ULL);
            if (result != VK_SUCCESS) { lost = true; fail("vkWaitForFences acquiring image", result); return nullptr; }
            result = vkResetFences(device, 1, &acquireFence);
            if (result != VK_SUCCESS || acquired >= surfaces.size()) { lost = true; fail("Invalid Vulkan acquired image", result); return nullptr; }
            imageIndex = acquired;
            return SkSafeRef(contentSurface.get());
        }
        fail("Vulkan swapchain remained out of date after recreation");
        return nullptr;
    }

    bool present() {
        if (imageIndex >= surfaces.size()) return fail("No acquired Vulkan frame");
        auto frame = contentSurface->makeImageSnapshot();
        if (!frame) return fail("Could not snapshot the Vulkan scene for presentation");
        auto* canvas = surfaces[imageIndex]->getCanvas();
        canvas->clear(SK_ColorBLACK);
        canvas->drawImage(frame, 0, 0);
        auto semaphore = GrBackendSemaphores::MakeVk(presentSemaphores[imageIndex]);
        GrFlushInfo flushInfo;
        flushInfo.fNumSemaphores = 1;
        flushInfo.fSignalSemaphores = &semaphore;
        auto flushed = context->flush(surfaces[imageIndex].get(), SkSurfaces::BackendSurfaceAccess::kPresent, flushInfo);
        if (!flushed.fSuccess || flushed.fSubmitted != GrSemaphoresSubmitted::kYes || !context->submit()) {
            lost = true;
            return fail("Skia could not submit the Vulkan frame and signal presentation");
        }
        VkPresentInfoKHR info = {VK_STRUCTURE_TYPE_PRESENT_INFO_KHR};
        info.waitSemaphoreCount = 1;
        info.pWaitSemaphores = &presentSemaphores[imageIndex];
        info.swapchainCount = 1;
        info.pSwapchains = &swapchain;
        info.pImageIndices = &imageIndex;
        VkResult result = vkQueuePresentKHR(queue, &info);
        imageIndex = UINT32_MAX;
        if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) { dirty = true; return true; }
        if (result != VK_SUCCESS) { lost = result == VK_ERROR_DEVICE_LOST; return fail("vkQueuePresentKHR", result); }
        return true;
    }
};
} // namespace

SKIKO_EXPORT KNativePointer RachelVulkanWindow_Create(KNativePointer names, KInt count,
    KNativePointer factory, KNativePointer userData, KInt priority, KInt buffers, KLong cacheLimit) {
    lastError[0] = '\0';
    auto window = std::unique_ptr<VulkanWindow>(new (std::nothrow) VulkanWindow());
    if (!window) { fail("Cannot allocate Vulkan window state"); return nullptr; }
    if (!window->init(static_cast<const char* const*>(names), count,
        reinterpret_cast<SurfaceFactory>(factory), userData, priority, buffers, cacheLimit)) return nullptr;
    return window.release();
}
SKIKO_EXPORT void RachelVulkanWindow_Destroy(KNativePointer ptr) { delete static_cast<VulkanWindow*>(ptr); }
SKIKO_EXPORT KNativePointer RachelVulkanWindow_Acquire(KNativePointer ptr, KInt width, KInt height, KBoolean vsync) {
    return static_cast<VulkanWindow*>(ptr)->acquire(width, height, vsync);
}
SKIKO_EXPORT KBoolean RachelVulkanWindow_Present(KNativePointer ptr) { return static_cast<VulkanWindow*>(ptr)->present(); }
SKIKO_EXPORT KBoolean RachelVulkanWindow_IsLost(KNativePointer ptr) {
    auto* window = static_cast<VulkanWindow*>(ptr);
    return window->lost || window->context->abandoned();
}
SKIKO_EXPORT KNativePointer RachelVulkanWindow_DeviceName(KNativePointer ptr) { return static_cast<VulkanWindow*>(ptr)->deviceName; }
SKIKO_EXPORT KNativePointer RachelVulkanWindow_LastError() { return lastError; }

#endif // __linux__ && SK_VULKAN
