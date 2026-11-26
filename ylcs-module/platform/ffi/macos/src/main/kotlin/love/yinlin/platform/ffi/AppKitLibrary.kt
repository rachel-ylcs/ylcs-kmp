package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.appKit: AppKitLibrary get() = AppKitLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class AppKitLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/AppKit.framework/AppKit") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = AppKitLibrary()

        private val pointer = NativeType.Pointer
        private val boolean = NativeType.Boolean
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float64 = NativeType.Double
        private val rect = MacOSDef.CGRect
    }

    val NSAccessibilityActionDescription by func { declare(pointer, retType = pointer) }
    val NSAccessibilityPostNotification by func { declare(pointer, pointer) }
    val NSAccessibilityPostNotificationWithUserInfo by func { declare(pointer, pointer, pointer) }
    val NSAccessibilityRoleDescription by func { declare(pointer, pointer, retType = pointer) }
    val NSAccessibilityRoleDescriptionForUIElement by func { declare(pointer, retType = pointer) }
    val NSAccessibilityUnignoredAncestor by func { declare(pointer, retType = pointer) }
    val NSAccessibilityUnignoredChildren by func { declare(pointer, retType = pointer) }
    val NSAccessibilityUnignoredChildrenForOnlyChild by func { declare(pointer, retType = pointer) }
    val NSAccessibilityUnignoredDescendant by func { declare(pointer, retType = pointer) }
    val NSApplicationLoad by func { declare(retType = boolean) }
    val NSApplicationMain by func { declare(int32, pointer, retType = int32) }
    val NSAvailableWindowDepths by func { declare(retType = pointer) }
    val NSBeep by func { declare() }
    val NSBestDepth by func { declare(pointer, int64, int64, boolean, pointer, retType = int32) }
    val NSBitsPerPixelFromDepth by func { declare(int32, retType = int64) }
    val NSBitsPerSampleFromDepth by func { declare(int32, retType = int64) }
    val NSColorSpaceFromDepth by func { declare(int32, retType = pointer) }
    val NSDottedFrameRect by func { declare(rect) }
    val NSDrawButton by func { declare(rect, rect) }
    val NSDrawDarkBezel by func { declare(rect, rect) }
    val NSDrawGrayBezel by func { declare(rect, rect) }
    val NSDrawGroove by func { declare(rect, rect) }
    val NSDrawLightBezel by func { declare(rect, rect) }
    val NSDrawTiledRects by func { declare(rect, rect, pointer, pointer, int64, retType = rect) }
    val NSDrawWhiteBezel by func { declare(rect, rect) }
    val NSDrawWindowBackground by func { declare(rect) }
    val NSFrameRect by func { declare(rect) }
    val NSFrameRectWithWidth by func { declare(rect, float64) }
    val NSFrameRectWithWidthUsingOperation by func { declare(rect, float64, int64) }
    val NSNumberOfColorComponents by func { declare(pointer, retType = int64) }
    val NSPlanarFromDepth by func { declare(int32, retType = boolean) }
    val NSRectClip by func { declare(rect) }
    val NSRectClipList by func { declare(pointer, int64) }
    val NSRectFill by func { declare(rect) }
    val NSRectFillList by func { declare(pointer, int64) }
    val NSRectFillListUsingOperation by func { declare(pointer, int64, int64) }
    val NSRectFillListWithColors by func { declare(pointer, pointer, int64) }
    val NSRectFillListWithColorsUsingOperation by func { declare(pointer, pointer, int64, int64) }
    val NSRectFillUsingOperation by func { declare(rect, int64) }
}
