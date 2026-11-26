package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.coreAudio: CoreAudioLibrary get() = CoreAudioLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class CoreAudioLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/CoreAudio.framework/CoreAudio") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = CoreAudioLibrary()

        private val pointer = NativeType.Pointer
        private val byte = NativeType.Byte
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float64 = NativeType.Double
    }

    val AudioConvertHostTimeToNanos by func { declare(int64, retType = int64) }
    val AudioConvertNanosToHostTime by func { declare(int64, retType = int64) }
    val AudioDeviceCreateIOProcID by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val AudioDeviceDestroyIOProcID by func { declare(int32, pointer, retType = int32) }
    val AudioDeviceGetCurrentTime by func { declare(int32, pointer, retType = int32) }
    val AudioDeviceGetNearestStartTime by func { declare(int32, pointer, int32, retType = int32) }
    val AudioDeviceStart by func { declare(int32, pointer, retType = int32) }
    val AudioDeviceStartAtTime by func { declare(int32, pointer, pointer, int32, retType = int32) }
    val AudioDeviceStop by func { declare(int32, pointer, retType = int32) }
    val AudioDeviceTranslateTime by func { declare(int32, pointer, pointer, retType = int32) }
    val AudioGetCurrentHostTime by func { declare(retType = int64) }
    val AudioGetHostClockFrequency by func { declare(retType = float64) }
    val AudioGetHostClockMinimumTimeDelta by func { declare(retType = int32) }
    val AudioHardwareCreateAggregateDevice by func { declare(pointer, pointer, retType = int32) }
    val AudioHardwareDestroyAggregateDevice by func { declare(int32, retType = int32) }
    val AudioObjectAddPropertyListener by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val AudioObjectGetPropertyData by func { declare(int32, pointer, int32, pointer, pointer, pointer, retType = int32) }
    val AudioObjectGetPropertyDataSize by func { declare(int32, pointer, int32, pointer, pointer, retType = int32) }
    val AudioObjectHasProperty by func { declare(int32, pointer, retType = byte) }
    val AudioObjectIsPropertySettable by func { declare(int32, pointer, pointer, retType = int32) }
    val AudioObjectRemovePropertyListener by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val AudioObjectSetPropertyData by func { declare(int32, pointer, int32, pointer, int32, pointer, retType = int32) }
    val AudioObjectShow by func { declare(int32) }
}
