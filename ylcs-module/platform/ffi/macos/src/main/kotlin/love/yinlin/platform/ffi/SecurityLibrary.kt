package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.security: SecurityLibrary get() = SecurityLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class SecurityLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/Security.framework/Security") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = SecurityLibrary()

        private val pointer = NativeType.Pointer
        private val boolean = NativeType.Boolean
        private val byte = NativeType.Byte
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float64 = NativeType.Double
    }

    val SecCertificateCopyCommonName by func { declare(pointer, pointer, retType = int32) }
    val SecCertificateCopyData by func { declare(pointer, retType = pointer) }
    val SecCertificateCopyEmailAddresses by func { declare(pointer, pointer, retType = int32) }
    val SecCertificateCopyKey by func { declare(pointer, retType = pointer) }
    val SecCertificateCopyNormalizedIssuerSequence by func { declare(pointer, retType = pointer) }
    val SecCertificateCopyNormalizedSubjectSequence by func { declare(pointer, retType = pointer) }
    val SecCertificateCopySerialNumberData by func { declare(pointer, pointer, retType = pointer) }
    val SecCertificateCopySubjectSummary by func { declare(pointer, retType = pointer) }
    val SecCertificateCopyValues by func { declare(pointer, pointer, pointer, retType = pointer) }
    val SecCertificateCreateWithData by func { declare(pointer, pointer, retType = pointer) }
    val SecCertificateGetTypeID by func { declare(retType = int64) }
    val SecCopyErrorMessageString by func { declare(int32, pointer, retType = pointer) }
    val SecIdentityCopyCertificate by func { declare(pointer, pointer, retType = int32) }
    val SecIdentityCopyPrivateKey by func { declare(pointer, pointer, retType = int32) }
    val SecIdentityGetTypeID by func { declare(retType = int64) }
    val SecItemAdd by func { declare(pointer, pointer, retType = int32) }
    val SecItemCopyMatching by func { declare(pointer, pointer, retType = int32) }
    val SecItemDelete by func { declare(pointer, retType = int32) }
    val SecItemUpdate by func { declare(pointer, pointer, retType = int32) }
    val SecKeyCopyAttributes by func { declare(pointer, retType = pointer) }
    val SecKeyCopyExternalRepresentation by func { declare(pointer, pointer, retType = pointer) }
    val SecKeyCopyKeyExchangeResult by func { declare(pointer, pointer, pointer, pointer, pointer, retType = pointer) }
    val SecKeyCopyPublicKey by func { declare(pointer, retType = pointer) }
    val SecKeyCreateDecryptedData by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val SecKeyCreateEncryptedData by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val SecKeyCreateRandomKey by func { declare(pointer, pointer, retType = pointer) }
    val SecKeyCreateSignature by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val SecKeyCreateWithData by func { declare(pointer, pointer, pointer, retType = pointer) }
    val SecKeyGetBlockSize by func { declare(pointer, retType = int64) }
    val SecKeyGetTypeID by func { declare(retType = int64) }
    val SecKeyIsAlgorithmSupported by func { declare(pointer, int64, pointer, retType = byte) }
    val SecKeyVerifySignature by func { declare(pointer, pointer, pointer, pointer, pointer, retType = byte) }
    val SecPolicyCopyProperties by func { declare(pointer, retType = pointer) }
    val SecPolicyCreateBasicX509 by func { declare(retType = pointer) }
    val SecPolicyCreateSSL by func { declare(byte, pointer, retType = pointer) }
    val SecPolicyCreateWithProperties by func { declare(pointer, pointer, retType = pointer) }
    val SecPolicyGetTypeID by func { declare(retType = int64) }
    val SecRandomCopyBytes by func { declare(pointer, int64, pointer, retType = int32) }
    val SecTrustCopyCertificateChain by func { declare(pointer, retType = pointer) }
    val SecTrustCopyKey by func { declare(pointer, retType = pointer) }
    val SecTrustCopyPolicies by func { declare(pointer, pointer, retType = int32) }
    val SecTrustCopyResult by func { declare(pointer, retType = pointer) }
    val SecTrustCreateWithCertificates by func { declare(pointer, pointer, pointer, retType = int32) }
    val SecTrustEvaluateWithError by func { declare(pointer, pointer, retType = boolean) }
    val SecTrustGetNetworkFetchAllowed by func { declare(pointer, pointer, retType = int32) }
    val SecTrustGetTypeID by func { declare(retType = int64) }
    val SecTrustGetVerifyTime by func { declare(pointer, retType = float64) }
    val SecTrustSetAnchorCertificates by func { declare(pointer, pointer, retType = int32) }
    val SecTrustSetAnchorCertificatesOnly by func { declare(pointer, byte, retType = int32) }
    val SecTrustSetNetworkFetchAllowed by func { declare(pointer, byte, retType = int32) }
    val SecTrustSetPolicies by func { declare(pointer, pointer, retType = int32) }
    val SecTrustSetVerifyDate by func { declare(pointer, pointer, retType = int32) }
}
