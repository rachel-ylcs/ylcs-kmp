package org.jetbrains.skiko.window

/** GPU selects D3D12 on Windows, Vulkan on Linux, Metal on macOS; failures are reported. */
enum class NativeWindowBackend { GPU, SOFTWARE }
