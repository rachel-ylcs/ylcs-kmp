package love.yinlin.platform.ffi

object Posix {
    const val O_CREAT = 64
    const val O_RDWR = 2
    const val LOCK_EX = 2
    const val LOCK_NB = 4
    const val LOCK_UN = 8
}

object PosixType {
    val DIV_T: Layout = NamedStructLayout("div_t") {
        "quot" with NativeType.Int
        "rem" with NativeType.Int
    }

    val LDIV_T: Layout = NamedStructLayout("ldiv_t") {
        "quot" with NativeType.Long
        "rem" with NativeType.Long
    }

    val LLDIV_T: Layout = LDIV_T.withName("lldiv_t")
    val IMAXDIV_T: Layout = LDIV_T.withName("imaxdiv_t")

    val ENTRY: Layout = NamedStructLayout("ENTRY") {
        "key" with NativeType.Pointer
        "data" with NativeType.Pointer
    }

    val IN_ADDR: Layout = NamedStructLayout("in_addr") {
        "s_addr" with NativeType.Int
    }

    val SIGVAL: Layout = NamedUnionLayout("sigval") {
        "sival_int" with NativeType.Int
        "sival_ptr" with NativeType.Pointer
    }

    val DATUM: Layout = NamedStructLayout("datum") {
        "dptr" with NativeType.Pointer
        "dsize" with NativeType.Long
    }
}
