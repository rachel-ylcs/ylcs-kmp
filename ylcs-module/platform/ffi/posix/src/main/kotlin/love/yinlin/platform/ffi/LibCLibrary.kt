package love.yinlin.platform.ffi

import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Suppress("UnusedReceiverParameter")
inline val FFI.libc: LibCLibrary get() = LibCLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class LibCLibrary private constructor() : NativeLibrary() {
    @PublishedApi
    internal companion object {
        private val darwin get() = platform == Platform.MacOS
        private val pointer = NativeType.Pointer
        private val boolean = NativeType.Boolean
        private val byte = NativeType.Byte
        private val int16 = NativeType.Short
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float32 = NativeType.Float
        private val float64 = NativeType.Double

        @PublishedApi
        internal val Instance = LibCLibrary()
    }

    val DNSServiceAddRecord by func { declare(pointer, pointer, int32, int16, int16, pointer, int32, retType = int32) }
    val DNSServiceBrowse by func { declare(pointer, int32, int32, pointer, pointer, pointer, pointer, retType = int32) }
    val DNSServiceConstructFullName by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val DNSServiceCreateConnection by func { declare(pointer, retType = int32) }
    val DNSServiceEnumerateDomains by func { declare(pointer, int32, int32, pointer, pointer, retType = int32) }
    val DNSServiceGetAddrInfo by func { declare(pointer, int32, int32, int32, pointer, pointer, pointer, retType = int32) }
    val DNSServiceGetProperty by func { declare(pointer, pointer, pointer, retType = int32) }
    val DNSServiceNATPortMappingCreate by func { declare(pointer, int32, int32, int32, int16, int16, int32, pointer, pointer, retType = int32) }
    val DNSServiceProcessResult by func { declare(pointer, retType = int32) }
    val DNSServiceQueryRecord by func { declare(pointer, int32, int32, pointer, int16, int16, pointer, pointer, retType = int32) }
    val DNSServiceReconfirmRecord by func { declare(int32, int32, pointer, int16, int16, int16, pointer, retType = int32) }
    val DNSServiceRefDeallocate by func { declare(pointer) }
    val DNSServiceRefSockFD by func { declare(pointer, retType = int32) }
    val DNSServiceRegister by func { declare(pointer, int32, int32, pointer, pointer, pointer, pointer, int16, int16, pointer, pointer, pointer, retType = int32) }
    val DNSServiceRegisterRecord by func { declare(pointer, pointer, int32, int32, pointer, int16, int16, int16, pointer, int32, pointer, pointer, retType = int32) }
    val DNSServiceRemoveRecord by func { declare(pointer, pointer, int32, retType = int32) }
    val DNSServiceResolve by func { declare(pointer, int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val DNSServiceSleepKeepalive by func { declare(pointer, int32, int32, int32, pointer, pointer, retType = int32) }
    val DNSServiceUpdateRecord by func { declare(pointer, pointer, int32, int16, pointer, int32, retType = int32) }
    val PeerConnectionRelease by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val TXTRecordContainsKey by func { declare(int16, pointer, pointer, retType = int32) }
    val TXTRecordCreate by func { declare(pointer, int16, pointer) }
    val TXTRecordDeallocate by func { declare(pointer) }
    val TXTRecordGetBytesPtr by func { declare(pointer, retType = pointer) }
    val TXTRecordGetCount by func { declare(int16, pointer, retType = int16) }
    val TXTRecordGetItemAtIndex by func { declare(int16, pointer, int16, int16, pointer, pointer, pointer, retType = int32) }
    val TXTRecordGetLength by func { declare(pointer, retType = int16) }
    val TXTRecordGetValuePtr by func { declare(int16, pointer, pointer, pointer, retType = pointer) }
    val TXTRecordRemoveValue by func { declare(pointer, pointer, retType = int32) }
    val TXTRecordSetValue by func { declare(pointer, pointer, byte, pointer, retType = int32) }
    val _Exit by func { declare(int32) }
    val _exit by func { declare(int32) }
    val a64l by func { declare(pointer, retType = int64) }
    val abort by func { declare() }
    val abs by func { declare(int32, retType = int32) }
    val accept by func { declare(int32, pointer, pointer, retType = int32) }
    val accept4 by func { declare(int32, pointer, pointer, int32, retType = int32) }
    val access by func { declare(pointer, int32, retType = int32) }
    val accessx_np by func { declare(pointer, int64, pointer, int32, retType = int32) }
    val acct by func { declare(pointer, retType = int32) }
    val acl_add_flag_np by func { declare(pointer, int32, retType = int32) }
    val acl_add_perm by func { declare(pointer, int32, retType = int32) }
    val acl_calc_mask by func { declare(pointer, retType = int32) }
    val acl_clear_flags_np by func { declare(pointer, retType = int32) }
    val acl_clear_perms by func { declare(pointer, retType = int32) }
    val acl_copy_entry by func { declare(pointer, pointer, retType = int32) }
    val acl_copy_ext by func { declare(pointer, pointer, int64, retType = int64) }
    val acl_copy_ext_native by func { declare(pointer, pointer, int64, retType = int64) }
    val acl_copy_int by func { declare(pointer, retType = pointer) }
    val acl_copy_int_native by func { declare(pointer, retType = pointer) }
    val acl_create_entry by func { declare(pointer, pointer, retType = int32) }
    val acl_create_entry_np by func { declare(pointer, pointer, int32, retType = int32) }
    val acl_delete_def_file by func { declare(pointer, retType = int32) }
    val acl_delete_entry by func { declare(pointer, pointer, retType = int32) }
    val acl_delete_flag_np by func { declare(pointer, int32, retType = int32) }
    val acl_delete_perm by func { declare(pointer, int32, retType = int32) }
    val acl_dup by func { declare(pointer, retType = pointer) }
    val acl_free by func { declare(pointer, retType = int32) }
    val acl_from_text by func { declare(pointer, retType = pointer) }
    val acl_get_entry by func { declare(pointer, int32, pointer, retType = int32) }
    val acl_get_fd by func { declare(int32, retType = pointer) }
    val acl_get_fd_np by func { declare(int32, int32, retType = pointer) }
    val acl_get_file by func { declare(pointer, int32, retType = pointer) }
    val acl_get_flag_np by func { declare(pointer, int32, retType = int32) }
    val acl_get_flagset_np by func { declare(pointer, pointer, retType = int32) }
    val acl_get_link_np by func { declare(pointer, int32, retType = pointer) }
    val acl_get_perm_np by func { declare(pointer, int32, retType = int32) }
    val acl_get_permset by func { declare(pointer, pointer, retType = int32) }
    val acl_get_permset_mask_np by func { declare(pointer, pointer, retType = int32) }
    val acl_get_qualifier by func { declare(pointer, retType = pointer) }
    val acl_get_tag_type by func { declare(pointer, pointer, retType = int32) }
    val acl_init by func { declare(int32, retType = pointer) }
    val acl_maximal_permset_mask_np by func { declare(pointer, retType = int32) }
    val acl_set_fd by func { declare(int32, pointer, retType = int32) }
    val acl_set_fd_np by func { declare(int32, pointer, int32, retType = int32) }
    val acl_set_file by func { declare(pointer, int32, pointer, retType = int32) }
    val acl_set_flagset_np by func { declare(pointer, pointer, retType = int32) }
    val acl_set_link_np by func { declare(pointer, int32, pointer, retType = int32) }
    val acl_set_permset by func { declare(pointer, pointer, retType = int32) }
    val acl_set_permset_mask_np by func { declare(pointer, int64, retType = int32) }
    val acl_set_qualifier by func { declare(pointer, pointer, retType = int32) }
    val acl_set_tag_type by func { declare(pointer, int32, retType = int32) }
    val acl_size by func { declare(pointer, retType = int64) }
    val acl_to_text by func { declare(pointer, pointer, retType = pointer) }
    val acl_valid by func { declare(pointer, retType = int32) }
    val acl_valid_fd_np by func { declare(int32, int32, pointer, retType = int32) }
    val acl_valid_file_np by func { declare(pointer, int32, pointer, retType = int32) }
    val acos by func { declare(float64, retType = float64) }
    val acosf by func { declare(float32, retType = float32) }
    val acosh by func { declare(float64, retType = float64) }
    val acoshf by func { declare(float32, retType = float32) }
    val act_get_state by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val act_set_state by func { declare(int32, int32, pointer, int32, retType = int32) }
    val addmntent by func { declare(pointer, pointer, retType = int32) }
    val addr2ascii by func { declare(int32, pointer, int32, pointer, retType = pointer) }
    val adjtime by func { declare(pointer, pointer, retType = int32) }
    val adjtimex by func { declare(pointer, retType = int32) }
    val aio_cancel by func { declare(int32, pointer, retType = int32) }
    val aio_error by func { declare(pointer, retType = int32) }
    val aio_fsync by func { declare(int32, pointer, retType = int32) }
    val aio_read by func { declare(pointer, retType = int32) }
    val aio_return by func { declare(pointer, retType = int64) }
    val aio_suspend by func { declare(pointer, int32, pointer, retType = int32) }
    val aio_write by func { declare(pointer, retType = int32) }
    val alarm by func { declare(int32, retType = int32) }
    val aligned_alloc by func { declare(int64, int64, retType = pointer) }
    val alphasort by func { declare(pointer, pointer, retType = int32) }
    val arc4random by func { declare(retType = int32) }
    val arc4random_addrandom by func { declare(pointer, int32) }
    val arc4random_buf by func { declare(pointer, int64) }
    val arc4random_stir by func { declare() }
    val arc4random_uniform by func { declare(int32, retType = int32) }
    val argp_help by func { declare(pointer, pointer, int32, pointer) }
    val argp_parse by func { declare(pointer, int32, pointer, int32, pointer, pointer, retType = int32) }
    val argp_state_help by func { declare(pointer, pointer, int32) }
    val argp_usage by func { declare(pointer) }
    val argz_add by func { declare(pointer, pointer, pointer, retType = int32) }
    val argz_add_sep by func { declare(pointer, pointer, pointer, int32, retType = int32) }
    val argz_append by func { declare(pointer, pointer, pointer, int64, retType = int32) }
    val argz_count by func { declare(pointer, int64, retType = int64) }
    val argz_create by func { declare(pointer, pointer, pointer, retType = int32) }
    val argz_create_sep by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val argz_delete by func { declare(pointer, pointer, pointer) }
    val argz_extract by func { declare(pointer, int64, pointer) }
    val argz_insert by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val argz_next by func { declare(pointer, int64, pointer, retType = pointer) }
    val argz_replace by func { declare(pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val argz_stringify by func { declare(pointer, int64, int32) }
    val ascii2addr by func { declare(int32, pointer, pointer, retType = int32) }
    val asctime by func { declare(pointer, retType = pointer) }
    val asctime_r by func { declare(pointer, pointer, retType = pointer) }
    val asin by func { declare(float64, retType = float64) }
    val asinf by func { declare(float32, retType = float32) }
    val asinh by func { declare(float64, retType = float64) }
    val asinhf by func { declare(float32, retType = float32) }
    val asl_add_log_file by func { declare(pointer, int32, retType = int32) }
    val asl_add_output_file by func { declare(pointer, int32, pointer, pointer, int32, int32, retType = int32) }
    val asl_append by func { declare(pointer, pointer) }
    val asl_close by func { declare(pointer) }
    val asl_close_auxiliary_file by func { declare(int32, retType = int32) }
    val asl_count by func { declare(pointer, retType = int64) }
    val asl_create_auxiliary_file by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val asl_fetch_key_val_op by func { declare(pointer, int32, pointer, pointer, pointer, retType = int32) }
    val asl_format by func { declare(pointer, pointer, pointer, int32, retType = pointer) }
    val asl_free by func { declare(pointer) }
    val asl_get by func { declare(pointer, pointer, retType = pointer) }
    val asl_get_index by func { declare(pointer, int64, retType = pointer) }
    val asl_get_type by func { declare(pointer, retType = int32) }
    val asl_key by func { declare(pointer, int32, retType = pointer) }
    val asl_log_auxiliary_location by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val asl_log_descriptor by func { declare(pointer, pointer, int32, int32, int32, retType = int32) }
    val asl_match by func { declare(pointer, pointer, pointer, int64, int64, int32, int32, retType = pointer) }
    val asl_new by func { declare(int32, retType = pointer) }
    val asl_next by func { declare(pointer, retType = pointer) }
    val asl_open by func { declare(pointer, pointer, int32, retType = pointer) }
    val asl_open_from_file by func { declare(int32, pointer, pointer, retType = pointer) }
    val asl_open_path by func { declare(pointer, int32, retType = pointer) }
    val asl_prepend by func { declare(pointer, pointer) }
    val asl_prev by func { declare(pointer, retType = pointer) }
    val asl_release by func { declare(pointer) }
    val asl_remove_log_file by func { declare(pointer, int32, retType = int32) }
    val asl_reset_iteration by func { declare(pointer, int64) }
    val asl_retain by func { declare(pointer, retType = pointer) }
    val asl_search by func { declare(pointer, pointer, retType = pointer) }
    val asl_send by func { declare(pointer, pointer, retType = int32) }
    val asl_set by func { declare(pointer, pointer, pointer, retType = int32) }
    val asl_set_filter by func { declare(pointer, int32, retType = int32) }
    val asl_set_output_file_filter by func { declare(pointer, int32, int32, retType = int32) }
    val asl_set_query by func { declare(pointer, pointer, pointer, int32, retType = int32) }
    val asl_unset by func { declare(pointer, pointer, retType = int32) }
    val aslresponse_free by func { declare(pointer) }
    val aslresponse_next by func { declare(pointer, retType = pointer) }
    val atan by func { declare(float64, retType = float64) }
    val atan2 by func { declare(float64, float64, retType = float64) }
    val atan2f by func { declare(float32, float32, retType = float32) }
    val atanf by func { declare(float32, retType = float32) }
    val atanh by func { declare(float64, retType = float64) }
    val atanhf by func { declare(float32, retType = float32) }
    val atexit by func { declare(pointer, retType = int32) }
    val atof by func { declare(pointer, retType = float64) }
    val atof_l by func { declare(pointer, pointer, retType = float64) }
    val atoi by func { declare(pointer, retType = int32) }
    val atoi_l by func { declare(pointer, pointer, retType = int32) }
    val atol by func { declare(pointer, retType = int64) }
    val atol_l by func { declare(pointer, pointer, retType = int64) }
    val atoll by func { declare(pointer, retType = int64) }
    val atoll_l by func { declare(pointer, pointer, retType = int64) }
    val atomic_flag_clear by func { declare(pointer) }
    val atomic_flag_clear_explicit by func { declare(pointer, int32) }
    val atomic_flag_test_and_set by func { declare(pointer, retType = boolean) }
    val atomic_flag_test_and_set_explicit by func { declare(pointer, int32, retType = boolean) }
    val atomic_signal_fence by func { declare(int32) }
    val atomic_thread_fence by func { declare(int32) }
    val audit by func { declare(pointer, int32, retType = int32) }
    val audit_session_join by func { declare(int32, retType = int32) }
    val audit_session_port by func { declare(int32, pointer, retType = int32) }
    val audit_session_self by func { declare(retType = int32) }
    val auditctl by func { declare(pointer, retType = int32) }
    val auditon by func { declare(int32, pointer, int32, retType = int32) }
    val backtrace by func { declare(pointer, int32, retType = int32) }
    val backtrace_from_fp by func { declare(pointer, pointer, int32, retType = int32) }
    val backtrace_image_offsets by func { declare(pointer, pointer, int32) }
    val backtrace_symbols by func { declare(pointer, int32, retType = pointer) }
    val backtrace_symbols_fd by func { declare(pointer, int32, int32) }
    val basename_r by func { declare(pointer, pointer, retType = pointer) }
    val bcmp by func { declare(pointer, pointer, int64, retType = int32) }
    val bcopy by func { declare(pointer, pointer, int64) }
    val bind by func { declare(int32, pointer, int32, retType = int32) }
    val bind_textdomain_codeset by func { declare(pointer, pointer, retType = pointer) }
    val bindresvport by func { declare(int32, pointer, retType = int32) }
    val bindresvport_sa by func { declare(int32, pointer, retType = int32) }
    val bindtextdomain by func { declare(pointer, pointer, retType = pointer) }
    val brk by func { if (darwin) declare(pointer, retType = pointer) else declare(pointer, retType = int32) }
    val bsd_signal by func { declare(int32, pointer, retType = pointer) }
    val bsearch by func { declare(pointer, pointer, int64, int64, pointer, retType = pointer) }
    val btowc by func { declare(int32, retType = int32) }
    val btowc_l by func { declare(int32, pointer, retType = int32) }
    val bzero by func { declare(pointer, int64) }
    val c16rtomb by func { declare(pointer, int16, pointer, retType = int64) }
    val c32rtomb by func { declare(pointer, int32, pointer, retType = int64) }
    val cache_create by func { declare(pointer, pointer, pointer, retType = int32) }
    val cache_destroy by func { declare(pointer, retType = int32) }
    val cache_get_and_retain by func { declare(pointer, pointer, pointer, retType = int32) }
    val cache_hash_byte_string by func { declare(pointer, int64, retType = int64) }
    val cache_key_hash_cb_cstring by func { declare(pointer, pointer, retType = int64) }
    val cache_key_hash_cb_integer by func { declare(pointer, pointer, retType = int64) }
    val cache_key_is_equal_cb_cstring by func { declare(pointer, pointer, pointer, retType = boolean) }
    val cache_key_is_equal_cb_integer by func { declare(pointer, pointer, pointer, retType = boolean) }
    val cache_release_cb_free by func { declare(pointer, pointer) }
    val cache_release_value by func { declare(pointer, pointer, retType = int32) }
    val cache_remove by func { declare(pointer, pointer, retType = int32) }
    val cache_remove_all by func { declare(pointer, retType = int32) }
    val cache_set_and_retain by func { declare(pointer, pointer, pointer, int64, retType = int32) }
    val cache_value_make_nonpurgeable_cb by func { declare(pointer, pointer, retType = boolean) }
    val cache_value_make_purgeable_cb by func { declare(pointer, pointer) }
    val call_once by func { declare(pointer, pointer) }
    val calloc by func { declare(int64, int64, retType = pointer) }
    val catclose by func { declare(pointer, retType = int32) }
    val catgets by func { declare(pointer, int32, int32, pointer, retType = pointer) }
    val catopen by func { declare(pointer, int32, retType = pointer) }
    val cbrt by func { declare(float64, retType = float64) }
    val cbrtf by func { declare(float32, retType = float32) }
    val ceil by func { declare(float64, retType = float64) }
    val ceilf by func { declare(float32, retType = float32) }
    val cfgetispeed by func { if (darwin) declare(pointer, retType = int64) else declare(pointer, retType = int32) }
    val cfgetospeed by func { if (darwin) declare(pointer, retType = int64) else declare(pointer, retType = int32) }
    val cfmakeraw by func { declare(pointer) }
    val cfsetispeed by func { if (darwin) declare(pointer, int64, retType = int32) else declare(pointer, int32, retType = int32) }
    val cfsetospeed by func { if (darwin) declare(pointer, int64, retType = int32) else declare(pointer, int32, retType = int32) }
    val cfsetspeed by func { if (darwin) declare(pointer, int64, retType = int32) else declare(pointer, int32, retType = int32) }
    val cgetcap by func { declare(pointer, pointer, int32, retType = pointer) }
    val cgetclose by func { declare(retType = int32) }
    val cgetent by func { declare(pointer, pointer, pointer, retType = int32) }
    val cgetfirst by func { declare(pointer, pointer, retType = int32) }
    val cgetmatch by func { declare(pointer, pointer, retType = int32) }
    val cgetnext by func { declare(pointer, pointer, retType = int32) }
    val cgetnum by func { declare(pointer, pointer, pointer, retType = int32) }
    val cgetset by func { declare(pointer, retType = int32) }
    val cgetstr by func { declare(pointer, pointer, pointer, retType = int32) }
    val cgetustr by func { declare(pointer, pointer, pointer, retType = int32) }
    val chdir by func { declare(pointer, retType = int32) }
    val chmod by func { if (darwin) declare(pointer, int16, retType = int32) else declare(pointer, int32, retType = int32) }
    val chmodx_np by func { declare(pointer, pointer, retType = int32) }
    val chown by func { declare(pointer, int32, int32, retType = int32) }
    val chroot by func { declare(pointer, retType = int32) }
    val clearenv by func { declare(retType = int32) }
    val clearerr by func { declare(pointer) }
    val clock by func { declare(retType = int64) }
    val clock_getcpuclockid by func { declare(int32, pointer, retType = int32) }
    val clock_getres by func { declare(int32, pointer, retType = int32) }
    val clock_gettime by func { declare(int32, pointer, retType = int32) }
    val clock_gettime_nsec_np by func { declare(int32, retType = int64) }
    val clock_nanosleep by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val clock_set_attributes by func { declare(int32, int32, pointer, int32, retType = int32) }
    val clock_settime by func { declare(int32, pointer, retType = int32) }
    val clock_sleep_trap by func { declare(int32, int32, int32, int32, pointer, retType = int32) }
    val close by func { declare(int32, retType = int32) }
    val close_range by func { declare(int32, int32, int32, retType = int32) }
    val closedir by func { declare(pointer, retType = int32) }
    val closelog by func { declare() }
    val cnd_broadcast by func { declare(pointer, retType = int32) }
    val cnd_destroy by func { declare(pointer) }
    val cnd_init by func { declare(pointer, retType = int32) }
    val cnd_signal by func { declare(pointer, retType = int32) }
    val cnd_timedwait by func { declare(pointer, pointer, pointer, retType = int32) }
    val cnd_wait by func { declare(pointer, pointer, retType = int32) }
    val confstr by func { declare(int32, pointer, int64, retType = int64) }
    val connect by func { declare(int32, pointer, int32, retType = int32) }
    val connectx by func { declare(int32, pointer, int32, int32, pointer, int32, pointer, pointer, retType = int32) }
    val copy_file_range by func { declare(int32, pointer, int32, pointer, int64, int32, retType = int64) }
    val copy_printf_domain by func { declare(pointer, retType = pointer) }
    val copyfile by func { declare(pointer, pointer, pointer, int32, retType = int32) }
    val copyfile_state_alloc by func { declare(retType = pointer) }
    val copyfile_state_free by func { declare(pointer, retType = int32) }
    val copyfile_state_get by func { declare(pointer, int32, pointer, retType = int32) }
    val copyfile_state_set by func { declare(pointer, int32, pointer, retType = int32) }
    val copysign by func { declare(float64, float64, retType = float64) }
    val copysignf by func { declare(float32, float32, retType = float32) }
    val cos by func { declare(float64, retType = float64) }
    val cosf by func { declare(float32, retType = float32) }
    val cosh by func { declare(float64, retType = float64) }
    val coshf by func { declare(float32, retType = float32) }
    val creat by func { if (darwin) declare(pointer, int16, retType = int32) else declare(pointer, int32, retType = int32) }
    val crypt by func { declare(pointer, pointer, retType = pointer) }
    val ctermid by func { declare(pointer, retType = pointer) }
    val ctermid_r by func { declare(pointer, retType = pointer) }
    val ctime by func { declare(pointer, retType = pointer) }
    val ctime_r by func { declare(pointer, pointer, retType = pointer) }
    val cuserid by func { declare(pointer, retType = pointer) }
    val daemon by func { declare(int32, int32, retType = int32) }
    val dbm_clearerr by func { declare(pointer, retType = int32) }
    val dbm_close by func { declare(pointer) }
    val dbm_delete by func { declare(pointer, PosixType.DATUM, retType = int32) }
    val dbm_dirfno by func { declare(pointer, retType = int32) }
    val dbm_error by func { declare(pointer, retType = int32) }
    val dbm_fetch by func { declare(pointer, PosixType.DATUM, retType = PosixType.DATUM) }
    val dbm_firstkey by func { declare(pointer, retType = PosixType.DATUM) }
    val dbm_nextkey by func { declare(pointer, retType = PosixType.DATUM) }
    val dbm_open by func { declare(pointer, int32, int16, retType = pointer) }
    val dbm_store by func { declare(pointer, PosixType.DATUM, PosixType.DATUM, int32, retType = int32) }
    val dcgettext by func { declare(pointer, pointer, int32, retType = pointer) }
    val dcngettext by func { declare(pointer, pointer, pointer, int64, int32, retType = pointer) }
    val debug_control_port_for_pid by func { declare(int32, int32, pointer, retType = int32) }
    val devname by func { declare(int32, int16, retType = pointer) }
    val devname_r by func { declare(int32, int16, pointer, int32, retType = pointer) }
    val dgettext by func { declare(pointer, pointer, retType = pointer) }
    val difftime by func { declare(int64, int64, retType = float64) }
    val digittoint by func { declare(int32, retType = int32) }
    val digittoint_l by func { declare(int32, pointer, retType = int32) }
    val dirfd by func { declare(pointer, retType = int32) }
    val dirname by func { declare(pointer, retType = pointer) }
    val dirname_r by func { declare(pointer, pointer, retType = pointer) }
    val disconnectx by func { declare(int32, int32, int32, retType = int32) }
    val dispatch_get_specific by func { declare(pointer, retType = pointer) }
    val dispatch_main by func { declare() }
    val dispatch_once_f by func { declare(pointer, pointer, pointer) }
    val dispatch_time by func { declare(int64, int64, retType = int64) }
    val dispatch_walltime by func { declare(pointer, int64, retType = int64) }
    val div by func { declare(int32, int32, retType = PosixType.DIV_T) }
    val dladdr by func { declare(pointer, pointer, retType = int32) }
    val dlclose by func { declare(pointer, retType = int32) }
    val dlerror by func { declare(retType = pointer) }
    val dlopen by func { declare(pointer, int32, retType = pointer) }
    val dlopen_preflight by func { declare(pointer, retType = boolean) }
    val dlsym by func { declare(pointer, pointer, retType = pointer) }
    val dngettext by func { declare(pointer, pointer, pointer, int64, retType = pointer) }
    val drand48 by func { declare(retType = float64) }
    val drand48_r by func { declare(pointer, pointer, retType = int32) }
    val drem by func { declare(float64, float64, retType = float64) }
    val dremf by func { declare(float32, float32, retType = float32) }
    val dup by func { declare(int32, retType = int32) }
    val dup2 by func { declare(int32, int32, retType = int32) }
    val dup3 by func { declare(int32, int32, int32, retType = int32) }
    val duplocale by func { declare(pointer, retType = pointer) }
    val dysize by func { declare(int32, retType = int32) }
    val ecvt by func { declare(float64, int32, pointer, pointer, retType = pointer) }
    val ecvt_r by func { declare(float64, int32, pointer, pointer, pointer, int64, retType = int32) }
    val encrypt by func { declare(pointer, int32) }
    val endaliasent by func { declare() }
    val endfsent by func { declare() }
    val endgrent by func { declare() }
    val endhostent by func { declare() }
    val endmntent by func { declare(pointer, retType = int32) }
    val endnetent by func { declare() }
    val endnetgrent by func { declare() }
    val endprotoent by func { declare() }
    val endpwent by func { declare() }
    val endrpcent by func { declare() }
    val endservent by func { declare() }
    val endsgent by func { declare() }
    val endttyent by func { declare(retType = int32) }
    val endusershell by func { declare() }
    val endutent by func { declare() }
    val endutxent by func { declare() }
    val endutxent_wtmp by func { declare() }
    val envz_add by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val envz_entry by func { declare(pointer, int64, pointer, retType = pointer) }
    val envz_get by func { declare(pointer, int64, pointer, retType = pointer) }
    val envz_merge by func { declare(pointer, pointer, pointer, int64, int32, retType = int32) }
    val envz_remove by func { declare(pointer, pointer, pointer) }
    val envz_strip by func { declare(pointer, pointer) }
    val epoll_create by func { declare(int32, retType = int32) }
    val epoll_create1 by func { declare(int32, retType = int32) }
    val epoll_ctl by func { declare(int32, int32, int32, pointer, retType = int32) }
    val epoll_pwait by func { declare(int32, pointer, int32, int32, pointer, retType = int32) }
    val epoll_wait by func { declare(int32, pointer, int32, int32, retType = int32) }
    val erand48 by func { declare(pointer, retType = float64) }
    val erand48_r by func { declare(pointer, pointer, pointer, retType = int32) }
    val erf by func { declare(float64, retType = float64) }
    val erfc by func { declare(float64, retType = float64) }
    val erfcf by func { declare(float32, retType = float32) }
    val erff by func { declare(float32, retType = float32) }
    val err_set_exit by func { declare(pointer) }
    val err_set_file by func { declare(pointer) }
    val etap_trace_thread by func { declare(int32, int32, retType = int32) }
    val ether_aton by func { declare(pointer, retType = pointer) }
    val ether_aton_r by func { declare(pointer, pointer, retType = pointer) }
    val ether_hostton by func { declare(pointer, pointer, retType = int32) }
    val ether_line by func { declare(pointer, pointer, pointer, retType = int32) }
    val ether_ntoa by func { declare(pointer, retType = pointer) }
    val ether_ntoa_r by func { declare(pointer, pointer, retType = pointer) }
    val ether_ntohost by func { declare(pointer, pointer, retType = int32) }
    val eventfd by func { declare(int32, int32, retType = int32) }
    val eventfd_read by func { declare(int32, pointer, retType = int32) }
    val eventfd_write by func { declare(int32, int64, retType = int32) }
    val exchangedata by func { declare(pointer, pointer, int32, retType = int32) }
    val execv by func { declare(pointer, pointer, retType = int32) }
    val execvP by func { declare(pointer, pointer, pointer, retType = int32) }
    val execve by func { declare(pointer, pointer, pointer, retType = int32) }
    val execvp by func { declare(pointer, pointer, retType = int32) }
    val exit by func { declare(int32) }
    val exp by func { declare(float64, retType = float64) }
    val exp2 by func { declare(float64, retType = float64) }
    val exp2f by func { declare(float32, retType = float32) }
    val expf by func { declare(float32, retType = float32) }
    val expm1 by func { declare(float64, retType = float64) }
    val expm1f by func { declare(float32, retType = float32) }
    val fabs by func { declare(float64, retType = float64) }
    val fabsf by func { declare(float32, retType = float32) }
    val faccessat by func { declare(int32, pointer, int32, int32, retType = int32) }
    val fanotify_init by func { declare(int32, int32, retType = int32) }
    val fanotify_mark by func { declare(int32, int32, int64, int32, pointer, retType = int32) }
    val fchdir by func { declare(int32, retType = int32) }
    val fchmod by func { if (darwin) declare(int32, int16, retType = int32) else declare(int32, int32, retType = int32) }
    val fchmodat by func { if (darwin) declare(int32, pointer, int16, int32, retType = int32) else declare(int32, pointer, int32, int32, retType = int32) }
    val fchmodx_np by func { declare(int32, pointer, retType = int32) }
    val fchown by func { declare(int32, int32, int32, retType = int32) }
    val fchownat by func { declare(int32, pointer, int32, int32, int32, retType = int32) }
    val fclose by func { declare(pointer, retType = int32) }
    val fcopyfile by func { declare(int32, int32, pointer, int32, retType = int32) }
    val fcvt by func { declare(float64, int32, pointer, pointer, retType = pointer) }
    val fcvt_r by func { declare(float64, int32, pointer, pointer, pointer, int64, retType = int32) }
    val fdatasync by func { declare(int32, retType = int32) }
    val fdim by func { declare(float64, float64, retType = float64) }
    val fdimf by func { declare(float32, float32, retType = float32) }
    val fdopen by func { declare(int32, pointer, retType = pointer) }
    val fdopendir by func { declare(int32, retType = pointer) }
    val feclearexcept by func { declare(int32, retType = int32) }
    val fegetenv by func { declare(pointer, retType = int32) }
    val fegetexceptflag by func { declare(pointer, int32, retType = int32) }
    val fegetround by func { declare(retType = int32) }
    val feholdexcept by func { declare(pointer, retType = int32) }
    val feof by func { declare(pointer, retType = int32) }
    val feraiseexcept by func { declare(int32, retType = int32) }
    val ferror by func { declare(pointer, retType = int32) }
    val fesetenv by func { declare(pointer, retType = int32) }
    val fesetexceptflag by func { declare(pointer, int32, retType = int32) }
    val fesetround by func { declare(int32, retType = int32) }
    val fetestexcept by func { declare(int32, retType = int32) }
    val feupdateenv by func { declare(pointer, retType = int32) }
    val fexecve by func { declare(int32, pointer, pointer, retType = int32) }
    val fflagstostr by func { declare(int64, retType = pointer) }
    val fflush by func { declare(pointer, retType = int32) }
    val fflush_unlocked by func { declare(pointer, retType = int32) }
    val ffs by func { declare(int32, retType = int32) }
    val ffsctl by func { declare(int32, int64, pointer, int32, retType = int32) }
    val ffsl by func { declare(int64, retType = int32) }
    val ffsll by func { declare(int64, retType = int32) }
    val fgetattrlist by func { declare(int32, pointer, pointer, int64, int32, retType = int32) }
    val fgetc by func { declare(pointer, retType = int32) }
    val fgetc_unlocked by func { declare(pointer, retType = int32) }
    val fgetln by func { declare(pointer, pointer, retType = pointer) }
    val fgetpos by func { declare(pointer, pointer, retType = int32) }
    val fgets by func { declare(pointer, int32, pointer, retType = pointer) }
    val fgetsgent by func { declare(pointer, retType = pointer) }
    val fgetsgent_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val fgetwc by func { declare(pointer, retType = int32) }
    val fgetwc_l by func { declare(pointer, pointer, retType = int32) }
    val fgetwln by func { declare(pointer, pointer, retType = pointer) }
    val fgetwln_l by func { declare(pointer, pointer, pointer, retType = pointer) }
    val fgetws by func { declare(pointer, int32, pointer, retType = pointer) }
    val fgetws_l by func { declare(pointer, int32, pointer, pointer, retType = pointer) }
    val fileno by func { declare(pointer, retType = int32) }
    val filesec_dup by func { declare(pointer, retType = pointer) }
    val filesec_free by func { declare(pointer) }
    val filesec_get_property by func { declare(pointer, int32, pointer, retType = int32) }
    val filesec_init by func { declare(retType = pointer) }
    val filesec_query_property by func { declare(pointer, int32, pointer, retType = int32) }
    val filesec_set_property by func { declare(pointer, int32, pointer, retType = int32) }
    val filesec_unset_property by func { declare(pointer, int32, retType = int32) }
    val finite by func { declare(float64, retType = int32) }
    val finitef by func { declare(float32, retType = int32) }
    val flock by func { declare(int32, int32, retType = int32) }
    val flockfile by func { declare(pointer) }
    val floor by func { declare(float64, retType = float64) }
    val floorf by func { declare(float32, retType = float32) }
    val fls by func { declare(int32, retType = int32) }
    val flsl by func { declare(int64, retType = int32) }
    val flsll by func { declare(int64, retType = int32) }
    val fma by func { declare(float64, float64, float64, retType = float64) }
    val fmaf by func { declare(float32, float32, float32, retType = float32) }
    val fmax by func { declare(float64, float64, retType = float64) }
    val fmaxf by func { declare(float32, float32, retType = float32) }
    val fmemopen by func { declare(pointer, int64, pointer, retType = pointer) }
    val fmin by func { declare(float64, float64, retType = float64) }
    val fminf by func { declare(float32, float32, retType = float32) }
    val fmod by func { declare(float64, float64, retType = float64) }
    val fmodf by func { declare(float32, float32, retType = float32) }
    val fmtcheck by func { declare(pointer, pointer, retType = pointer) }
    val fmtmsg by func { declare(int64, pointer, int32, pointer, pointer, pointer, retType = int32) }
    val fnmatch by func { declare(pointer, pointer, int32, retType = int32) }
    val fopen by func { declare(pointer, pointer, retType = pointer) }
    val forkpty by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val fparseln by func { declare(pointer, pointer, pointer, pointer, int32, retType = pointer) }
    val fpathconf by func { declare(int32, int32, retType = int64) }
    val fpurge by func { declare(pointer, retType = int32) }
    val fputc by func { declare(int32, pointer, retType = int32) }
    val fputc_unlocked by func { declare(int32, pointer, retType = int32) }
    val fputs by func { declare(pointer, pointer, retType = int32) }
    val fputwc by func { declare(int32, pointer, retType = int32) }
    val fputwc_l by func { declare(int32, pointer, pointer, retType = int32) }
    val fputws by func { declare(pointer, pointer, retType = int32) }
    val fputws_l by func { declare(pointer, pointer, pointer, retType = int32) }
    val fread by func { declare(pointer, int64, int64, pointer, retType = int64) }
    val fread_unlocked by func { declare(pointer, int64, int64, pointer, retType = int64) }
    val free by func { declare(pointer) }
    val free_printf_comp by func { declare(pointer) }
    val free_printf_domain by func { declare(pointer) }
    val freeaddrinfo by func { declare(pointer) }
    val freehostent by func { declare(pointer) }
    val freeifaddrs by func { declare(pointer) }
    val freeifmaddrs by func { declare(pointer) }
    val freelocale by func { if (darwin) declare(pointer, retType = int32) else declare(pointer) }
    val freopen by func { declare(pointer, pointer, pointer, retType = pointer) }
    val frexp by func { declare(float64, pointer, retType = float64) }
    val frexpf by func { declare(float32, pointer, retType = float32) }
    val fsctl by func { declare(pointer, int64, pointer, int32, retType = int32) }
    val fseek by func { declare(pointer, int64, int32, retType = int32) }
    val fseeko by func { declare(pointer, int64, int32, retType = int32) }
    val fsetattrlist by func { declare(int32, pointer, pointer, int64, int32, retType = int32) }
    val fsetpos by func { declare(pointer, pointer, retType = int32) }
    val fstat by func { declare(int32, pointer, retType = int32) }
    val fstatat by func { declare(int32, pointer, pointer, int32, retType = int32) }
    val fstatvfs by func { declare(int32, pointer, retType = int32) }
    val fstatx_np by func { declare(int32, pointer, pointer, retType = int32) }
    val fsync by func { declare(int32, retType = int32) }
    val fsync_volume_np by func { declare(int32, int32, retType = int32) }
    val ftell by func { declare(pointer, retType = int64) }
    val ftello by func { declare(pointer, retType = int64) }
    val ftok by func { declare(pointer, int32, retType = int32) }
    val ftruncate by func { declare(int32, int64, retType = int32) }
    val ftrylockfile by func { declare(pointer, retType = int32) }
    val fts_children by func { declare(pointer, int32, retType = pointer) }
    val fts_close by func { declare(pointer, retType = int32) }
    val fts_open by func { declare(pointer, int32, pointer, retType = pointer) }
    val fts_read by func { declare(pointer, retType = pointer) }
    val fts_set by func { declare(pointer, pointer, int32, retType = int32) }
    val ftw by func { declare(pointer, pointer, int32, retType = int32) }
    val funlockfile by func { declare(pointer) }
    val funopen by func { declare(pointer, pointer, pointer, pointer, pointer, retType = pointer) }
    val futimens by func { declare(int32, pointer, retType = int32) }
    val futimes by func { declare(int32, pointer, retType = int32) }
    val fwide by func { declare(pointer, int32, retType = int32) }
    val fwrite by func { declare(pointer, int64, int64, pointer, retType = int64) }
    val fwrite_unlocked by func { declare(pointer, int64, int64, pointer, retType = int64) }
    val gai_strerror by func { declare(int32, retType = pointer) }
    val gamma by func { declare(float64, retType = float64) }
    val gammaf by func { declare(float32, retType = float32) }
    val gcvt by func { declare(float64, int32, pointer, retType = pointer) }
    val get_avphys_pages by func { declare(retType = int64) }
    val get_nprocs by func { declare(retType = int32) }
    val get_nprocs_conf by func { declare(retType = int32) }
    val get_phys_pages by func { declare(retType = int64) }
    val getaddrinfo by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val getaliasbyname by func { declare(pointer, retType = pointer) }
    val getaliasbyname_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getaliasent by func { declare(retType = pointer) }
    val getaliasent_r by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val getattrlist by func { declare(pointer, pointer, pointer, int64, int32, retType = int32) }
    val getattrlistat by func { declare(int32, pointer, pointer, pointer, int64, int64, retType = int32) }
    val getattrlistbulk by func { declare(int32, pointer, pointer, int64, int64, retType = int32) }
    val getaudit by func { declare(pointer, retType = int32) }
    val getaudit_addr by func { declare(pointer, int32, retType = int32) }
    val getauid by func { declare(pointer, retType = int32) }
    val getauxval by func { declare(int64, retType = int64) }
    val getbsize by func { declare(pointer, pointer, retType = pointer) }
    val getc by func { declare(pointer, retType = int32) }
    val getc_unlocked by func { declare(pointer, retType = int32) }
    val getchar by func { declare(retType = int32) }
    val getchar_unlocked by func { declare(retType = int32) }
    val getcwd by func { declare(pointer, int64, retType = pointer) }
    val getdate by func { declare(pointer, retType = pointer) }
    val getdelim by func { declare(pointer, pointer, int32, pointer, retType = int64) }
    val getdirentriesattr by func { declare(int32, pointer, pointer, int64, pointer, pointer, pointer, int32, retType = int32) }
    val getdomainname by func { if (darwin) declare(pointer, int32, retType = int32) else declare(pointer, int64, retType = int32) }
    val getdtablesize by func { declare(retType = int32) }
    val getegid by func { declare(retType = int32) }
    val getentropy by func { declare(pointer, int64, retType = int32) }
    val getenv by func { declare(pointer, retType = pointer) }
    val geteuid by func { declare(retType = int32) }
    val getfsent by func { declare(retType = pointer) }
    val getfsfile by func { declare(pointer, retType = pointer) }
    val getfsspec by func { declare(pointer, retType = pointer) }
    val getgid by func { declare(retType = int32) }
    val getgrent by func { declare(retType = pointer) }
    val getgrgid by func { declare(int32, retType = pointer) }
    val getgrgid_r by func { declare(int32, pointer, pointer, int64, pointer, retType = int32) }
    val getgrnam by func { declare(pointer, retType = pointer) }
    val getgrnam_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getgrouplist by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val getgroups by func { declare(int32, pointer, retType = int32) }
    val getgruuid by func { declare(pointer, retType = pointer) }
    val getgruuid_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val gethostbyaddr by func { declare(pointer, int32, int32, retType = pointer) }
    val gethostbyaddr_r by func { declare(pointer, int32, int32, pointer, pointer, int64, pointer, pointer, retType = int32) }
    val gethostbyname by func { declare(pointer, retType = pointer) }
    val gethostbyname2 by func { declare(pointer, int32, retType = pointer) }
    val gethostbyname2_r by func { declare(pointer, int32, pointer, pointer, int64, pointer, pointer, retType = int32) }
    val gethostbyname_r by func { declare(pointer, pointer, pointer, int64, pointer, pointer, retType = int32) }
    val gethostent by func { declare(retType = pointer) }
    val gethostent_r by func { declare(pointer, pointer, int64, pointer, pointer, retType = int32) }
    val gethostid by func { declare(retType = int64) }
    val gethostname by func { declare(pointer, int64, retType = int32) }
    val gethostuuid by func { declare(pointer, pointer, retType = int32) }
    val getifaddrs by func { declare(pointer, retType = int32) }
    val getifmaddrs by func { declare(pointer, retType = int32) }
    val getiopolicy_np by func { declare(int32, int32, retType = int32) }
    val getipnodebyaddr by func { declare(pointer, int64, int32, pointer, retType = pointer) }
    val getipnodebyname by func { declare(pointer, int32, int32, pointer, retType = pointer) }
    val getitimer by func { declare(int32, pointer, retType = int32) }
    val getlastlogx by func { declare(int32, pointer, retType = pointer) }
    val getlastlogxbyname by func { declare(pointer, pointer, retType = pointer) }
    val getline by func { declare(pointer, pointer, pointer, retType = int64) }
    val getloadavg by func { declare(pointer, int32, retType = int32) }
    val getlogin by func { declare(retType = pointer) }
    val getlogin_r by func { declare(pointer, int64, retType = int32) }
    val getmntent by func { declare(pointer, retType = pointer) }
    val getmntent_r by func { declare(pointer, pointer, pointer, int32, retType = pointer) }
    val getmode by func { declare(pointer, int16, retType = int16) }
    val getnameinfo by func { declare(pointer, int32, pointer, int32, pointer, int32, int32, retType = int32) }
    val getnetbyaddr by func { declare(int32, int32, retType = pointer) }
    val getnetbyaddr_r by func { declare(int32, int32, pointer, pointer, int64, pointer, pointer, retType = int32) }
    val getnetbyname by func { declare(pointer, retType = pointer) }
    val getnetbyname_r by func { declare(pointer, pointer, pointer, int64, pointer, pointer, retType = int32) }
    val getnetent by func { declare(retType = pointer) }
    val getnetent_r by func { declare(pointer, pointer, int64, pointer, pointer, retType = int32) }
    val getnetgrent by func { declare(pointer, pointer, pointer, retType = int32) }
    val getnetgrent_r by func { declare(pointer, pointer, pointer, pointer, int64, retType = int32) }
    val getopt by func { declare(int32, pointer, pointer, retType = int32) }
    val getopt_long by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val getopt_long_only by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val getpagesize by func { declare(retType = int32) }
    val getpass by func { declare(pointer, retType = pointer) }
    val getpeereid by func { declare(int32, pointer, pointer, retType = int32) }
    val getpeername by func { declare(int32, pointer, pointer, retType = int32) }
    val getpgid by func { declare(int32, retType = int32) }
    val getpgrp by func { declare(retType = int32) }
    val getpid by func { declare(retType = int32) }
    val getppid by func { declare(retType = int32) }
    val getpriority by func { declare(int32, int32, retType = int32) }
    val getprogname by func { declare(retType = pointer) }
    val getprotobyname by func { declare(pointer, retType = pointer) }
    val getprotobyname_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getprotobynumber by func { declare(int32, retType = pointer) }
    val getprotobynumber_r by func { declare(int32, pointer, pointer, int64, pointer, retType = int32) }
    val getprotoent by func { declare(retType = pointer) }
    val getprotoent_r by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val getpwent by func { declare(retType = pointer) }
    val getpwent_r by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val getpwnam by func { declare(pointer, retType = pointer) }
    val getpwnam_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getpwuid by func { declare(int32, retType = pointer) }
    val getpwuid_r by func { declare(int32, pointer, pointer, int64, pointer, retType = int32) }
    val getpwuuid by func { declare(pointer, retType = pointer) }
    val getpwuuid_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getrandom by func { declare(pointer, int64, int32, retType = int64) }
    val getresgid by func { declare(pointer, pointer, pointer, retType = int32) }
    val getresuid by func { declare(pointer, pointer, pointer, retType = int32) }
    val getrlimit by func { declare(int32, pointer, retType = int32) }
    val getrpcbyname by func { declare(pointer, retType = pointer) }
    val getrpcbyname_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getrpcbynumber by func { declare(int32, retType = pointer) }
    val getrpcbynumber_r by func { declare(int32, pointer, pointer, int64, pointer, retType = int32) }
    val getrpcent by func { declare(retType = pointer) }
    val getrpcent_r by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val getrusage by func { declare(int32, pointer, retType = int32) }
    val getservbyname by func { declare(pointer, pointer, retType = pointer) }
    val getservbyname_r by func { declare(pointer, pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getservbyport by func { declare(int32, pointer, retType = pointer) }
    val getservbyport_r by func { declare(int32, pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getservent by func { declare(retType = pointer) }
    val getservent_r by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val getsgent by func { declare(retType = pointer) }
    val getsgent_r by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val getsgnam by func { declare(pointer, retType = pointer) }
    val getsgnam_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val getsgroups_np by func { declare(pointer, pointer, retType = int32) }
    val getsid by func { declare(int32, retType = int32) }
    val getsockname by func { declare(int32, pointer, pointer, retType = int32) }
    val getsockopt by func { declare(int32, int32, int32, pointer, pointer, retType = int32) }
    val getsubopt by func { declare(pointer, pointer, pointer, retType = int32) }
    val gettext by func { declare(pointer, retType = pointer) }
    val gettid by func { declare(retType = int32) }
    val gettimeofday by func { declare(pointer, pointer, retType = int32) }
    val getttyent by func { declare(retType = pointer) }
    val getttynam by func { declare(pointer, retType = pointer) }
    val getuid by func { declare(retType = int32) }
    val getusershell by func { declare(retType = pointer) }
    val getutent by func { declare(retType = pointer) }
    val getutent_r by func { declare(pointer, pointer, retType = int32) }
    val getutid by func { declare(pointer, retType = pointer) }
    val getutid_r by func { declare(pointer, pointer, pointer, retType = int32) }
    val getutline by func { declare(pointer, retType = pointer) }
    val getutline_r by func { declare(pointer, pointer, pointer, retType = int32) }
    val getutxent by func { declare(retType = pointer) }
    val getutxent_wtmp by func { declare(retType = pointer) }
    val getutxid by func { declare(pointer, retType = pointer) }
    val getutxline by func { declare(pointer, retType = pointer) }
    val getw by func { declare(pointer, retType = int32) }
    val getwc by func { declare(pointer, retType = int32) }
    val getwc_l by func { declare(pointer, pointer, retType = int32) }
    val getwchar by func { declare(retType = int32) }
    val getwchar_l by func { declare(pointer, retType = int32) }
    val getwd by func { declare(pointer, retType = pointer) }
    val getwgroups_np by func { declare(pointer, pointer, retType = int32) }
    val glob by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val globfree by func { declare(pointer) }
    val gmtime by func { declare(pointer, retType = pointer) }
    val gmtime_r by func { declare(pointer, pointer, retType = pointer) }
    val gnu_dev_major by func { declare(int64, retType = int32) }
    val gnu_dev_makedev by func { declare(int32, int32, retType = int64) }
    val gnu_dev_minor by func { declare(int64, retType = int32) }
    val grantpt by func { declare(int32, retType = int32) }
    val group_from_gid by func { declare(int32, int32, retType = pointer) }
    val gtty by func { declare(int32, pointer, retType = int32) }
    val hasmntopt by func { declare(pointer, pointer, retType = pointer) }
    val hcreate by func { declare(int64, retType = int32) }
    val hdestroy by func { declare() }
    val heapsort by func { declare(pointer, int64, int64, pointer, retType = int32) }
    val herror by func { declare(pointer) }
    val host_check_multiuser_mode by func { declare(int32, pointer, retType = int32) }
    val host_create_mach_voucher by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val host_create_mach_voucher_trap by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val host_default_memory_manager by func { declare(int32, pointer, int32, retType = int32) }
    val host_get_UNDServer by func { declare(int32, pointer, retType = int32) }
    val host_get_atm_diagnostic_flag by func { declare(int32, pointer, retType = int32) }
    val host_get_boot_info by func { declare(int32, pointer, retType = int32) }
    val host_get_clock_control by func { declare(int32, int32, pointer, retType = int32) }
    val host_get_clock_service by func { declare(int32, int32, pointer, retType = int32) }
    val host_get_exception_ports by func { declare(int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val host_get_multiuser_config_flags by func { declare(int32, pointer, retType = int32) }
    val host_get_special_port by func { declare(int32, int32, int32, pointer, retType = int32) }
    val host_info by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val host_kernel_version by func { declare(int32, pointer, retType = int32) }
    val host_lockgroup_info by func { declare(int32, pointer, pointer, retType = int32) }
    val host_page_size by func { declare(int32, pointer, retType = int32) }
    val host_priv_statistics by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val host_processor_info by func { declare(int32, int32, pointer, pointer, pointer, retType = int32) }
    val host_processor_set_priv by func { declare(int32, int32, pointer, retType = int32) }
    val host_processor_sets by func { declare(int32, pointer, pointer, retType = int32) }
    val host_processors by func { declare(int32, pointer, pointer, retType = int32) }
    val host_reboot by func { declare(int32, int32, retType = int32) }
    val host_register_mach_voucher_attr_manager by func { declare(int32, int32, int64, pointer, pointer, retType = int32) }
    val host_register_well_known_mach_voucher_attr_manager by func { declare(int32, int32, int64, int32, pointer, retType = int32) }
    val host_request_notification by func { declare(int32, int32, int32, retType = int32) }
    val host_set_UNDServer by func { declare(int32, int32, retType = int32) }
    val host_set_atm_diagnostic_flag by func { declare(int32, int32, retType = int32) }
    val host_set_exception_ports by func { declare(int32, int32, int32, int32, int32, retType = int32) }
    val host_set_multiuser_config_flags by func { declare(int32, int32, retType = int32) }
    val host_set_special_port by func { declare(int32, int32, int32, retType = int32) }
    val host_statistics by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val host_statistics64 by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val host_swap_exception_ports by func { declare(int32, int32, int32, int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val host_virtual_physical_table_info by func { declare(int32, pointer, pointer, retType = int32) }
    val hsearch by func { declare(PosixType.ENTRY, int32, retType = pointer) }
    val hstrerror by func { declare(int32, retType = pointer) }
    val htonl by func { declare(int32, retType = int32) }
    val htons by func { declare(int16, retType = int16) }
    val hypot by func { declare(float64, float64, retType = float64) }
    val hypotf by func { declare(float32, float32, retType = float32) }
    val iconv by func { declare(pointer, pointer, pointer, pointer, pointer, retType = int64) }
    val iconv_close by func { declare(pointer, retType = int32) }
    val iconv_open by func { declare(pointer, pointer, retType = pointer) }
    val if_freenameindex by func { declare(pointer) }
    val if_indextoname by func { declare(int32, pointer, retType = pointer) }
    val if_nameindex by func { declare(retType = pointer) }
    val if_nametoindex by func { declare(pointer, retType = int32) }
    val ilogb by func { declare(float64, retType = int32) }
    val ilogbf by func { declare(float32, retType = int32) }
    val imaxabs by func { declare(int64, retType = int64) }
    val imaxdiv by func { declare(int64, int64, retType = PosixType.IMAXDIV_T) }
    val index by func { declare(pointer, int32, retType = pointer) }
    val inet6_rthdr_add by func { declare(pointer, pointer, int32, retType = int32) }
    val inet6_rthdr_getaddr by func { declare(pointer, int32, retType = pointer) }
    val inet6_rthdr_getflags by func { declare(pointer, int32, retType = int32) }
    val inet6_rthdr_init by func { declare(pointer, int32, retType = pointer) }
    val inet6_rthdr_lasthop by func { declare(pointer, int32, retType = int32) }
    val inet6_rthdr_segments by func { declare(pointer, retType = int32) }
    val inet6_rthdr_space by func { declare(int32, int32, retType = int64) }
    val inet_addr by func { declare(pointer, retType = int32) }
    val inet_aton by func { declare(pointer, pointer, retType = int32) }
    val inet_lnaof by func { declare(PosixType.IN_ADDR, retType = int32) }
    val inet_makeaddr by func { declare(int32, int32, retType = PosixType.IN_ADDR) }
    val inet_net_ntop by func { declare(int32, pointer, int32, pointer, int64, retType = pointer) }
    val inet_net_pton by func { declare(int32, pointer, pointer, int64, retType = int32) }
    val inet_neta by func { declare(int32, pointer, int64, retType = pointer) }
    val inet_netof by func { declare(PosixType.IN_ADDR, retType = int32) }
    val inet_network by func { declare(pointer, retType = int32) }
    val inet_nsap_addr by func { declare(pointer, pointer, int32, retType = int32) }
    val inet_nsap_ntoa by func { declare(int32, pointer, pointer, retType = pointer) }
    val inet_ntoa by func { declare(PosixType.IN_ADDR, retType = pointer) }
    val inet_ntop by func { declare(int32, pointer, pointer, int32, retType = pointer) }
    val inet_pton by func { declare(int32, pointer, pointer, retType = int32) }
    val initgroups by func { declare(pointer, int32, retType = int32) }
    val initstate by func { declare(int32, pointer, int64, retType = pointer) }
    val initstate_r by func { declare(int32, pointer, int64, pointer, retType = int32) }
    val innetgr by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val inotify_add_watch by func { declare(int32, pointer, int32, retType = int32) }
    val inotify_init by func { declare(retType = int32) }
    val inotify_init1 by func { declare(int32, retType = int32) }
    val inotify_rm_watch by func { declare(int32, int32, retType = int32) }
    val insque by func { declare(pointer, pointer) }
    val iruserok by func { if (darwin) declare(int64, int32, pointer, pointer, retType = int32) else declare(int32, int32, pointer, pointer, retType = int32) }
    val iruserok_af by func { declare(pointer, int32, pointer, pointer, int16, retType = int32) }
    val iruserok_sa by func { declare(pointer, int32, int32, pointer, pointer, retType = int32) }
    val isalnum by func { declare(int32, retType = int32) }
    val isalnum_l by func { declare(int32, pointer, retType = int32) }
    val isalpha by func { declare(int32, retType = int32) }
    val isalpha_l by func { declare(int32, pointer, retType = int32) }
    val isascii by func { declare(int32, retType = int32) }
    val isatty by func { declare(int32, retType = int32) }
    val isblank by func { declare(int32, retType = int32) }
    val isblank_l by func { declare(int32, pointer, retType = int32) }
    val iscntrl by func { declare(int32, retType = int32) }
    val iscntrl_l by func { declare(int32, pointer, retType = int32) }
    val isdigit by func { declare(int32, retType = int32) }
    val isdigit_l by func { declare(int32, pointer, retType = int32) }
    val isfdtype by func { declare(int32, int32, retType = int32) }
    val isgraph by func { declare(int32, retType = int32) }
    val isgraph_l by func { declare(int32, pointer, retType = int32) }
    val ishexnumber by func { declare(int32, retType = int32) }
    val ishexnumber_l by func { declare(int32, pointer, retType = int32) }
    val isideogram by func { declare(int32, retType = int32) }
    val isideogram_l by func { declare(int32, pointer, retType = int32) }
    val isinff by func { declare(float32, retType = int32) }
    val islower by func { declare(int32, retType = int32) }
    val islower_l by func { declare(int32, pointer, retType = int32) }
    val isnanf by func { declare(float32, retType = int32) }
    val isnumber by func { declare(int32, retType = int32) }
    val isnumber_l by func { declare(int32, pointer, retType = int32) }
    val isphonogram by func { declare(int32, retType = int32) }
    val isphonogram_l by func { declare(int32, pointer, retType = int32) }
    val isprint by func { declare(int32, retType = int32) }
    val isprint_l by func { declare(int32, pointer, retType = int32) }
    val ispunct by func { declare(int32, retType = int32) }
    val ispunct_l by func { declare(int32, pointer, retType = int32) }
    val isrune by func { declare(int32, retType = int32) }
    val isrune_l by func { declare(int32, pointer, retType = int32) }
    val issetugid by func { declare(retType = int32) }
    val isspace by func { declare(int32, retType = int32) }
    val isspace_l by func { declare(int32, pointer, retType = int32) }
    val isspecial by func { declare(int32, retType = int32) }
    val isspecial_l by func { declare(int32, pointer, retType = int32) }
    val isupper by func { declare(int32, retType = int32) }
    val isupper_l by func { declare(int32, pointer, retType = int32) }
    val iswalnum by func { declare(int32, retType = int32) }
    val iswalnum_l by func { declare(int32, pointer, retType = int32) }
    val iswalpha by func { declare(int32, retType = int32) }
    val iswalpha_l by func { declare(int32, pointer, retType = int32) }
    val iswascii by func { declare(int32, retType = int32) }
    val iswblank by func { declare(int32, retType = int32) }
    val iswblank_l by func { declare(int32, pointer, retType = int32) }
    val iswcntrl by func { declare(int32, retType = int32) }
    val iswcntrl_l by func { declare(int32, pointer, retType = int32) }
    val iswctype by func { if (darwin) declare(int32, int32, retType = int32) else declare(int32, int64, retType = int32) }
    val iswctype_l by func { if (darwin) declare(int32, int32, pointer, retType = int32) else declare(int32, int64, pointer, retType = int32) }
    val iswdigit by func { declare(int32, retType = int32) }
    val iswdigit_l by func { declare(int32, pointer, retType = int32) }
    val iswgraph by func { declare(int32, retType = int32) }
    val iswgraph_l by func { declare(int32, pointer, retType = int32) }
    val iswhexnumber by func { declare(int32, retType = int32) }
    val iswhexnumber_l by func { declare(int32, pointer, retType = int32) }
    val iswideogram by func { declare(int32, retType = int32) }
    val iswideogram_l by func { declare(int32, pointer, retType = int32) }
    val iswlower by func { declare(int32, retType = int32) }
    val iswlower_l by func { declare(int32, pointer, retType = int32) }
    val iswnumber by func { declare(int32, retType = int32) }
    val iswnumber_l by func { declare(int32, pointer, retType = int32) }
    val iswphonogram by func { declare(int32, retType = int32) }
    val iswphonogram_l by func { declare(int32, pointer, retType = int32) }
    val iswprint by func { declare(int32, retType = int32) }
    val iswprint_l by func { declare(int32, pointer, retType = int32) }
    val iswpunct by func { declare(int32, retType = int32) }
    val iswpunct_l by func { declare(int32, pointer, retType = int32) }
    val iswrune by func { declare(int32, retType = int32) }
    val iswrune_l by func { declare(int32, pointer, retType = int32) }
    val iswspace by func { declare(int32, retType = int32) }
    val iswspace_l by func { declare(int32, pointer, retType = int32) }
    val iswspecial by func { declare(int32, retType = int32) }
    val iswspecial_l by func { declare(int32, pointer, retType = int32) }
    val iswupper by func { declare(int32, retType = int32) }
    val iswupper_l by func { declare(int32, pointer, retType = int32) }
    val iswxdigit by func { declare(int32, retType = int32) }
    val iswxdigit_l by func { declare(int32, pointer, retType = int32) }
    val isxdigit by func { declare(int32, retType = int32) }
    val isxdigit_l by func { declare(int32, pointer, retType = int32) }
    val j0 by func { declare(float64, retType = float64) }
    val j0f by func { declare(float32, retType = float32) }
    val j1 by func { declare(float64, retType = float64) }
    val j1f by func { declare(float32, retType = float32) }
    val jn by func { declare(int32, float64, retType = float64) }
    val jnf by func { declare(int32, float32, retType = float32) }
    val jrand48 by func { declare(pointer, retType = int64) }
    val jrand48_r by func { declare(pointer, pointer, pointer, retType = int32) }
    val kevent by func { declare(int32, pointer, int32, pointer, int32, pointer, retType = int32) }
    val kevent64 by func { declare(int32, pointer, int32, pointer, int32, int32, pointer, retType = int32) }
    val kext_request by func { declare(int32, int32, int64, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val kill by func { declare(int32, int32, retType = int32) }
    val killpg by func { declare(int32, int32, retType = int32) }
    val klogctl by func { declare(int32, pointer, int32, retType = int32) }
    val kmod_control by func { declare(int32, int32, int32, pointer, pointer, retType = int32) }
    val kmod_create by func { declare(int32, int64, pointer, retType = int32) }
    val kmod_destroy by func { declare(int32, int32, retType = int32) }
    val kmod_get_info by func { declare(int32, pointer, pointer, retType = int32) }
    val kqueue by func { declare(retType = int32) }
    val l64a by func { declare(int64, retType = pointer) }
    val labs by func { declare(int64, retType = int64) }
    val launch_activate_socket by func { declare(pointer, pointer, pointer, retType = int32) }
    val launch_data_alloc by func { declare(int32, retType = pointer) }
    val launch_data_array_get_count by func { declare(pointer, retType = int64) }
    val launch_data_array_get_index by func { declare(pointer, int64, retType = pointer) }
    val launch_data_array_set_index by func { declare(pointer, pointer, int64, retType = boolean) }
    val launch_data_copy by func { declare(pointer, retType = pointer) }
    val launch_data_dict_get_count by func { declare(pointer, retType = int64) }
    val launch_data_dict_insert by func { declare(pointer, pointer, pointer, retType = boolean) }
    val launch_data_dict_iterate by func { declare(pointer, pointer, pointer) }
    val launch_data_dict_lookup by func { declare(pointer, pointer, retType = pointer) }
    val launch_data_dict_remove by func { declare(pointer, pointer, retType = boolean) }
    val launch_data_free by func { declare(pointer) }
    val launch_data_get_bool by func { declare(pointer, retType = boolean) }
    val launch_data_get_errno by func { declare(pointer, retType = int32) }
    val launch_data_get_fd by func { declare(pointer, retType = int32) }
    val launch_data_get_integer by func { declare(pointer, retType = int64) }
    val launch_data_get_machport by func { declare(pointer, retType = int32) }
    val launch_data_get_opaque by func { declare(pointer, retType = pointer) }
    val launch_data_get_opaque_size by func { declare(pointer, retType = int64) }
    val launch_data_get_real by func { declare(pointer, retType = float64) }
    val launch_data_get_string by func { declare(pointer, retType = pointer) }
    val launch_data_get_type by func { declare(pointer, retType = int32) }
    val launch_data_new_bool by func { declare(boolean, retType = pointer) }
    val launch_data_new_fd by func { declare(int32, retType = pointer) }
    val launch_data_new_integer by func { declare(int64, retType = pointer) }
    val launch_data_new_machport by func { declare(int32, retType = pointer) }
    val launch_data_new_opaque by func { declare(pointer, int64, retType = pointer) }
    val launch_data_new_real by func { declare(float64, retType = pointer) }
    val launch_data_new_string by func { declare(pointer, retType = pointer) }
    val launch_data_set_bool by func { declare(pointer, boolean, retType = boolean) }
    val launch_data_set_fd by func { declare(pointer, int32, retType = boolean) }
    val launch_data_set_integer by func { declare(pointer, int64, retType = boolean) }
    val launch_data_set_machport by func { declare(pointer, int32, retType = boolean) }
    val launch_data_set_opaque by func { declare(pointer, pointer, int64, retType = boolean) }
    val launch_data_set_real by func { declare(pointer, float64, retType = boolean) }
    val launch_data_set_string by func { declare(pointer, pointer, retType = boolean) }
    val launch_get_fd by func { declare(retType = int32) }
    val launch_msg by func { declare(pointer, retType = pointer) }
    val lchflags by func { declare(pointer, int32, retType = int32) }
    val lchmod by func { if (darwin) declare(pointer, int16, retType = int32) else declare(pointer, int32, retType = int32) }
    val lchown by func { declare(pointer, int32, int32, retType = int32) }
    val lcong48 by func { declare(pointer) }
    val lcong48_r by func { declare(pointer, pointer, retType = int32) }
    val ldexp by func { declare(float64, int32, retType = float64) }
    val ldexpf by func { declare(float32, int32, retType = float32) }
    val ldiv by func { declare(int64, int64, retType = PosixType.LDIV_T) }
    val lfind by func { declare(pointer, pointer, pointer, int64, pointer, retType = pointer) }
    val lgamma by func { declare(float64, retType = float64) }
    val lgammaf by func { declare(float32, retType = float32) }
    val lgetxattr by func { declare(pointer, pointer, pointer, int64, retType = int64) }
    val link by func { declare(pointer, pointer, retType = int32) }
    val linkat by func { declare(int32, pointer, int32, pointer, int32, retType = int32) }
    val lio_listio by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val listen by func { declare(int32, int32, retType = int32) }
    val llabs by func { declare(int64, retType = int64) }
    val lldiv by func { declare(int64, int64, retType = PosixType.LLDIV_T) }
    val llistxattr by func { declare(pointer, pointer, int64, retType = int64) }
    val llrint by func { declare(float64, retType = int64) }
    val llrintf by func { declare(float32, retType = int64) }
    val llround by func { declare(float64, retType = int64) }
    val llroundf by func { declare(float32, retType = int64) }
    val localeconv by func { declare(retType = pointer) }
    val localeconv_l by func { declare(pointer, retType = pointer) }
    val localtime by func { declare(pointer, retType = pointer) }
    val localtime_r by func { declare(pointer, pointer, retType = pointer) }
    val lock_set_create by func { declare(int32, pointer, int32, int32, retType = int32) }
    val lock_set_destroy by func { declare(int32, int32, retType = int32) }
    val lockf by func { declare(int32, int32, int64, retType = int32) }
    val log by func { declare(float64, retType = float64) }
    val log10 by func { declare(float64, retType = float64) }
    val log10f by func { declare(float32, retType = float32) }
    val log1p by func { declare(float64, retType = float64) }
    val log1pf by func { declare(float32, retType = float32) }
    val log2 by func { declare(float64, retType = float64) }
    val log2f by func { declare(float32, retType = float32) }
    val logb by func { declare(float64, retType = float64) }
    val logbf by func { declare(float32, retType = float32) }
    val logf by func { declare(float32, retType = float32) }
    val login by func { declare(pointer) }
    val login_tty by func { declare(int32, retType = int32) }
    val logout by func { declare(pointer, retType = int32) }
    val logwtmp by func { declare(pointer, pointer, pointer) }
    val lrand48 by func { declare(retType = int64) }
    val lrand48_r by func { declare(pointer, pointer, retType = int32) }
    val lremovexattr by func { declare(pointer, pointer, retType = int32) }
    val lrint by func { declare(float64, retType = int64) }
    val lrintf by func { declare(float32, retType = int64) }
    val lround by func { declare(float64, retType = int64) }
    val lroundf by func { declare(float32, retType = int64) }
    val lsearch by func { declare(pointer, pointer, pointer, int64, pointer, retType = pointer) }
    val lseek by func { declare(int32, int64, int32, retType = int64) }
    val lsetxattr by func { declare(pointer, pointer, pointer, int64, int32, retType = int32) }
    val lstat by func { declare(pointer, pointer, retType = int32) }
    val lstatx_np by func { declare(pointer, pointer, pointer, retType = int32) }
    val lutimes by func { declare(pointer, pointer, retType = int32) }
    val mach_absolute_time by func { declare(retType = int64) }
    val mach_continuous_time by func { declare(retType = int64) }
    val mach_error by func { declare(pointer, int32) }
    val mach_error_string by func { declare(int32, retType = pointer) }
    val mach_error_type by func { declare(int32, retType = pointer) }
    val mach_generate_activity_id by func { declare(int32, int32, pointer, retType = int32) }
    val mach_host_self by func { declare(retType = int32) }
    val mach_make_memory_entry by func { declare(int32, pointer, int64, int32, pointer, int32, retType = int32) }
    val mach_make_memory_entry_64 by func { declare(int32, pointer, int64, int32, pointer, int32, retType = int32) }
    val mach_memory_info by func { declare(int32, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val mach_memory_object_memory_entry by func { declare(int32, int32, int64, int32, int32, pointer, retType = int32) }
    val mach_memory_object_memory_entry_64 by func { declare(int32, int32, int64, int32, int32, pointer, retType = int32) }
    val mach_msg by func { declare(pointer, int32, int32, int32, int32, int32, int32, retType = int32) }
    val mach_msg_destroy by func { declare(pointer) }
    val mach_msg_overwrite by func { declare(pointer, int32, int32, int32, int32, int32, int32, pointer, int32, retType = int32) }
    val mach_msg_receive by func { declare(pointer, retType = int32) }
    val mach_msg_send by func { declare(pointer, retType = int32) }
    val mach_msg_server by func { declare(pointer, int32, int32, int32, retType = int32) }
    val mach_msg_server_importance by func { declare(pointer, int32, int32, int32, retType = int32) }
    val mach_msg_server_once by func { declare(pointer, int32, int32, int32, retType = int32) }
    val mach_port_allocate by func { declare(int32, int32, pointer, retType = int32) }
    val mach_port_allocate_full by func { declare(int32, int32, int32, pointer, pointer, retType = int32) }
    val mach_port_allocate_name by func { declare(int32, int32, int32, retType = int32) }
    val mach_port_allocate_qos by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val mach_port_construct by func { declare(int32, pointer, int64, pointer, retType = int32) }
    val mach_port_deallocate by func { declare(int32, int32, retType = int32) }
    val mach_port_destroy by func { declare(int32, int32, retType = int32) }
    val mach_port_destruct by func { declare(int32, int32, int32, int64, retType = int32) }
    val mach_port_dnrequest_info by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val mach_port_extract_member by func { declare(int32, int32, int32, retType = int32) }
    val mach_port_extract_right by func { declare(int32, int32, int32, pointer, pointer, retType = int32) }
    val mach_port_get_attributes by func { declare(int32, int32, int32, pointer, pointer, retType = int32) }
    val mach_port_get_context by func { declare(int32, int32, pointer, retType = int32) }
    val mach_port_get_refs by func { declare(int32, int32, int32, pointer, retType = int32) }
    val mach_port_get_set_status by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val mach_port_get_srights by func { declare(int32, int32, pointer, retType = int32) }
    val mach_port_guard by func { declare(int32, int32, int64, int32, retType = int32) }
    val mach_port_guard_with_flags by func { declare(int32, int32, int64, int64, retType = int32) }
    val mach_port_insert_member by func { declare(int32, int32, int32, retType = int32) }
    val mach_port_insert_right by func { declare(int32, int32, int32, int32, retType = int32) }
    val mach_port_kernel_object by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val mach_port_kobject by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val mach_port_kobject_description by func { declare(int32, int32, pointer, pointer, pointer, retType = int32) }
    val mach_port_mod_refs by func { declare(int32, int32, int32, int32, retType = int32) }
    val mach_port_move_member by func { declare(int32, int32, int32, retType = int32) }
    val mach_port_names by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val mach_port_peek by func { declare(int32, int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val mach_port_rename by func { declare(int32, int32, int32, retType = int32) }
    val mach_port_request_notification by func { declare(int32, int32, int32, int32, int32, int32, pointer, retType = int32) }
    val mach_port_set_attributes by func { declare(int32, int32, int32, pointer, int32, retType = int32) }
    val mach_port_set_context by func { declare(int32, int32, int64, retType = int32) }
    val mach_port_set_mscount by func { declare(int32, int32, int32, retType = int32) }
    val mach_port_set_seqno by func { declare(int32, int32, int32, retType = int32) }
    val mach_port_space_basic_info by func { declare(int32, pointer, retType = int32) }
    val mach_port_space_info by func { declare(int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val mach_port_swap_guard by func { declare(int32, int32, int64, int64, retType = int32) }
    val mach_port_type by func { declare(int32, int32, pointer, retType = int32) }
    val mach_port_unguard by func { declare(int32, int32, int64, retType = int32) }
    val mach_ports_lookup by func { declare(int32, pointer, pointer, retType = int32) }
    val mach_ports_register by func { declare(int32, pointer, int32, retType = int32) }
    val mach_task_is_self by func { declare(int32, retType = int32) }
    val mach_thread_self by func { declare(retType = int32) }
    val mach_timebase_info by func { declare(pointer, retType = int32) }
    val mach_vm_wire by func { declare(int32, int32, int64, int64, int32, retType = int32) }
    val mach_voucher_deallocate by func { declare(int32, retType = int32) }
    val mach_voucher_extract_attr_recipe_trap by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val mach_zone_info by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val macx_backing_store_recovery by func { declare(int32, retType = int32) }
    val macx_backing_store_suspend by func { declare(int32, retType = int32) }
    val macx_swapoff by func { declare(int64, int32, retType = int32) }
    val macx_swapon by func { declare(int64, int32, int32, int32, retType = int32) }
    val macx_triggers by func { declare(int32, int32, int32, int32, retType = int32) }
    val madvise by func { declare(pointer, int64, int32, retType = int32) }
    val malloc by func { declare(int64, retType = pointer) }
    val malloc_info by func { declare(int32, pointer, retType = int32) }
    val malloc_stats by func { declare() }
    val malloc_trim by func { declare(int64, retType = int32) }
    val malloc_usable_size by func { declare(pointer, retType = int64) }
    val mallopt by func { declare(int32, int32, retType = int32) }
    val mblen by func { declare(pointer, int64, retType = int32) }
    val mblen_l by func { declare(pointer, int64, pointer, retType = int32) }
    val mbr_check_membership by func { declare(pointer, pointer, pointer, retType = int32) }
    val mbr_check_service_membership by func { declare(pointer, pointer, pointer, retType = int32) }
    val mbr_gid_to_uuid by func { declare(int32, pointer, retType = int32) }
    val mbr_identifier_to_uuid by func { declare(int32, pointer, int64, pointer, retType = int32) }
    val mbr_sid_to_string by func { declare(pointer, pointer, retType = int32) }
    val mbr_sid_to_uuid by func { declare(pointer, pointer, retType = int32) }
    val mbr_string_to_sid by func { declare(pointer, pointer, retType = int32) }
    val mbr_uid_to_uuid by func { declare(int32, pointer, retType = int32) }
    val mbr_uuid_to_id by func { declare(pointer, pointer, pointer, retType = int32) }
    val mbr_uuid_to_sid by func { declare(pointer, pointer, retType = int32) }
    val mbrlen by func { declare(pointer, int64, pointer, retType = int64) }
    val mbrlen_l by func { declare(pointer, int64, pointer, pointer, retType = int64) }
    val mbrtoc16 by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val mbrtoc32 by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val mbrtowc by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val mbrtowc_l by func { declare(pointer, pointer, int64, pointer, pointer, retType = int64) }
    val mbsinit by func { declare(pointer, retType = int32) }
    val mbsinit_l by func { declare(pointer, pointer, retType = int32) }
    val mbsnrtowcs by func { declare(pointer, pointer, int64, int64, pointer, retType = int64) }
    val mbsnrtowcs_l by func { declare(pointer, pointer, int64, int64, pointer, pointer, retType = int64) }
    val mbsrtowcs by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val mbsrtowcs_l by func { declare(pointer, pointer, int64, pointer, pointer, retType = int64) }
    val mbstowcs by func { declare(pointer, pointer, int64, retType = int64) }
    val mbstowcs_l by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val mbtowc by func { declare(pointer, pointer, int64, retType = int32) }
    val mbtowc_l by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val mcheck by func { declare(pointer, retType = int32) }
    val mcheck_check_all by func { declare() }
    val mcheck_pedantic by func { declare(pointer, retType = int32) }
    val memalign by func { declare(int64, int64, retType = pointer) }
    val memccpy by func { declare(pointer, pointer, int32, int64, retType = pointer) }
    val memchr by func { declare(pointer, int32, int64, retType = pointer) }
    val memcmp by func { declare(pointer, pointer, int64, retType = int32) }
    val memcpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val memfd_create by func { declare(pointer, int32, retType = int32) }
    val memmem by func { declare(pointer, int64, pointer, int64, retType = pointer) }
    val memmove by func { declare(pointer, pointer, int64, retType = pointer) }
    val memset by func { declare(pointer, int32, int64, retType = pointer) }
    val memset_pattern16 by func { declare(pointer, pointer, int64) }
    val memset_pattern4 by func { declare(pointer, pointer, int64) }
    val memset_pattern8 by func { declare(pointer, pointer, int64) }
    val memset_s by func { declare(pointer, int64, int32, int64, retType = int32) }
    val mergesort by func { declare(pointer, int64, int64, pointer, retType = int32) }
    val mig_allocate by func { declare(pointer, int64) }
    val mig_dealloc_reply_port by func { declare(int32) }
    val mig_deallocate by func { declare(int64, int64) }
    val mig_get_reply_port by func { declare(retType = int32) }
    val mig_put_reply_port by func { declare(int32) }
    val mig_reply_setup by func { declare(pointer, pointer) }
    val mig_strncpy by func { declare(pointer, pointer, int32, retType = int32) }
    val mig_strncpy_zerofill by func { declare(pointer, pointer, int32, retType = int32) }
    val mincore by func { declare(pointer, int64, pointer, retType = int32) }
    val minherit by func { declare(pointer, int64, int32, retType = int32) }
    val mkdir by func { if (darwin) declare(pointer, int16, retType = int32) else declare(pointer, int32, retType = int32) }
    val mkdirat by func { if (darwin) declare(int32, pointer, int16, retType = int32) else declare(int32, pointer, int32, retType = int32) }
    val mkdirx_np by func { declare(pointer, pointer, retType = int32) }
    val mkdtemp by func { declare(pointer, retType = pointer) }
    val mkdtempat_np by func { declare(int32, pointer, retType = pointer) }
    val mkfifo by func { if (darwin) declare(pointer, int16, retType = int32) else declare(pointer, int32, retType = int32) }
    val mkfifoat by func { declare(int32, pointer, int32, retType = int32) }
    val mkfifox_np by func { declare(pointer, pointer, retType = int32) }
    val mknod by func { if (darwin) declare(pointer, int16, int32, retType = int32) else declare(pointer, int32, int64, retType = int32) }
    val mknodat by func { declare(int32, pointer, int32, int64, retType = int32) }
    val mkostemp by func { declare(pointer, int32, retType = int32) }
    val mkostemps by func { declare(pointer, int32, int32, retType = int32) }
    val mkostempsat_np by func { declare(int32, pointer, int32, int32, retType = int32) }
    val mkpath_np by func { declare(pointer, int16, retType = int32) }
    val mkpathat_np by func { declare(int32, pointer, int16, retType = int32) }
    val mkstemp by func { declare(pointer, retType = int32) }
    val mkstemps by func { declare(pointer, int32, retType = int32) }
    val mkstempsat_np by func { declare(int32, pointer, int32, retType = int32) }
    val mktemp by func { declare(pointer, retType = pointer) }
    val mktime by func { declare(pointer, retType = int64) }
    val mlock by func { declare(pointer, int64, retType = int32) }
    val mlockall by func { declare(int32, retType = int32) }
    val mmap by func { declare(pointer, int64, int32, int32, int32, int64, retType = pointer) }
    val modf by func { declare(float64, pointer, retType = float64) }
    val modff by func { declare(float32, pointer, retType = float32) }
    val mpool_close by func { declare(pointer, retType = int32) }
    val mpool_filter by func { declare(pointer, pointer, pointer, pointer) }
    val mpool_get by func { declare(pointer, int32, int32, retType = pointer) }
    val mpool_new by func { declare(pointer, pointer, retType = pointer) }
    val mpool_open by func { declare(pointer, int32, int32, int32, retType = pointer) }
    val mpool_put by func { declare(pointer, pointer, int32, retType = int32) }
    val mpool_sync by func { declare(pointer, retType = int32) }
    val mprobe by func { declare(pointer, retType = int32) }
    val mprotect by func { declare(pointer, int64, int32, retType = int32) }
    val mq_close by func { declare(int32, retType = int32) }
    val mq_getattr by func { declare(int32, pointer, retType = int32) }
    val mq_notify by func { declare(int32, pointer, retType = int32) }
    val mq_receive by func { declare(int32, pointer, int64, pointer, retType = int64) }
    val mq_send by func { declare(int32, pointer, int64, int32, retType = int32) }
    val mq_setattr by func { declare(int32, pointer, pointer, retType = int32) }
    val mq_timedreceive by func { declare(int32, pointer, int64, pointer, pointer, retType = int64) }
    val mq_timedsend by func { declare(int32, pointer, int64, int32, pointer, retType = int32) }
    val mq_unlink by func { declare(pointer, retType = int32) }
    val mrand48 by func { declare(retType = int64) }
    val mrand48_r by func { declare(pointer, pointer, retType = int32) }
    val msgctl by func { declare(int32, int32, pointer, retType = int32) }
    val msgget by func { declare(int32, int32, retType = int32) }
    val msgrcv by func { declare(int32, pointer, int64, int64, int32, retType = int64) }
    val msgsnd by func { declare(int32, pointer, int64, int32, retType = int32) }
    val msync by func { declare(pointer, int64, int32, retType = int32) }
    val mtrace by func { declare() }
    val mtx_destroy by func { declare(pointer) }
    val mtx_init by func { declare(pointer, int32, retType = int32) }
    val mtx_lock by func { declare(pointer, retType = int32) }
    val mtx_timedlock by func { declare(pointer, pointer, retType = int32) }
    val mtx_trylock by func { declare(pointer, retType = int32) }
    val mtx_unlock by func { declare(pointer, retType = int32) }
    val munlock by func { declare(pointer, int64, retType = int32) }
    val munlockall by func { declare(retType = int32) }
    val munmap by func { declare(pointer, int64, retType = int32) }
    val muntrace by func { declare() }
    val nan by func { declare(pointer, retType = float64) }
    val nanf by func { declare(pointer, retType = float32) }
    val nanosleep by func { declare(pointer, pointer, retType = int32) }
    val nearbyint by func { declare(float64, retType = float64) }
    val nearbyintf by func { declare(float32, retType = float32) }
    val new_printf_comp by func { declare(pointer, pointer, pointer, retType = pointer) }
    val new_printf_domain by func { declare(retType = pointer) }
    val newlocale by func { declare(int32, pointer, pointer, retType = pointer) }
    val nextafter by func { declare(float64, float64, retType = float64) }
    val nextafterf by func { declare(float32, float32, retType = float32) }
    val nextwctype by func { declare(int32, int32, retType = int32) }
    val nextwctype_l by func { declare(int32, int32, pointer, retType = int32) }
    val nfssvc by func { declare(int32, pointer, retType = int32) }
    val nftw by func { declare(pointer, pointer, int32, int32, retType = int32) }
    val ngettext by func { declare(pointer, pointer, int64, retType = pointer) }
    val nice by func { declare(int32, retType = int32) }
    val nl_langinfo by func { declare(int32, retType = pointer) }
    val nl_langinfo_l by func { declare(int32, pointer, retType = pointer) }
    val notify_cancel by func { declare(int32, retType = int32) }
    val notify_check by func { declare(int32, pointer, retType = int32) }
    val notify_get_state by func { declare(int32, pointer, retType = int32) }
    val notify_is_valid_token by func { declare(int32, retType = boolean) }
    val notify_post by func { declare(pointer, retType = int32) }
    val notify_register_check by func { declare(pointer, pointer, retType = int32) }
    val notify_register_file_descriptor by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val notify_register_mach_port by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val notify_register_signal by func { declare(pointer, int32, pointer, retType = int32) }
    val notify_resume by func { declare(int32, retType = int32) }
    val notify_set_state by func { declare(int32, int64, retType = int32) }
    val notify_suspend by func { declare(int32, retType = int32) }
    val nrand48 by func { declare(pointer, retType = int64) }
    val nrand48_r by func { declare(pointer, pointer, pointer, retType = int32) }
    val ns_name_compress by func { declare(pointer, pointer, int64, pointer, pointer, retType = int32) }
    val ns_name_ntop by func { declare(pointer, pointer, int64, retType = int32) }
    val ns_name_pack by func { declare(pointer, pointer, int32, pointer, pointer, retType = int32) }
    val ns_name_pton by func { declare(pointer, pointer, int64, retType = int32) }
    val ns_name_skip by func { declare(pointer, pointer, retType = int32) }
    val ns_name_uncompress by func { declare(pointer, pointer, pointer, pointer, int64, retType = int32) }
    val ns_name_unpack by func { declare(pointer, pointer, pointer, pointer, int64, retType = int32) }
    val ntohl by func { declare(int32, retType = int32) }
    val ntohs by func { declare(int16, retType = int16) }
    val nvis by func { declare(pointer, int64, int32, int32, int32, retType = pointer) }
    val obstack_free by func { declare(pointer, pointer) }
    val on_exit by func { declare(pointer, pointer, retType = int32) }
    val open_memstream by func { declare(pointer, pointer, retType = pointer) }
    val open_wmemstream by func { declare(pointer, pointer, retType = pointer) }
    val opendev by func { declare(pointer, int32, int32, pointer, retType = int32) }
    val opendir by func { declare(pointer, retType = pointer) }
    val openlog by func { declare(pointer, int32, int32) }
    val openpty by func { declare(pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val openx_np by func { declare(pointer, int32, pointer, retType = int32) }
    val os_activity_get_active by func { declare(pointer, pointer, retType = int32) }
    val os_activity_scope_leave by func { declare(pointer) }
    val os_trace_debug_enabled by func { declare(retType = boolean) }
    val os_trace_info_enabled by func { declare(retType = boolean) }
    val os_unfair_lock_assert_not_owner by func { declare(pointer) }
    val os_unfair_lock_assert_owner by func { declare(pointer) }
    val os_unfair_lock_lock by func { declare(pointer) }
    val os_unfair_lock_trylock by func { declare(pointer, retType = boolean) }
    val os_unfair_lock_unlock by func { declare(pointer) }
    val panic_init by func { declare(int32) }
    val parse_printf_format by func { declare(pointer, int64, pointer, retType = int64) }
    val pathconf by func { declare(pointer, int32, retType = int64) }
    val pause by func { declare(retType = int32) }
    val pclose by func { declare(pointer, retType = int32) }
    val perror by func { declare(pointer) }
    val personality by func { declare(int64, retType = int32) }
    val pid_for_task by func { declare(int32, pointer, retType = int32) }
    val pidfd_getfd by func { declare(int32, int32, int32, retType = int32) }
    val pidfd_open by func { declare(int32, int32, retType = int32) }
    val pidfd_send_signal by func { declare(int32, int32, pointer, int32, retType = int32) }
    val pipe by func { declare(pointer, retType = int32) }
    val pipe2 by func { declare(pointer, int32, retType = int32) }
    val poll by func { if (darwin) declare(pointer, int32, int32, retType = int32) else declare(pointer, int64, int32, retType = int32) }
    val popen by func { declare(pointer, pointer, retType = pointer) }
    val posix2time by func { declare(int64, retType = int64) }
    val posix_fadvise by func { declare(int32, int64, int64, int32, retType = int32) }
    val posix_fallocate by func { declare(int32, int64, int64, retType = int32) }
    val posix_madvise by func { declare(pointer, int64, int32, retType = int32) }
    val posix_memalign by func { declare(pointer, int64, int64, retType = int32) }
    val posix_openpt by func { declare(int32, retType = int32) }
    val posix_spawn by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val posix_spawn_file_actions_addclose by func { declare(pointer, int32, retType = int32) }
    val posix_spawn_file_actions_adddup2 by func { declare(pointer, int32, int32, retType = int32) }
    val posix_spawn_file_actions_addopen by func { if (darwin) declare(pointer, int32, pointer, int32, int16, retType = int32) else declare(pointer, int32, pointer, int32, int32, retType = int32) }
    val posix_spawn_file_actions_destroy by func { declare(pointer, retType = int32) }
    val posix_spawn_file_actions_init by func { declare(pointer, retType = int32) }
    val posix_spawnattr_destroy by func { declare(pointer, retType = int32) }
    val posix_spawnattr_getflags by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_getpgroup by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_getschedparam by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_getschedpolicy by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_getsigdefault by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_getsigmask by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_init by func { declare(pointer, retType = int32) }
    val posix_spawnattr_setflags by func { declare(pointer, int16, retType = int32) }
    val posix_spawnattr_setpgroup by func { declare(pointer, int32, retType = int32) }
    val posix_spawnattr_setschedparam by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_setschedpolicy by func { declare(pointer, int32, retType = int32) }
    val posix_spawnattr_setsigdefault by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnattr_setsigmask by func { declare(pointer, pointer, retType = int32) }
    val posix_spawnp by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val pow by func { declare(float64, float64, retType = float64) }
    val powf by func { declare(float32, float32, retType = float32) }
    val ppoll by func { declare(pointer, int64, pointer, pointer, retType = int32) }
    val pread by func { declare(int32, pointer, int64, int64, retType = int64) }
    val printf_size by func { declare(pointer, pointer, pointer, retType = int32) }
    val printf_size_info by func { declare(pointer, int64, pointer, retType = int32) }
    val processor_assign by func { declare(int32, int32, int32, retType = int32) }
    val processor_control by func { declare(int32, pointer, int32, retType = int32) }
    val processor_exit by func { declare(int32, retType = int32) }
    val processor_get_assignment by func { declare(int32, pointer, retType = int32) }
    val processor_info by func { declare(int32, int32, pointer, pointer, pointer, retType = int32) }
    val processor_set_create by func { declare(int32, pointer, pointer, retType = int32) }
    val processor_set_default by func { declare(int32, pointer, retType = int32) }
    val processor_set_destroy by func { declare(int32, retType = int32) }
    val processor_set_info by func { declare(int32, int32, pointer, pointer, pointer, retType = int32) }
    val processor_set_max_priority by func { declare(int32, int32, int32, retType = int32) }
    val processor_set_policy_control by func { declare(int32, int32, pointer, int32, int32, retType = int32) }
    val processor_set_policy_disable by func { declare(int32, int32, int32, retType = int32) }
    val processor_set_policy_enable by func { declare(int32, int32, retType = int32) }
    val processor_set_stack_usage by func { declare(int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val processor_set_statistics by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val processor_set_tasks by func { declare(int32, pointer, pointer, retType = int32) }
    val processor_set_tasks_with_flavor by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val processor_set_threads by func { declare(int32, pointer, pointer, retType = int32) }
    val processor_start by func { declare(int32, retType = int32) }
    val profil by func { declare(pointer, int64, int64, int32, retType = int32) }
    val pselect by func { declare(int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val psiginfo by func { declare(pointer, pointer) }
    val psignal by func { declare(int32, pointer) }
    val psort by func { declare(pointer, int64, int64, pointer) }
    val psort_r by func { declare(pointer, int64, int64, pointer, pointer) }
    val pthread_atfork by func { declare(pointer, pointer, pointer, retType = int32) }
    val pthread_attr_destroy by func { declare(pointer, retType = int32) }
    val pthread_attr_get_qos_class_np by func { declare(pointer, pointer, pointer, retType = int32) }
    val pthread_attr_getdetachstate by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_getguardsize by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_getinheritsched by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_getschedparam by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_getschedpolicy by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_getscope by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_getstack by func { declare(pointer, pointer, pointer, retType = int32) }
    val pthread_attr_getstackaddr by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_getstacksize by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_init by func { declare(pointer, retType = int32) }
    val pthread_attr_set_qos_class_np by func { declare(pointer, int32, int32, retType = int32) }
    val pthread_attr_setdetachstate by func { declare(pointer, int32, retType = int32) }
    val pthread_attr_setguardsize by func { declare(pointer, int64, retType = int32) }
    val pthread_attr_setinheritsched by func { declare(pointer, int32, retType = int32) }
    val pthread_attr_setschedparam by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_setschedpolicy by func { declare(pointer, int32, retType = int32) }
    val pthread_attr_setscope by func { declare(pointer, int32, retType = int32) }
    val pthread_attr_setstack by func { declare(pointer, pointer, int64, retType = int32) }
    val pthread_attr_setstackaddr by func { declare(pointer, pointer, retType = int32) }
    val pthread_attr_setstacksize by func { declare(pointer, int64, retType = int32) }
    val pthread_barrier_destroy by func { declare(pointer, retType = int32) }
    val pthread_barrier_init by func { declare(pointer, pointer, int32, retType = int32) }
    val pthread_barrier_wait by func { declare(pointer, retType = int32) }
    val pthread_barrierattr_destroy by func { declare(pointer, retType = int32) }
    val pthread_barrierattr_getpshared by func { declare(pointer, pointer, retType = int32) }
    val pthread_barrierattr_init by func { declare(pointer, retType = int32) }
    val pthread_barrierattr_setpshared by func { declare(pointer, int32, retType = int32) }
    val pthread_cancel by func { if (darwin) declare(pointer, retType = int32) else declare(int64, retType = int32) }
    val pthread_cond_broadcast by func { declare(pointer, retType = int32) }
    val pthread_cond_clockwait by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val pthread_cond_destroy by func { declare(pointer, retType = int32) }
    val pthread_cond_init by func { declare(pointer, pointer, retType = int32) }
    val pthread_cond_signal by func { declare(pointer, retType = int32) }
    val pthread_cond_signal_thread_np by func { declare(pointer, pointer, retType = int32) }
    val pthread_cond_timedwait by func { declare(pointer, pointer, pointer, retType = int32) }
    val pthread_cond_timedwait_relative_np by func { declare(pointer, pointer, pointer, retType = int32) }
    val pthread_cond_wait by func { declare(pointer, pointer, retType = int32) }
    val pthread_condattr_destroy by func { declare(pointer, retType = int32) }
    val pthread_condattr_getclock by func { declare(pointer, pointer, retType = int32) }
    val pthread_condattr_getpshared by func { declare(pointer, pointer, retType = int32) }
    val pthread_condattr_init by func { declare(pointer, retType = int32) }
    val pthread_condattr_setclock by func { declare(pointer, int32, retType = int32) }
    val pthread_condattr_setpshared by func { declare(pointer, int32, retType = int32) }
    val pthread_cpu_number_np by func { declare(pointer, retType = int32) }
    val pthread_create by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val pthread_create_suspended_np by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val pthread_detach by func { if (darwin) declare(pointer, retType = int32) else declare(int64, retType = int32) }
    val pthread_equal by func { if (darwin) declare(pointer, pointer, retType = int32) else declare(int64, int64, retType = int32) }
    val pthread_from_mach_thread_np by func { declare(int32, retType = pointer) }
    val pthread_get_qos_class_np by func { declare(pointer, pointer, pointer, retType = int32) }
    val pthread_get_stackaddr_np by func { declare(pointer, retType = pointer) }
    val pthread_get_stacksize_np by func { declare(pointer, retType = int64) }
    val pthread_getconcurrency by func { declare(retType = int32) }
    val pthread_getcpuclockid by func { declare(int64, pointer, retType = int32) }
    val pthread_getschedparam by func { if (darwin) declare(pointer, pointer, pointer, retType = int32) else declare(int64, pointer, pointer, retType = int32) }
    val pthread_getspecific by func { if (darwin) declare(int64, retType = pointer) else declare(int32, retType = pointer) }
    val pthread_getugid_np by func { declare(pointer, pointer, retType = int32) }
    val pthread_is_threaded_np by func { declare(retType = int32) }
    val pthread_jit_write_protect_np by func { declare(int32) }
    val pthread_jit_write_protect_supported_np by func { declare(retType = int32) }
    val pthread_join by func { if (darwin) declare(pointer, pointer, retType = int32) else declare(int64, pointer, retType = int32) }
    val pthread_key_create by func { declare(pointer, pointer, retType = int32) }
    val pthread_key_delete by func { if (darwin) declare(int64, retType = int32) else declare(int32, retType = int32) }
    val pthread_kill by func { if (darwin) declare(pointer, int32, retType = int32) else declare(int64, int32, retType = int32) }
    val pthread_mach_thread_np by func { declare(pointer, retType = int32) }
    val pthread_main_np by func { declare(retType = int32) }
    val pthread_mutex_clocklock by func { declare(pointer, int32, pointer, retType = int32) }
    val pthread_mutex_consistent by func { declare(pointer, retType = int32) }
    val pthread_mutex_destroy by func { declare(pointer, retType = int32) }
    val pthread_mutex_getprioceiling by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutex_init by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutex_lock by func { declare(pointer, retType = int32) }
    val pthread_mutex_setprioceiling by func { declare(pointer, int32, pointer, retType = int32) }
    val pthread_mutex_timedlock by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutex_trylock by func { declare(pointer, retType = int32) }
    val pthread_mutex_unlock by func { declare(pointer, retType = int32) }
    val pthread_mutexattr_destroy by func { declare(pointer, retType = int32) }
    val pthread_mutexattr_getpolicy_np by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutexattr_getprioceiling by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutexattr_getprotocol by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutexattr_getpshared by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutexattr_getrobust by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutexattr_gettype by func { declare(pointer, pointer, retType = int32) }
    val pthread_mutexattr_init by func { declare(pointer, retType = int32) }
    val pthread_mutexattr_setpolicy_np by func { declare(pointer, int32, retType = int32) }
    val pthread_mutexattr_setprioceiling by func { declare(pointer, int32, retType = int32) }
    val pthread_mutexattr_setprotocol by func { declare(pointer, int32, retType = int32) }
    val pthread_mutexattr_setpshared by func { declare(pointer, int32, retType = int32) }
    val pthread_mutexattr_setrobust by func { declare(pointer, int32, retType = int32) }
    val pthread_mutexattr_settype by func { declare(pointer, int32, retType = int32) }
    val pthread_once by func { declare(pointer, pointer, retType = int32) }
    val pthread_override_qos_class_end_np by func { declare(pointer, retType = int32) }
    val pthread_override_qos_class_start_np by func { declare(pointer, int32, int32, retType = pointer) }
    val pthread_rwlock_clockrdlock by func { declare(pointer, int32, pointer, retType = int32) }
    val pthread_rwlock_clockwrlock by func { declare(pointer, int32, pointer, retType = int32) }
    val pthread_rwlock_destroy by func { declare(pointer, retType = int32) }
    val pthread_rwlock_init by func { declare(pointer, pointer, retType = int32) }
    val pthread_rwlock_rdlock by func { declare(pointer, retType = int32) }
    val pthread_rwlock_timedrdlock by func { declare(pointer, pointer, retType = int32) }
    val pthread_rwlock_timedwrlock by func { declare(pointer, pointer, retType = int32) }
    val pthread_rwlock_tryrdlock by func { declare(pointer, retType = int32) }
    val pthread_rwlock_trywrlock by func { declare(pointer, retType = int32) }
    val pthread_rwlock_unlock by func { declare(pointer, retType = int32) }
    val pthread_rwlock_wrlock by func { declare(pointer, retType = int32) }
    val pthread_rwlockattr_destroy by func { declare(pointer, retType = int32) }
    val pthread_rwlockattr_getkind_np by func { declare(pointer, pointer, retType = int32) }
    val pthread_rwlockattr_getpshared by func { declare(pointer, pointer, retType = int32) }
    val pthread_rwlockattr_init by func { declare(pointer, retType = int32) }
    val pthread_rwlockattr_setkind_np by func { declare(pointer, int32, retType = int32) }
    val pthread_rwlockattr_setpshared by func { declare(pointer, int32, retType = int32) }
    val pthread_self by func { if (darwin) declare(retType = pointer) else declare(retType = int64) }
    val pthread_set_qos_class_self_np by func { declare(int32, int32, retType = int32) }
    val pthread_setcancelstate by func { declare(int32, pointer, retType = int32) }
    val pthread_setcanceltype by func { declare(int32, pointer, retType = int32) }
    val pthread_setconcurrency by func { declare(int32, retType = int32) }
    val pthread_setschedparam by func { if (darwin) declare(pointer, int32, pointer, retType = int32) else declare(int64, int32, pointer, retType = int32) }
    val pthread_setschedprio by func { declare(int64, int32, retType = int32) }
    val pthread_setspecific by func { if (darwin) declare(int64, pointer, retType = int32) else declare(int32, pointer, retType = int32) }
    val pthread_setugid_np by func { declare(int32, int32, retType = int32) }
    val pthread_sigmask by func { declare(int32, pointer, pointer, retType = int32) }
    val pthread_spin_destroy by func { declare(pointer, retType = int32) }
    val pthread_spin_init by func { declare(pointer, int32, retType = int32) }
    val pthread_spin_lock by func { declare(pointer, retType = int32) }
    val pthread_spin_trylock by func { declare(pointer, retType = int32) }
    val pthread_spin_unlock by func { declare(pointer, retType = int32) }
    val pthread_testcancel by func { declare() }
    val pthread_threadid_np by func { declare(pointer, pointer, retType = int32) }
    val pthread_yield_np by func { declare() }
    val ptsname by func { declare(int32, retType = pointer) }
    val ptsname_r by func { declare(int32, pointer, int64, retType = int32) }
    val putc by func { declare(int32, pointer, retType = int32) }
    val putc_unlocked by func { declare(int32, pointer, retType = int32) }
    val putchar by func { declare(int32, retType = int32) }
    val putchar_unlocked by func { declare(int32, retType = int32) }
    val putenv by func { declare(pointer, retType = int32) }
    val puts by func { declare(pointer, retType = int32) }
    val putsgent by func { declare(pointer, pointer, retType = int32) }
    val pututline by func { declare(pointer, retType = pointer) }
    val pututxline by func { declare(pointer, retType = pointer) }
    val putw by func { declare(int32, pointer, retType = int32) }
    val putwc by func { declare(int32, pointer, retType = int32) }
    val putwc_l by func { declare(int32, pointer, pointer, retType = int32) }
    val putwchar by func { declare(int32, retType = int32) }
    val putwchar_l by func { declare(int32, pointer, retType = int32) }
    val pvalloc by func { declare(int64, retType = pointer) }
    val pwrite by func { declare(int32, pointer, int64, int64, retType = int64) }
    val qos_class_main by func { declare(retType = int32) }
    val qos_class_self by func { declare(retType = int32) }
    val qsort by func { declare(pointer, int64, int64, pointer) }
    val qsort_r by func { declare(pointer, int64, int64, pointer, pointer) }
    val querylocale by func { declare(int32, pointer, retType = pointer) }
    val quick_exit by func { declare(int32) }
    val radixsort by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val raise by func { declare(int32, retType = int32) }
    val rand by func { declare(retType = int32) }
    val rand_r by func { declare(pointer, retType = int32) }
    val random by func { declare(retType = int64) }
    val random_r by func { declare(pointer, pointer, retType = int32) }
    val rcmd by func { if (darwin) declare(pointer, int32, pointer, pointer, pointer, pointer, retType = int32) else declare(pointer, int16, pointer, pointer, pointer, pointer, retType = int32) }
    val rcmd_af by func { if (darwin) declare(pointer, int32, pointer, pointer, pointer, pointer, int32, retType = int32) else declare(pointer, int16, pointer, pointer, pointer, pointer, int16, retType = int32) }
    val read by func { declare(int32, pointer, int64, retType = int64) }
    val readdir by func { declare(pointer, retType = pointer) }
    val readdir_r by func { declare(pointer, pointer, pointer, retType = int32) }
    val readlink by func { declare(pointer, pointer, int64, retType = int64) }
    val readlinkat by func { declare(int32, pointer, pointer, int64, retType = int64) }
    val readpassphrase by func { declare(pointer, pointer, int64, int32, retType = pointer) }
    val readv by func { declare(int32, pointer, int32, retType = int64) }
    val realloc by func { declare(pointer, int64, retType = pointer) }
    val reallocarray by func { declare(pointer, int64, int64, retType = pointer) }
    val reallocf by func { declare(pointer, int64, retType = pointer) }
    val realpath by func { declare(pointer, pointer, retType = pointer) }
    val reboot by func { declare(int32, retType = int32) }
    val recv by func { declare(int32, pointer, int64, int32, retType = int64) }
    val recvfrom by func { declare(int32, pointer, int64, int32, pointer, pointer, retType = int64) }
    val recvmsg by func { declare(int32, pointer, int32, retType = int64) }
    val regcomp by func { declare(pointer, pointer, int32, retType = int32) }
    val regcomp_l by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val regerror by func { declare(int32, pointer, pointer, int64, retType = int64) }
    val regexec by func { declare(pointer, pointer, int64, pointer, int32, retType = int32) }
    val regfree by func { declare(pointer) }
    val register_printf_domain_function by func { declare(pointer, int32, pointer, pointer, pointer, retType = int32) }
    val register_printf_domain_render_std by func { declare(pointer, pointer, retType = int32) }
    val register_printf_modifier by func { declare(pointer, retType = int32) }
    val regncomp by func { declare(pointer, pointer, int64, int32, retType = int32) }
    val regncomp_l by func { declare(pointer, pointer, int64, int32, pointer, retType = int32) }
    val regnexec by func { declare(pointer, pointer, int64, int64, pointer, int32, retType = int32) }
    val regwcomp by func { declare(pointer, pointer, int32, retType = int32) }
    val regwcomp_l by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val regwexec by func { declare(pointer, pointer, int64, pointer, int32, retType = int32) }
    val regwncomp by func { declare(pointer, pointer, int64, int32, retType = int32) }
    val regwnexec by func { declare(pointer, pointer, int64, int64, pointer, int32, retType = int32) }
    val remainder by func { declare(float64, float64, retType = float64) }
    val remainderf by func { declare(float32, float32, retType = float32) }
    val remove by func { declare(pointer, retType = int32) }
    val removefile by func { declare(pointer, pointer, int32, retType = int32) }
    val removefile_cancel by func { declare(pointer, retType = int32) }
    val removefile_state_alloc by func { declare(retType = pointer) }
    val removefile_state_free by func { declare(pointer, retType = int32) }
    val removefile_state_get by func { declare(pointer, int32, pointer, retType = int32) }
    val removefile_state_set by func { declare(pointer, int32, pointer, retType = int32) }
    val removefileat by func { declare(int32, pointer, pointer, int32, retType = int32) }
    val remque by func { declare(pointer) }
    val remquo by func { declare(float64, float64, pointer, retType = float64) }
    val remquof by func { declare(float32, float32, pointer, retType = float32) }
    val rename by func { declare(pointer, pointer, retType = int32) }
    val renameat by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val renameatx_np by func { declare(int32, pointer, int32, pointer, int32, retType = int32) }
    val renamex_np by func { declare(pointer, pointer, int32, retType = int32) }
    val revoke by func { declare(pointer, retType = int32) }
    val rewind by func { declare(pointer) }
    val rewinddir by func { declare(pointer) }
    val rexec by func { declare(pointer, int32, pointer, pointer, pointer, pointer, retType = int32) }
    val rexec_af by func { declare(pointer, int32, pointer, pointer, pointer, pointer, int16, retType = int32) }
    val rindex by func { declare(pointer, int32, retType = pointer) }
    val rint by func { declare(float64, retType = float64) }
    val rintf by func { declare(float32, retType = float32) }
    val rmdir by func { declare(pointer, retType = int32) }
    val round by func { declare(float64, retType = float64) }
    val roundf by func { declare(float32, retType = float32) }
    val rpmatch by func { declare(pointer, retType = int32) }
    val rresvport by func { declare(pointer, retType = int32) }
    val rresvport_af by func { if (darwin) declare(pointer, int32, retType = int32) else declare(pointer, int16, retType = int32) }
    val ruserok by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val ruserok_af by func { declare(pointer, int32, pointer, pointer, int16, retType = int32) }
    val sandbox_free_error by func { declare(pointer) }
    val sandbox_init by func { declare(pointer, int64, pointer, retType = int32) }
    val sbrk by func { if (darwin) declare(int32, retType = pointer) else declare(int64, retType = pointer) }
    val scalb by func { declare(float64, float64, retType = float64) }
    val scalbf by func { declare(float32, float32, retType = float32) }
    val scalbln by func { declare(float64, int64, retType = float64) }
    val scalblnf by func { declare(float32, int64, retType = float32) }
    val scalbn by func { declare(float64, int32, retType = float64) }
    val scalbnf by func { declare(float32, int32, retType = float32) }
    val scandir by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val sched_get_priority_max by func { declare(int32, retType = int32) }
    val sched_get_priority_min by func { declare(int32, retType = int32) }
    val sched_getparam by func { declare(int32, pointer, retType = int32) }
    val sched_getscheduler by func { declare(int32, retType = int32) }
    val sched_rr_get_interval by func { declare(int32, pointer, retType = int32) }
    val sched_setparam by func { declare(int32, pointer, retType = int32) }
    val sched_setscheduler by func { declare(int32, int32, pointer, retType = int32) }
    val sched_yield by func { declare(retType = int32) }
    val searchfs by func { declare(pointer, pointer, pointer, int32, int32, pointer, retType = int32) }
    val secure_getenv by func { declare(pointer, retType = pointer) }
    val seed48 by func { declare(pointer, retType = pointer) }
    val seed48_r by func { declare(pointer, pointer, retType = int32) }
    val seekdir by func { declare(pointer, int64) }
    val select by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val sem_clockwait by func { declare(pointer, int32, pointer, retType = int32) }
    val sem_close by func { declare(pointer, retType = int32) }
    val sem_destroy by func { declare(pointer, retType = int32) }
    val sem_getvalue by func { declare(pointer, pointer, retType = int32) }
    val sem_init by func { declare(pointer, int32, int32, retType = int32) }
    val sem_post by func { declare(pointer, retType = int32) }
    val sem_timedwait by func { declare(pointer, pointer, retType = int32) }
    val sem_trywait by func { declare(pointer, retType = int32) }
    val sem_unlink by func { declare(pointer, retType = int32) }
    val sem_wait by func { declare(pointer, retType = int32) }
    val semaphore_create by func { declare(int32, pointer, int32, int32, retType = int32) }
    val semaphore_destroy by func { declare(int32, int32, retType = int32) }
    val semaphore_signal by func { declare(int32, retType = int32) }
    val semaphore_signal_all by func { declare(int32, retType = int32) }
    val semaphore_signal_thread by func { declare(int32, int32, retType = int32) }
    val semaphore_wait by func { declare(int32, retType = int32) }
    val semaphore_wait_signal by func { declare(int32, int32, retType = int32) }
    val semget by func { declare(int32, int32, int32, retType = int32) }
    val semop by func { if (darwin) declare(int32, pointer, int32, retType = int32) else declare(int32, pointer, int64, retType = int32) }
    val send by func { declare(int32, pointer, int64, int32, retType = int64) }
    val sendfile by func { if (darwin) declare(int32, int32, int64, pointer, pointer, int32, retType = int32) else declare(int32, int32, pointer, int64, retType = int64) }
    val sendmsg by func { declare(int32, pointer, int32, retType = int64) }
    val sendto by func { declare(int32, pointer, int64, int32, pointer, int32, retType = int64) }
    val setaliasent by func { declare() }
    val setattrlist by func { declare(pointer, pointer, pointer, int64, int32, retType = int32) }
    val setattrlistat by func { declare(int32, pointer, pointer, pointer, int64, int32, retType = int32) }
    val setaudit by func { declare(pointer, retType = int32) }
    val setaudit_addr by func { declare(pointer, int32, retType = int32) }
    val setauid by func { declare(pointer, retType = int32) }
    val setbuf by func { declare(pointer, pointer) }
    val setbuffer by func { if (darwin) declare(pointer, pointer, int32) else declare(pointer, pointer, int64) }
    val setdomainname by func { if (darwin) declare(pointer, int32, retType = int32) else declare(pointer, int64, retType = int32) }
    val setegid by func { declare(int32, retType = int32) }
    val setenv by func { declare(pointer, pointer, int32, retType = int32) }
    val seteuid by func { declare(int32, retType = int32) }
    val setfsent by func { declare(retType = int32) }
    val setfsgid by func { declare(int32, retType = int32) }
    val setfsuid by func { declare(int32, retType = int32) }
    val setgid by func { declare(int32, retType = int32) }
    val setgrent by func { declare() }
    val setgroupent by func { declare(int32, retType = int32) }
    val setgroups by func { if (darwin) declare(int32, pointer, retType = int32) else declare(int64, pointer, retType = int32) }
    val sethostent by func { declare(int32) }
    val sethostid by func { if (darwin) declare(int64) else declare(int64, retType = int32) }
    val sethostname by func { if (darwin) declare(pointer, int32, retType = int32) else declare(pointer, int64, retType = int32) }
    val setiopolicy_np by func { declare(int32, int32, int32, retType = int32) }
    val setitimer by func { declare(int32, pointer, pointer, retType = int32) }
    val setkey by func { declare(pointer) }
    val setlinebuf by func { if (darwin) declare(pointer, retType = int32) else declare(pointer) }
    val setlocale by func { declare(int32, pointer, retType = pointer) }
    val setlogin by func { declare(pointer, retType = int32) }
    val setlogmask by func { declare(int32, retType = int32) }
    val setmntent by func { declare(pointer, pointer, retType = pointer) }
    val setmode by func { declare(pointer, retType = pointer) }
    val setnetent by func { declare(int32) }
    val setnetgrent by func { if (darwin) declare(pointer) else declare(pointer, retType = int32) }
    val setpassent by func { declare(int32, retType = int32) }
    val setpgid by func { declare(int32, int32, retType = int32) }
    val setpgrp by func { declare(retType = int32) }
    val setpriority by func { declare(int32, int32, int32, retType = int32) }
    val setprogname by func { declare(pointer) }
    val setprotoent by func { declare(int32) }
    val setpwent by func { declare() }
    val setregid by func { declare(int32, int32, retType = int32) }
    val setresgid by func { declare(int32, int32, int32, retType = int32) }
    val setresuid by func { declare(int32, int32, int32, retType = int32) }
    val setreuid by func { declare(int32, int32, retType = int32) }
    val setrgid by func { declare(int32, retType = int32) }
    val setrlimit by func { declare(int32, pointer, retType = int32) }
    val setrpcent by func { declare(int32) }
    val setruid by func { declare(int32, retType = int32) }
    val setservent by func { declare(int32) }
    val setsgent by func { declare() }
    val setsgroups_np by func { declare(int32, pointer, retType = int32) }
    val setsid by func { declare(retType = int32) }
    val setsockopt by func { declare(int32, int32, int32, pointer, int32, retType = int32) }
    val setstate by func { declare(pointer, retType = pointer) }
    val setstate_r by func { declare(pointer, pointer, retType = int32) }
    val settimeofday by func { declare(pointer, pointer, retType = int32) }
    val setttyent by func { declare(retType = int32) }
    val setuid by func { declare(int32, retType = int32) }
    val setusershell by func { declare() }
    val setutent by func { declare() }
    val setutxent by func { declare() }
    val setutxent_wtmp by func { declare(int32) }
    val setvbuf by func { declare(pointer, pointer, int32, int64, retType = int32) }
    val setwgroups_np by func { declare(int32, pointer, retType = int32) }
    val sgetsgent by func { declare(pointer, retType = pointer) }
    val sgetsgent_r by func { declare(pointer, pointer, pointer, int64, pointer, retType = int32) }
    val shm_unlink by func { declare(pointer, retType = int32) }
    val shmat by func { declare(int32, pointer, int32, retType = pointer) }
    val shmctl by func { declare(int32, int32, pointer, retType = int32) }
    val shmdt by func { declare(pointer, retType = int32) }
    val shmget by func { declare(int32, int64, int32, retType = int32) }
    val shutdown by func { declare(int32, int32, retType = int32) }
    val sigaction by func { declare(int32, pointer, pointer, retType = int32) }
    val sigaddset by func { declare(pointer, int32, retType = int32) }
    val sigaltstack by func { declare(pointer, pointer, retType = int32) }
    val sigblock by func { declare(int32, retType = int32) }
    val sigdelset by func { declare(pointer, int32, retType = int32) }
    val sigemptyset by func { declare(pointer, retType = int32) }
    val sigfillset by func { declare(pointer, retType = int32) }
    val siggetmask by func { declare(retType = int32) }
    val sighold by func { declare(int32, retType = int32) }
    val sigignore by func { declare(int32, retType = int32) }
    val siginterrupt by func { declare(int32, int32, retType = int32) }
    val sigismember by func { declare(pointer, int32, retType = int32) }
    val signal by func { declare(int32, pointer, retType = pointer) }
    val signalfd by func { declare(int32, pointer, int32, retType = int32) }
    val significand by func { declare(float64, retType = float64) }
    val significandf by func { declare(float32, retType = float32) }
    val sigpause by func { declare(int32, retType = int32) }
    val sigpending by func { declare(pointer, retType = int32) }
    val sigprocmask by func { declare(int32, pointer, pointer, retType = int32) }
    val sigqueue by func { declare(int32, int32, PosixType.SIGVAL, retType = int32) }
    val sigrelse by func { declare(int32, retType = int32) }
    val sigreturn by func { declare(pointer, retType = int32) }
    val sigset by func { declare(int32, pointer, retType = pointer) }
    val sigsetmask by func { declare(int32, retType = int32) }
    val sigstack by func { declare(pointer, pointer, retType = int32) }
    val sigsuspend by func { declare(pointer, retType = int32) }
    val sigtimedwait by func { declare(pointer, pointer, pointer, retType = int32) }
    val sigvec by func { declare(int32, pointer, pointer, retType = int32) }
    val sigwait by func { declare(pointer, pointer, retType = int32) }
    val sigwaitinfo by func { declare(pointer, pointer, retType = int32) }
    val sin by func { declare(float64, retType = float64) }
    val sinf by func { declare(float32, retType = float32) }
    val sinh by func { declare(float64, retType = float64) }
    val sinhf by func { declare(float32, retType = float32) }
    val sl_add by func { declare(pointer, pointer, retType = int32) }
    val sl_find by func { declare(pointer, pointer, retType = pointer) }
    val sl_free by func { declare(pointer, int32) }
    val sl_init by func { declare(retType = pointer) }
    val sleep by func { declare(int32, retType = int32) }
    val slot_name by func { declare(int32, int32, pointer, pointer) }
    val snvis by func { declare(pointer, int64, int32, int32, int32, pointer, retType = pointer) }
    val sockatmark by func { declare(int32, retType = int32) }
    val socket by func { declare(int32, int32, int32, retType = int32) }
    val socketpair by func { declare(int32, int32, int32, pointer, retType = int32) }
    val sprofil by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val sqrt by func { declare(float64, retType = float64) }
    val sqrtf by func { declare(float32, retType = float32) }
    val sradixsort by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val srand by func { declare(int32) }
    val srand48 by func { declare(int64) }
    val srand48_r by func { declare(int64, pointer, retType = int32) }
    val sranddev by func { declare() }
    val srandom by func { declare(int32) }
    val srandom_r by func { declare(int32, pointer, retType = int32) }
    val srandomdev by func { declare() }
    val stat by func { declare(pointer, pointer, retType = int32) }
    val statvfs by func { declare(pointer, pointer, retType = int32) }
    val statx by func { declare(int32, pointer, int32, int32, pointer, retType = int32) }
    val statx_np by func { declare(pointer, pointer, pointer, retType = int32) }
    val stpcpy by func { declare(pointer, pointer, retType = pointer) }
    val stpncpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val strcasecmp by func { declare(pointer, pointer, retType = int32) }
    val strcasecmp_l by func { declare(pointer, pointer, pointer, retType = int32) }
    val strcasestr_l by func { declare(pointer, pointer, pointer, retType = pointer) }
    val strcat by func { declare(pointer, pointer, retType = pointer) }
    val strchr by func { declare(pointer, int32, retType = pointer) }
    val strcmp by func { declare(pointer, pointer, retType = int32) }
    val strcoll by func { declare(pointer, pointer, retType = int32) }
    val strcoll_l by func { declare(pointer, pointer, pointer, retType = int32) }
    val strcpy by func { declare(pointer, pointer, retType = pointer) }
    val strcspn by func { declare(pointer, pointer, retType = int64) }
    val strdup by func { declare(pointer, retType = pointer) }
    val strenvisx by func { declare(pointer, int64, pointer, int64, int32, pointer, retType = int32) }
    val strerror by func { declare(int32, retType = pointer) }
    val strerror_l by func { declare(int32, pointer, retType = pointer) }
    val strerror_r by func { declare(int32, pointer, int64, retType = int32) }
    val strftime by func { declare(pointer, int64, pointer, pointer, retType = int64) }
    val strftime_l by func { declare(pointer, int64, pointer, pointer, pointer, retType = int64) }
    val strlcat by func { declare(pointer, pointer, int64, retType = int64) }
    val strlcpy by func { declare(pointer, pointer, int64, retType = int64) }
    val strlen by func { declare(pointer, retType = int64) }
    val strmode by func { declare(int32, pointer) }
    val strncasecmp by func { declare(pointer, pointer, int64, retType = int32) }
    val strncasecmp_l by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val strncat by func { declare(pointer, pointer, int64, retType = pointer) }
    val strncmp by func { declare(pointer, pointer, int64, retType = int32) }
    val strncpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val strndup by func { declare(pointer, int64, retType = pointer) }
    val strnlen by func { declare(pointer, int64, retType = int64) }
    val strnstr by func { declare(pointer, pointer, int64, retType = pointer) }
    val strnunvis by func { declare(pointer, int64, pointer, retType = int32) }
    val strnunvisx by func { declare(pointer, int64, pointer, int32, retType = int32) }
    val strnvis by func { declare(pointer, int64, pointer, int32, retType = int32) }
    val strnvisx by func { declare(pointer, int64, pointer, int64, int32, retType = int32) }
    val strpbrk by func { declare(pointer, pointer, retType = pointer) }
    val strptime by func { declare(pointer, pointer, pointer, retType = pointer) }
    val strrchr by func { declare(pointer, int32, retType = pointer) }
    val strsenvisx by func { declare(pointer, int64, pointer, int64, int32, pointer, pointer, retType = int32) }
    val strsep by func { declare(pointer, pointer, retType = pointer) }
    val strsignal by func { declare(int32, retType = pointer) }
    val strsignal_r by func { declare(int32, pointer, int64, retType = int32) }
    val strsnvis by func { declare(pointer, int64, pointer, int32, pointer, retType = int32) }
    val strsnvisx by func { declare(pointer, int64, pointer, int64, int32, pointer, retType = int32) }
    val strspn by func { declare(pointer, pointer, retType = int64) }
    val strstr by func { declare(pointer, pointer, retType = pointer) }
    val strsvis by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val strsvisx by func { declare(pointer, pointer, int64, int32, pointer, retType = int32) }
    val strtod by func { declare(pointer, pointer, retType = float64) }
    val strtof by func { declare(pointer, pointer, retType = float32) }
    val strtofflags by func { declare(pointer, pointer, pointer, retType = int32) }
    val strtoimax by func { declare(pointer, pointer, int32, retType = int64) }
    val strtoimax_l by func { declare(pointer, pointer, int32, pointer, retType = int64) }
    val strtok by func { declare(pointer, pointer, retType = pointer) }
    val strtok_r by func { declare(pointer, pointer, pointer, retType = pointer) }
    val strtol by func { declare(pointer, pointer, int32, retType = int64) }
    val strtoll by func { declare(pointer, pointer, int32, retType = int64) }
    val strtonum by func { declare(pointer, int64, int64, pointer, retType = int64) }
    val strtoq by func { declare(pointer, pointer, int32, retType = int64) }
    val strtoq_l by func { declare(pointer, pointer, int32, pointer, retType = int64) }
    val strtoul by func { declare(pointer, pointer, int32, retType = int64) }
    val strtoull by func { declare(pointer, pointer, int32, retType = int64) }
    val strtoumax by func { declare(pointer, pointer, int32, retType = int64) }
    val strtoumax_l by func { declare(pointer, pointer, int32, pointer, retType = int64) }
    val strtouq by func { declare(pointer, pointer, int32, retType = int64) }
    val strtouq_l by func { declare(pointer, pointer, int32, pointer, retType = int64) }
    val strunvis by func { declare(pointer, pointer, retType = int32) }
    val strunvisx by func { declare(pointer, pointer, int32, retType = int32) }
    val strvis by func { declare(pointer, pointer, int32, retType = int32) }
    val strvisx by func { declare(pointer, pointer, int64, int32, retType = int32) }
    val strxfrm by func { declare(pointer, pointer, int64, retType = int64) }
    val strxfrm_l by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val stty by func { declare(int32, pointer, retType = int32) }
    val svis by func { declare(pointer, int32, int32, int32, pointer, retType = pointer) }
    val swab by func { declare(pointer, pointer, int64) }
    val swapoff by func { declare(pointer, retType = int32) }
    val swapon by func { if (darwin) declare(pointer, retType = int32) else declare(pointer, int32, retType = int32) }
    val swtch by func { declare(retType = int32) }
    val swtch_pri by func { declare(int32, retType = int32) }
    val symlink by func { declare(pointer, pointer, retType = int32) }
    val symlinkat by func { declare(pointer, int32, pointer, retType = int32) }
    val sync by func { declare() }
    val sync_volume_np by func { declare(pointer, int32, retType = int32) }
    val syncfs by func { declare(int32, retType = int32) }
    val sysconf by func { declare(int32, retType = int64) }
    val sysctl by func { declare(pointer, int32, pointer, pointer, pointer, int64, retType = int32) }
    val sysctlbyname by func { declare(pointer, pointer, pointer, pointer, int64, retType = int32) }
    val sysctlnametomib by func { declare(pointer, pointer, pointer, retType = int32) }
    val sysdir_get_next_search_path_enumeration by func { declare(int32, pointer, retType = int32) }
    val sysdir_start_search_path_enumeration by func { declare(int32, int32, retType = int32) }
    val sysinfo by func { declare(pointer, retType = int32) }
    val system by func { declare(pointer, retType = int32) }
    val tan by func { declare(float64, retType = float64) }
    val tanf by func { declare(float32, retType = float32) }
    val tanh by func { declare(float64, retType = float64) }
    val tanhf by func { declare(float32, retType = float32) }
    val task_assign by func { declare(int32, int32, int32, retType = int32) }
    val task_assign_default by func { declare(int32, int32, retType = int32) }
    val task_create by func { declare(int32, pointer, int32, int32, pointer, retType = int32) }
    val task_create_identity_token by func { declare(int32, pointer, retType = int32) }
    val task_dyld_process_info_notify_deregister by func { declare(int32, int32, retType = int32) }
    val task_dyld_process_info_notify_get by func { declare(pointer, pointer, retType = int32) }
    val task_dyld_process_info_notify_register by func { declare(int32, int32, retType = int32) }
    val task_for_pid by func { declare(int32, int32, pointer, retType = int32) }
    val task_generate_corpse by func { declare(int32, pointer, retType = int32) }
    val task_get_assignment by func { declare(int32, pointer, retType = int32) }
    val task_get_dyld_image_infos by func { declare(int32, pointer, pointer, retType = int32) }
    val task_get_emulation_vector by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val task_get_exc_guard_behavior by func { declare(int32, pointer, retType = int32) }
    val task_get_exception_ports by func { declare(int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val task_get_exception_ports_info by func { declare(int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val task_get_mach_voucher by func { declare(int32, int32, pointer, retType = int32) }
    val task_get_special_port by func { declare(int32, int32, pointer, retType = int32) }
    val task_get_state by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val task_identity_token_get_task_port by func { declare(int32, int32, pointer, retType = int32) }
    val task_info by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val task_inspect by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val task_map_corpse_info by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val task_map_corpse_info_64 by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val task_name_for_pid by func { declare(int32, int32, pointer, retType = int32) }
    val task_policy by func { declare(int32, int32, pointer, int32, int32, int32, retType = int32) }
    val task_policy_get by func { declare(int32, int32, pointer, pointer, pointer, retType = int32) }
    val task_policy_set by func { declare(int32, int32, pointer, int32, retType = int32) }
    val task_purgable_info by func { declare(int32, pointer, retType = int32) }
    val task_register_dyld_get_process_state by func { declare(int32, pointer, retType = int32) }
    val task_register_dyld_image_infos by func { declare(int32, pointer, int32, retType = int32) }
    val task_register_dyld_set_dyld_state by func { declare(int32, byte, retType = int32) }
    val task_resume by func { declare(int32, retType = int32) }
    val task_resume2 by func { declare(int32, retType = int32) }
    val task_sample by func { declare(int32, int32, retType = int32) }
    val task_self_trap by func { declare(retType = int32) }
    val task_set_emulation by func { declare(int32, int64, int32, retType = int32) }
    val task_set_emulation_vector by func { declare(int32, int32, pointer, int32, retType = int32) }
    val task_set_exc_guard_behavior by func { declare(int32, int32, retType = int32) }
    val task_set_exception_ports by func { declare(int32, int32, int32, int32, int32, retType = int32) }
    val task_set_info by func { declare(int32, int32, pointer, int32, retType = int32) }
    val task_set_mach_voucher by func { declare(int32, int32, retType = int32) }
    val task_set_phys_footprint_limit by func { declare(int32, int32, pointer, retType = int32) }
    val task_set_policy by func { declare(int32, int32, int32, pointer, int32, pointer, int32, int32, retType = int32) }
    val task_set_port_space by func { declare(int32, int32, retType = int32) }
    val task_set_ras_pc by func { declare(int32, int64, int64, retType = int32) }
    val task_set_special_port by func { declare(int32, int32, int32, retType = int32) }
    val task_set_state by func { declare(int32, int32, pointer, int32, retType = int32) }
    val task_suspend by func { declare(int32, retType = int32) }
    val task_suspend2 by func { declare(int32, pointer, retType = int32) }
    val task_swap_exception_ports by func { declare(int32, int32, int32, int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val task_swap_mach_voucher by func { declare(int32, int32, pointer, retType = int32) }
    val task_terminate by func { declare(int32, retType = int32) }
    val task_threads by func { declare(int32, pointer, pointer, retType = int32) }
    val task_unregister_dyld_image_infos by func { declare(int32, pointer, int32, retType = int32) }
    val task_zone_info by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val tcdrain by func { declare(int32, retType = int32) }
    val tcflow by func { declare(int32, int32, retType = int32) }
    val tcflush by func { declare(int32, int32, retType = int32) }
    val tcgetattr by func { declare(int32, pointer, retType = int32) }
    val tcgetpgrp by func { declare(int32, retType = int32) }
    val tcgetsid by func { declare(int32, retType = int32) }
    val tcsendbreak by func { declare(int32, int32, retType = int32) }
    val tcsetattr by func { declare(int32, int32, pointer, retType = int32) }
    val tcsetpgrp by func { declare(int32, int32, retType = int32) }
    val tdelete by func { declare(pointer, pointer, pointer, retType = pointer) }
    val telldir by func { declare(pointer, retType = int64) }
    val tempnam by func { declare(pointer, pointer, retType = pointer) }
    val textdomain by func { declare(pointer, retType = pointer) }
    val tfind by func { declare(pointer, pointer, pointer, retType = pointer) }
    val tgamma by func { declare(float64, retType = float64) }
    val tgammaf by func { declare(float32, retType = float32) }
    val thrd_create by func { declare(pointer, pointer, pointer, retType = int32) }
    val thrd_current by func { declare(retType = int64) }
    val thrd_detach by func { declare(int64, retType = int32) }
    val thrd_equal by func { declare(int64, int64, retType = int32) }
    val thrd_join by func { declare(int64, pointer, retType = int32) }
    val thrd_sleep by func { declare(pointer, pointer, retType = int32) }
    val thrd_yield by func { declare() }
    val thread_abort by func { declare(int32, retType = int32) }
    val thread_abort_safely by func { declare(int32, retType = int32) }
    val thread_assign by func { declare(int32, int32, retType = int32) }
    val thread_assign_default by func { declare(int32, retType = int32) }
    val thread_convert_thread_state by func { declare(int32, int32, int32, pointer, int32, pointer, pointer, retType = int32) }
    val thread_create by func { declare(int32, pointer, retType = int32) }
    val thread_create_running by func { declare(int32, int32, pointer, int32, pointer, retType = int32) }
    val thread_depress_abort by func { declare(int32, retType = int32) }
    val thread_get_assignment by func { declare(int32, pointer, retType = int32) }
    val thread_get_exception_ports by func { declare(int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val thread_get_exception_ports_info by func { declare(int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val thread_get_mach_voucher by func { declare(int32, int32, pointer, retType = int32) }
    val thread_get_special_port by func { declare(int32, int32, pointer, retType = int32) }
    val thread_get_state by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val thread_info by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val thread_policy by func { declare(int32, int32, pointer, int32, int32, retType = int32) }
    val thread_policy_get by func { declare(int32, int32, pointer, pointer, pointer, retType = int32) }
    val thread_policy_set by func { declare(int32, int32, pointer, int32, retType = int32) }
    val thread_resume by func { declare(int32, retType = int32) }
    val thread_sample by func { declare(int32, int32, retType = int32) }
    val thread_set_exception_ports by func { declare(int32, int32, int32, int32, int32, retType = int32) }
    val thread_set_mach_voucher by func { declare(int32, int32, retType = int32) }
    val thread_set_policy by func { declare(int32, int32, int32, pointer, int32, pointer, int32, retType = int32) }
    val thread_set_special_port by func { declare(int32, int32, int32, retType = int32) }
    val thread_set_state by func { declare(int32, int32, pointer, int32, retType = int32) }
    val thread_suspend by func { declare(int32, retType = int32) }
    val thread_swap_exception_ports by func { declare(int32, int32, int32, int32, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val thread_swap_mach_voucher by func { declare(int32, int32, pointer, retType = int32) }
    val thread_switch by func { declare(int32, int32, int32, retType = int32) }
    val thread_terminate by func { declare(int32, retType = int32) }
    val thread_wire by func { declare(int32, int32, int32, retType = int32) }
    val time by func { declare(pointer, retType = int64) }
    val time2posix by func { declare(int64, retType = int64) }
    val timegm by func { declare(pointer, retType = int64) }
    val timelocal by func { declare(pointer, retType = int64) }
    val timer_create by func { declare(int32, pointer, pointer, retType = int32) }
    val timer_delete by func { declare(pointer, retType = int32) }
    val timer_getoverrun by func { declare(pointer, retType = int32) }
    val timer_gettime by func { declare(pointer, pointer, retType = int32) }
    val timer_settime by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val timerfd_create by func { declare(int32, int32, retType = int32) }
    val timerfd_gettime by func { declare(int32, pointer, retType = int32) }
    val timerfd_settime by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val times by func { declare(pointer, retType = int64) }
    val timespec_get by func { declare(pointer, int32, retType = int32) }
    val timingsafe_bcmp by func { declare(pointer, pointer, int64, retType = int32) }
    val tmpfile by func { declare(retType = pointer) }
    val tmpnam by func { declare(pointer, retType = pointer) }
    val tmpnam_r by func { declare(pointer, retType = pointer) }
    val toascii by func { declare(int32, retType = int32) }
    val tolower by func { declare(int32, retType = int32) }
    val tolower_l by func { declare(int32, pointer, retType = int32) }
    val toupper by func { declare(int32, retType = int32) }
    val toupper_l by func { declare(int32, pointer, retType = int32) }
    val towctrans by func { if (darwin) declare(int32, int32, retType = int32) else declare(int32, pointer, retType = int32) }
    val towctrans_l by func { if (darwin) declare(int32, int32, pointer, retType = int32) else declare(int32, pointer, pointer, retType = int32) }
    val towlower by func { declare(int32, retType = int32) }
    val towlower_l by func { declare(int32, pointer, retType = int32) }
    val towupper by func { declare(int32, retType = int32) }
    val towupper_l by func { declare(int32, pointer, retType = int32) }
    val trunc by func { declare(float64, retType = float64) }
    val truncate by func { declare(pointer, int64, retType = int32) }
    val truncf by func { declare(float32, retType = float32) }
    val tsearch by func { declare(pointer, pointer, pointer, retType = pointer) }
    val tss_create by func { declare(pointer, pointer, retType = int32) }
    val tss_delete by func { declare(int32) }
    val tss_get by func { declare(int32, retType = pointer) }
    val tss_set by func { declare(int32, pointer, retType = int32) }
    val ttyname by func { declare(int32, retType = pointer) }
    val ttyname_r by func { declare(int32, pointer, int64, retType = int32) }
    val ttyslot by func { declare(retType = int32) }
    val twalk by func { declare(pointer, pointer) }
    val tzset by func { declare() }
    val tzsetwall by func { declare() }
    val ualarm by func { declare(int32, int32, retType = int32) }
    val umask by func { if (darwin) declare(int16, retType = int16) else declare(int32, retType = int32) }
    val umaskx_np by func { declare(pointer, retType = int32) }
    val umount by func { declare(pointer, retType = int32) }
    val umount2 by func { declare(pointer, int32, retType = int32) }
    val uname by func { declare(pointer, retType = int32) }
    val undelete by func { declare(pointer, retType = int32) }
    val ungetc by func { declare(int32, pointer, retType = int32) }
    val ungetwc by func { declare(int32, pointer, retType = int32) }
    val ungetwc_l by func { declare(int32, pointer, pointer, retType = int32) }
    val unlink by func { declare(pointer, retType = int32) }
    val unlinkat by func { declare(int32, pointer, int32, retType = int32) }
    val unlockpt by func { declare(int32, retType = int32) }
    val unsetenv by func { declare(pointer, retType = int32) }
    val unvis by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val unw_get_fpreg by func { declare(pointer, int32, pointer, retType = int32) }
    val unw_get_proc_info by func { declare(pointer, pointer, retType = int32) }
    val unw_get_proc_name by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val unw_get_reg by func { declare(pointer, int32, pointer, retType = int32) }
    val unw_getcontext by func { declare(pointer, retType = int32) }
    val unw_init_local by func { declare(pointer, pointer, retType = int32) }
    val unw_is_fpreg by func { declare(pointer, int32, retType = int32) }
    val unw_is_signal_frame by func { declare(pointer, retType = int32) }
    val unw_regname by func { declare(pointer, int32, retType = pointer) }
    val unw_resume by func { declare(pointer, retType = int32) }
    val unw_set_fpreg by func { declare(pointer, int32, float64, retType = int32) }
    val unw_set_reg by func { declare(pointer, int32, int64, retType = int32) }
    val unw_step by func { declare(pointer, retType = int32) }
    val updwtmp by func { declare(pointer, pointer) }
    val uselocale by func { declare(pointer, retType = pointer) }
    val user_from_uid by func { declare(int32, int32, retType = pointer) }
    val usleep by func { declare(int32, retType = int32) }
    val utime by func { declare(pointer, pointer, retType = int32) }
    val utimensat by func { declare(int32, pointer, pointer, int32, retType = int32) }
    val utimes by func { declare(pointer, pointer, retType = int32) }
    val utmpname by func { declare(pointer, retType = int32) }
    val uuid_clear by func { declare(pointer) }
    val uuid_compare by func { declare(pointer, pointer, retType = int32) }
    val uuid_copy by func { declare(pointer, pointer) }
    val uuid_generate by func { declare(pointer) }
    val uuid_generate_random by func { declare(pointer) }
    val uuid_generate_time by func { declare(pointer) }
    val uuid_is_null by func { declare(pointer, retType = int32) }
    val uuid_parse by func { declare(pointer, pointer, retType = int32) }
    val uuid_unparse by func { declare(pointer, pointer) }
    val uuid_unparse_lower by func { declare(pointer, pointer) }
    val uuid_unparse_upper by func { declare(pointer, pointer) }
    val valloc by func { declare(int64, retType = pointer) }
    val vhangup by func { declare(retType = int32) }
    val vis by func { declare(pointer, int32, int32, int32, retType = pointer) }
    val vm_allocate by func { declare(int32, pointer, int64, int32, retType = int32) }
    val vm_allocate_cpm by func { declare(int32, int32, pointer, int64, int32, retType = int32) }
    val vm_behavior_set by func { declare(int32, int64, int64, int32, retType = int32) }
    val vm_copy by func { declare(int32, int64, int64, int64, retType = int32) }
    val vm_deallocate by func { declare(int32, int64, int64, retType = int32) }
    val vm_inherit by func { declare(int32, int64, int64, int32, retType = int32) }
    val vm_machine_attribute by func { declare(int32, int64, int64, int32, pointer, retType = int32) }
    val vm_map by func { declare(int32, pointer, int64, int64, int32, int32, int64, int32, int32, int32, int32, retType = int32) }
    val vm_map_page_query by func { declare(int32, int64, pointer, pointer, retType = int32) }
    val vm_msync by func { declare(int32, int64, int64, int32, retType = int32) }
    val vm_protect by func { declare(int32, int64, int64, int32, int32, retType = int32) }
    val vm_purgable_control by func { declare(int32, int64, int32, pointer, retType = int32) }
    val vm_read by func { declare(int32, int64, int64, pointer, pointer, retType = int32) }
    val vm_read_list by func { declare(int32, pointer, int32, retType = int32) }
    val vm_read_overwrite by func { declare(int32, int64, int64, int64, pointer, retType = int32) }
    val vm_region_64 by func { declare(int32, pointer, pointer, int32, pointer, pointer, pointer, retType = int32) }
    val vm_region_recurse_64 by func { declare(int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val vm_remap by func { declare(int32, pointer, int64, int64, int32, int32, int64, int32, pointer, pointer, int32, retType = int32) }
    val vm_remap_new by func { declare(int32, pointer, int64, int64, int32, int32, int64, int32, pointer, pointer, int32, retType = int32) }
    val vm_wire by func { declare(int32, int32, int64, int64, int32, retType = int32) }
    val vm_write by func { declare(int32, int64, int64, int32, retType = int32) }
    val voucher_mach_msg_adopt by func { declare(pointer, retType = pointer) }
    val voucher_mach_msg_clear by func { declare(pointer) }
    val voucher_mach_msg_revert by func { declare(pointer) }
    val voucher_mach_msg_set by func { declare(pointer, retType = int32) }
    val wait by func { declare(pointer, retType = int32) }
    val wait3 by func { declare(pointer, int32, pointer, retType = int32) }
    val wait4 by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val waitid by func { declare(int32, int32, pointer, int32, retType = int32) }
    val waitpid by func { declare(int32, pointer, int32, retType = int32) }
    val wcpcpy by func { declare(pointer, pointer, retType = pointer) }
    val wcpncpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val wcrtomb by func { declare(pointer, int32, pointer, retType = int64) }
    val wcrtomb_l by func { declare(pointer, int32, pointer, pointer, retType = int64) }
    val wcscasecmp by func { declare(pointer, pointer, retType = int32) }
    val wcscasecmp_l by func { declare(pointer, pointer, pointer, retType = int32) }
    val wcscat by func { declare(pointer, pointer, retType = pointer) }
    val wcschr by func { declare(pointer, int32, retType = pointer) }
    val wcscmp by func { declare(pointer, pointer, retType = int32) }
    val wcscoll by func { declare(pointer, pointer, retType = int32) }
    val wcscoll_l by func { declare(pointer, pointer, pointer, retType = int32) }
    val wcscpy by func { declare(pointer, pointer, retType = pointer) }
    val wcscspn by func { declare(pointer, pointer, retType = int64) }
    val wcsdup by func { declare(pointer, retType = pointer) }
    val wcsftime by func { declare(pointer, int64, pointer, pointer, retType = int64) }
    val wcslcat by func { declare(pointer, pointer, int64, retType = int64) }
    val wcslcpy by func { declare(pointer, pointer, int64, retType = int64) }
    val wcslen by func { declare(pointer, retType = int64) }
    val wcsncasecmp by func { declare(pointer, pointer, int64, retType = int32) }
    val wcsncasecmp_l by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val wcsncat by func { declare(pointer, pointer, int64, retType = pointer) }
    val wcsncmp by func { declare(pointer, pointer, int64, retType = int32) }
    val wcsncpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val wcsnlen by func { declare(pointer, int64, retType = int64) }
    val wcsnrtombs by func { declare(pointer, pointer, int64, int64, pointer, retType = int64) }
    val wcsnrtombs_l by func { declare(pointer, pointer, int64, int64, pointer, pointer, retType = int64) }
    val wcspbrk by func { declare(pointer, pointer, retType = pointer) }
    val wcsrchr by func { declare(pointer, int32, retType = pointer) }
    val wcsrtombs by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val wcsrtombs_l by func { declare(pointer, pointer, int64, pointer, pointer, retType = int64) }
    val wcsspn by func { declare(pointer, pointer, retType = int64) }
    val wcsstr by func { declare(pointer, pointer, retType = pointer) }
    val wcstod by func { declare(pointer, pointer, retType = float64) }
    val wcstof by func { declare(pointer, pointer, retType = float32) }
    val wcstoimax by func { declare(pointer, pointer, int32, retType = int64) }
    val wcstoimax_l by func { declare(pointer, pointer, int32, pointer, retType = int64) }
    val wcstok by func { declare(pointer, pointer, pointer, retType = pointer) }
    val wcstol by func { declare(pointer, pointer, int32, retType = int64) }
    val wcstoll by func { declare(pointer, pointer, int32, retType = int64) }
    val wcstombs by func { declare(pointer, pointer, int64, retType = int64) }
    val wcstombs_l by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val wcstoul by func { declare(pointer, pointer, int32, retType = int64) }
    val wcstoull by func { declare(pointer, pointer, int32, retType = int64) }
    val wcstoumax by func { declare(pointer, pointer, int32, retType = int64) }
    val wcstoumax_l by func { declare(pointer, pointer, int32, pointer, retType = int64) }
    val wcswcs by func { declare(pointer, pointer, retType = pointer) }
    val wcswidth by func { declare(pointer, int64, retType = int32) }
    val wcswidth_l by func { declare(pointer, int64, pointer, retType = int32) }
    val wcsxfrm by func { declare(pointer, pointer, int64, retType = int64) }
    val wcsxfrm_l by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val wctob by func { declare(int32, retType = int32) }
    val wctob_l by func { declare(int32, pointer, retType = int32) }
    val wctomb by func { declare(pointer, int32, retType = int32) }
    val wctomb_l by func { declare(pointer, int32, pointer, retType = int32) }
    val wctrans by func { if (darwin) declare(pointer, retType = int32) else declare(pointer, retType = pointer) }
    val wctrans_l by func { if (darwin) declare(pointer, pointer, retType = int32) else declare(pointer, pointer, retType = pointer) }
    val wctype by func { if (darwin) declare(pointer, retType = int32) else declare(pointer, retType = int64) }
    val wctype_l by func { if (darwin) declare(pointer, pointer, retType = int32) else declare(pointer, pointer, retType = int64) }
    val wcwidth by func { declare(int32, retType = int32) }
    val wcwidth_l by func { declare(int32, pointer, retType = int32) }
    val wmemchr by func { declare(pointer, int32, int64, retType = pointer) }
    val wmemcmp by func { declare(pointer, pointer, int64, retType = int32) }
    val wmemcpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val wmemmove by func { declare(pointer, pointer, int64, retType = pointer) }
    val wmemset by func { declare(pointer, int32, int64, retType = pointer) }
    val wordexp by func { declare(pointer, pointer, int32, retType = int32) }
    val wordfree by func { declare(pointer) }
    val write by func { declare(int32, pointer, int64, retType = int64) }
    val writev by func { declare(int32, pointer, int32, retType = int64) }
    val wtmpxname by func { declare(pointer, retType = int32) }
    val xattr_flags_from_name by func { declare(pointer, retType = int64) }
    val xattr_intent_with_flags by func { declare(int32, int64, retType = int32) }
    val xattr_name_with_flags by func { declare(pointer, int64, retType = pointer) }
    val xattr_name_without_flags by func { declare(pointer, retType = pointer) }
    val xattr_preserve_for_intent by func { declare(pointer, int32, retType = int32) }
    val xpc_activity_unregister by func { declare(pointer) }
    val xpc_main by func { declare(pointer) }
    val xpc_transaction_begin by func { declare() }
    val xpc_transaction_end by func { declare() }
    val xpc_type_get_name by func { declare(pointer, retType = pointer) }
    val y0 by func { declare(float64, retType = float64) }
    val y0f by func { declare(float32, retType = float32) }
    val y1 by func { declare(float64, retType = float64) }
    val y1f by func { declare(float32, retType = float32) }
    val yn by func { declare(int32, float64, retType = float64) }
    val ynf by func { declare(int32, float32, retType = float32) }
}
