/*
 * Tencent is pleased to support the open source community by making
 * MMKV available.
 *
 * Copyright (C) 2018 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *       https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "CodedInputData.h"
#include "CodedOutputData.h"
#include "InterProcessLock.h"
#include "KeyValueHolder.h"
#include "MMBuffer.h"
#include "MMKVLog.h"
#include "MMKVMetaInfo.hpp"
#include "MMKV_IO.h"
#include "MMKV_OSX.h"
#include "MemoryFile.h"
#include "MiniPBCoder.h"
#include "PBUtility.h"
#include "ScopedLock.hpp"
#include "ThreadLock.h"
#include "aes/AESCrypt.h"
#include "aes/openssl/openssl_aes.h"
#include "aes/openssl/openssl_md5.h"
#include "crc32/Checksum.h"
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <unordered_set>
#include <cassert>

#if defined(__aarch64__) && defined(__linux__) && !defined (MMKV_OHOS)
#    include <asm/hwcap.h>
#    include <sys/auxv.h>
#endif

#ifdef MMKV_APPLE
#    if __has_feature(objc_arc)
#        error This file must be compiled with MRC. Use -fno-objc-arc flag.
#    endif
#    include "MMKV_OSX.h"
#endif // MMKV_APPLE

using namespace std;
using namespace mmkv;

unordered_map<string, MMKV *> *g_instanceDic;
ThreadLock *g_instanceLock;
MMKVPath_t g_rootDir;
MMKVPath_t g_realRootDir;
static ThreadLock *g_namespaceLock;
static unordered_map<MMKVPath_t, MMKVPath_t> g_realRootMap;
static mmkv::ErrorHandler g_errorHandler;
size_t mmkv::DEFAULT_MMAP_SIZE;

#ifndef MMKV_WIN32
constexpr auto SPECIAL_CHARACTER_DIRECTORY_NAME = "specialCharacter";
constexpr auto CRC_SUFFIX = ".crc";
#else
constexpr auto SPECIAL_CHARACTER_DIRECTORY_NAME = L"specialCharacter";
constexpr auto CRC_SUFFIX = L".crc";
#endif

MMKV_NAMESPACE_BEGIN

static MMKVPath_t encodeFilePath(const string &mmapID, const MMKVPath_t &rootDir);
bool endsWith(const MMKVPath_t &str, const MMKVPath_t &suffix);
MMKVPath_t filename(const MMKVPath_t &path);

#ifndef MMKV_ANDROID
MMKV::MMKV(const string &mmapID, MMKVMode mode, const string *cryptKey, const MMKVPath_t *rootPath, size_t expectedCapacity)
    : m_mmapID(mmapID)
    , m_mode(mode)
    , m_path(mappedKVPathWithID(m_mmapID, rootPath))
    , m_crcPath(crcPathWithPath(m_path))
    , m_dic(nullptr)
    , m_dicCrypt(nullptr)
    , m_expectedCapacity(std::max<size_t>(DEFAULT_MMAP_SIZE, roundUp<size_t>(expectedCapacity, DEFAULT_MMAP_SIZE)))
    , m_file(new MemoryFile(m_path, m_expectedCapacity, isReadOnly(), true))
    , m_metaFile(new MemoryFile(m_crcPath, 0, isReadOnly(), !isMultiProcess()))
    , m_metaInfo(new MMKVMetaInfo())
    , m_crypter(nullptr)
    , m_lock(new ThreadLock())
    , m_fileLock(new FileLock(isMultiProcess() ? m_metaFile->getFd() : MMKVFileHandleInvalidValue))
    , m_sharedProcessLock(new InterProcessLock(m_fileLock, SharedLockType))
    , m_exclusiveProcessLock(new InterProcessLock(m_fileLock, ExclusiveLockType))
{
    m_actualSize = 0;
    m_output = nullptr;

#    ifndef MMKV_DISABLE_CRYPT
    if (cryptKey && !cryptKey->empty()) {
        m_dicCrypt = new MMKVMapCrypt();
        m_crypter = new AESCrypt(cryptKey->data(), cryptKey->length());
    } else {
        m_dic = new MMKVMap();
    }
#    else
    m_dic = new MMKVMap();
#    endif

    m_needLoadFromFile = true;
    m_hasFullWriteback = false;

    m_crcDigest = 0;

    m_lock->initialize();
    m_sharedProcessLock->m_enable = isMultiProcess();
    m_exclusiveProcessLock->m_enable = isMultiProcess();

    // sensitive zone
    /*{
        SCOPED_LOCK(m_sharedProcessLock);
        loadFromFile();
    }*/
}
#endif

MMKV::~MMKV() {
    clearMemoryCache();

    delete m_dic;
#ifndef MMKV_DISABLE_CRYPT
    delete m_dicCrypt;
    delete m_crypter;
#endif
    delete m_metaInfo;
    delete m_lock;
    delete m_fileLock;
    delete m_sharedProcessLock;
    delete m_exclusiveProcessLock;
#ifdef MMKV_ANDROID
#ifndef MMKV_OHOS
    delete m_sharedProcessModeLock;
    delete m_exclusiveProcessModeLock;
    delete m_fileModeLock;
#endif // !MMKV_OHOS
    delete m_sharedMigrationLock;
    delete m_fileMigrationLock;
#endif // MMKV_ANDROID
    delete m_metaFile;
    delete m_file;

    MMKVInfo("destruct [%s]", m_mmapID.c_str());
}

MMKV *MMKV::defaultMMKV(MMKVMode mode, const string *cryptKey) {
#ifndef MMKV_ANDROID
    return mmkvWithID(DEFAULT_MMAP_ID, mode, cryptKey);
#else
    return mmkvWithID(DEFAULT_MMAP_ID, DEFAULT_MMAP_SIZE, mode, cryptKey);
#endif
}

static void initialize() {
    g_instanceDic = new unordered_map<string, MMKV *>;
    g_instanceLock = new ThreadLock();
    g_instanceLock->initialize();

    mmkv::DEFAULT_MMAP_SIZE = mmkv::getPageSize();
    MMKVInfo("version %s, page size %d, arch %s", MMKV_VERSION, DEFAULT_MMAP_SIZE, MMKV_ABI);

    // get CPU status of ARMv8 extensions (CRC32, AES)
#if defined(__aarch64__) && defined(__linux__) && !defined (MMKV_OHOS)
    auto hwcaps = getauxval(AT_HWCAP);
#    ifndef MMKV_DISABLE_CRYPT
    if (hwcaps & HWCAP_AES) {
        openssl::AES_set_encrypt_key = openssl_aes_arm_set_encrypt_key;
        openssl::AES_set_decrypt_key = openssl_aes_arm_set_decrypt_key;
        openssl::AES_encrypt = openssl_aes_arm_encrypt;
        openssl::AES_decrypt = openssl_aes_arm_decrypt;
        MMKVInfo("armv8 AES instructions is supported");
    } else {
        MMKVInfo("armv8 AES instructions is not supported");
    }
#    endif // MMKV_DISABLE_CRYPT
#    ifdef MMKV_USE_ARMV8_CRC32
    if (hwcaps & HWCAP_CRC32) {
        CRC32 = mmkv::armv8_crc32;
        MMKVInfo("armv8 CRC32 instructions is supported");
    } else {
        MMKVInfo("armv8 CRC32 instructions is not supported");
    }
#    endif // MMKV_USE_ARMV8_CRC32
#endif     // __aarch64__ && defined(__linux__) && !defined (MMKV_OHOS)

#if defined(MMKV_DEBUG) && !defined(MMKV_DISABLE_CRYPT)
    // AESCrypt::testAESCrypt();
    // KeyValueHolderCrypt::testAESToMMBuffer();
#endif
}

static void ensureMinimalInitialize() {
    static ThreadOnceToken_t once_control = ThreadOnceUninitialized;
    ThreadLock::ThreadOnce(&once_control, initialize);
}

void MMKV::initializeMMKV(const MMKVPath_t &rootDir, MMKVLogLevel logLevel, mmkv::LogHandler handler) {
    g_currentLogLevel = logLevel;
    g_logHandler = handler;

    ensureMinimalInitialize();

#ifdef MMKV_APPLE
    // crc32 instruction requires A10 chip, aka iPhone 7 or iPad 6th generation
    int device = 0, version = 0;
    GetAppleMachineInfo(device, version);
    MMKVInfo("Apple Device: %d, version: %d", device, version);
#endif

    if (g_rootDir.empty()) {
        g_rootDir = rootDir;
        // avoid operating g_realRootMap directly
        g_realRootDir = nameSpace(rootDir).getRootDir();
        mkPath(g_realRootDir);
    }

    MMKVInfo("root dir: " MMKV_PATH_FORMAT, g_realRootDir.c_str());
}

const MMKVPath_t &MMKV::getRootDir() {
    // for backword consistency we can't return g_realRootDir
    return g_rootDir;
}

#ifndef MMKV_ANDROID
MMKV *MMKV::mmkvWithID(const string &mmapID, MMKVMode mode, const string *cryptKey, const MMKVPath_t *rootPath, size_t expectedCapacity) {

    if (mmapID.empty() || !g_instanceLock) {
        return nullptr;
    }
    SCOPED_LOCK(g_instanceLock);

    auto mmapKey = mmapedKVKey(mmapID, rootPath);
    auto itr = g_instanceDic->find(mmapKey);
    if (itr != g_instanceDic->end()) {
        MMKV *kv = itr->second;
        return kv;
    }

    if (rootPath && !(mode & MMKV_READ_ONLY)) {
        MMKVPath_t specialPath = (*rootPath) + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
        if (!isFileExist(specialPath)) {
            mkPath(specialPath);
        }
    }
    auto theRootDir = rootPath ? rootPath : &g_realRootDir;
#ifdef MMKV_WIN32
    MMKVInfo("prepare to load %s (id %s) from rootPath %ls", mmapID.c_str(), mmapKey.c_str(), theRootDir->c_str());
#else
    MMKVInfo("prepare to load %s (id %s) from rootPath %s", mmapID.c_str(), mmapKey.c_str(), theRootDir->c_str());
#endif

    auto kv = new MMKV(mmapID, mode, cryptKey, rootPath, expectedCapacity);
    kv->m_mmapKey = mmapKey;
    (*g_instanceDic)[mmapKey] = kv;
    return kv;
}
#endif

void MMKV::onExit() {
    if (!g_instanceLock) {
        return;
    }
    SCOPED_LOCK(g_instanceLock);

    for (auto &pair : *g_instanceDic) {
        MMKV *kv = pair.second;
        kv->sync();
        kv->clearMemoryCache();
        delete kv;
        pair.second = nullptr;
    }

    delete g_instanceDic;
    g_instanceDic = nullptr;
}

const string &MMKV::mmapID() const {
    return m_mmapID;
}

mmkv::ContentChangeHandler g_contentChangeHandler = nullptr;

void MMKV::notifyContentChanged() {
    if (g_contentChangeHandler) {
        g_contentChangeHandler(m_mmapID);
    }
}

void MMKV::checkContentChanged() {
    SCOPED_LOCK(m_lock);
    checkLoadData();
}

void MMKV::registerContentChangeHandler(mmkv::ContentChangeHandler handler) {
    g_contentChangeHandler = handler;
}

void MMKV::unRegisterContentChangeHandler() {
    g_contentChangeHandler = nullptr;
}

void MMKV::clearMemoryCache(bool keepSpace) {
    SCOPED_LOCK(m_lock);
    if (m_needLoadFromFile) {
        return;
    }
    MMKVInfo("clearMemoryCache [%s]", m_mmapID.c_str());
    m_needLoadFromFile = true;
    m_hasFullWriteback = false;

    clearDictionary(m_dic);
#ifndef MMKV_DISABLE_CRYPT
    clearDictionary(m_dicCrypt);
    if (m_crypter) {
        if (m_metaInfo->m_version >= MMKVVersionRandomIV) {
            m_crypter->resetIV(m_metaInfo->m_vector, sizeof(m_metaInfo->m_vector));
        } else {
            m_crypter->resetIV();
        }
    }
#endif

    delete m_output;
    m_output = nullptr;

    if (!keepSpace) {
        m_file->clearMemoryCache();
    }
    // inter-process lock rely on MetaFile's fd, never close it
    // m_metaFile->clearMemoryCache();
    m_actualSize = 0;
    m_metaInfo->m_crcDigest = 0;
}

void MMKV::close() {
    MMKVInfo("close [%s]", m_mmapID.c_str());
    SCOPED_LOCK(g_instanceLock);
    m_lock->lock();

    auto itr = g_instanceDic->find(m_mmapKey);
    if (itr != g_instanceDic->end()) {
        g_instanceDic->erase(itr);
    }
    delete this;
}

#ifndef MMKV_DISABLE_CRYPT

string MMKV::cryptKey() const {
    SCOPED_LOCK(m_lock);

    if (m_crypter) {
        char key[AES_KEY_LEN];
        m_crypter->getKey(key);
        return {key, strnlen(key, AES_KEY_LEN)};
    }
    return "";
}

void MMKV::checkReSetCryptKey(const string *cryptKey) {
    SCOPED_LOCK(m_lock);

    if (m_crypter) {
        if (cryptKey && !cryptKey->empty()) {
            string oldKey = this->cryptKey();
            if (oldKey != *cryptKey) {
                MMKVInfo("setting new aes key");
                delete m_crypter;
                auto ptr = cryptKey->data();
                m_crypter = new AESCrypt(ptr, cryptKey->length());

                checkLoadData();
            } else {
                // nothing to do
            }
        } else {
            MMKVInfo("reset aes key");
            delete m_crypter;
            m_crypter = nullptr;

            checkLoadData();
        }
    } else {
        if (cryptKey && !cryptKey->empty()) {
            MMKVInfo("setting new aes key");
            auto ptr = cryptKey->data();
            m_crypter = new AESCrypt(ptr, cryptKey->length());

            checkLoadData();
        } else {
            // nothing to do
        }
    }
}

#endif // MMKV_DISABLE_CRYPT

bool MMKV::isFileValid() {
    return m_file->isFileValid();
}

// crc

// assuming m_file is valid
bool MMKV::checkFileCRCValid(size_t actualSize, uint32_t crcDigest) {
    auto ptr = (uint8_t *) m_file->getMemory();
    if (ptr) {
        m_crcDigest = (uint32_t) CRC32(0, (const uint8_t *) ptr + Fixed32Size, (uint32_t) actualSize);

        if (m_crcDigest == crcDigest) {
            return true;
        }
        MMKVError("check crc [%s] fail, crc32:%u, m_crcDigest:%u", m_mmapID.c_str(), crcDigest, m_crcDigest);
    }
    return false;
}

void MMKV::recalculateCRCDigestWithIV(const void *iv) {
    auto ptr = (const uint8_t *) m_file->getMemory();
    if (ptr) {
        m_crcDigest = 0;
        m_crcDigest = (uint32_t) CRC32(0, ptr + Fixed32Size, (uint32_t) m_actualSize);
        writeActualSize(m_actualSize, m_crcDigest, iv, IncreaseSequence);
    }
}

void MMKV::recalculateCRCDigestOnly() {
    auto ptr = (const uint8_t *) m_file->getMemory();
    if (ptr) {
        m_crcDigest = 0;
        m_crcDigest = (uint32_t) CRC32(0, ptr + Fixed32Size, (uint32_t) m_actualSize);
        writeActualSize(m_actualSize, m_crcDigest, nullptr, KeepSequence);
    }
}

void MMKV::updateCRCDigest(const uint8_t *ptr, size_t length) {
    if (ptr == nullptr) {
        return;
    }
    m_crcDigest = (uint32_t) CRC32(m_crcDigest, ptr, (uint32_t) length);

    writeActualSize(m_actualSize, m_crcDigest, nullptr, KeepSequence);
}

// set & get

bool MMKV::set(bool value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(bool value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    size_t size = mmkv_unlikely(m_enableKeyExpire) ? Fixed32Size + pbBoolSize() : pbBoolSize();
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeBool(value);
    if (mmkv_unlikely(m_enableKeyExpire)) {
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
    } else {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
    }

    return setDataForKey(std::move(data), key);
}

bool MMKV::set(int32_t value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(int32_t value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    size_t size = mmkv_unlikely(m_enableKeyExpire) ? Fixed32Size + pbInt32Size(value) : pbInt32Size(value);
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeInt32(value);
    if (mmkv_unlikely(m_enableKeyExpire)) {
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
    } else {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
    }

    return setDataForKey(std::move(data), key);
}

bool MMKV::set(uint32_t value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(uint32_t value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    size_t size = mmkv_unlikely(m_enableKeyExpire) ? Fixed32Size + pbUInt32Size(value) : pbUInt32Size(value);
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeUInt32(value);
    if (mmkv_unlikely(m_enableKeyExpire)) {
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
    } else {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
    }

    return setDataForKey(std::move(data), key);
}

bool MMKV::set(int64_t value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(int64_t value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    size_t size = mmkv_unlikely(m_enableKeyExpire) ? Fixed32Size + pbInt64Size(value) : pbInt64Size(value);
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeInt64(value);
    if (mmkv_unlikely(m_enableKeyExpire)) {
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
    } else {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
    }

    return setDataForKey(std::move(data), key);
}

bool MMKV::set(uint64_t value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(uint64_t value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    size_t size = mmkv_unlikely(m_enableKeyExpire) ? Fixed32Size + pbUInt64Size(value) : pbUInt64Size(value);
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeUInt64(value);
    if (mmkv_unlikely(m_enableKeyExpire)) {
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
    } else {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
    }

    return setDataForKey(std::move(data), key);
}

bool MMKV::set(float value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(float value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    size_t size = mmkv_unlikely(m_enableKeyExpire) ? Fixed32Size + pbFloatSize() : pbFloatSize();
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeFloat(value);
    if (mmkv_unlikely(m_enableKeyExpire)) {
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
    } else {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
    }

    return setDataForKey(std::move(data), key);
}

bool MMKV::set(double value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(double value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    size_t size = mmkv_unlikely(m_enableKeyExpire) ? Fixed32Size + pbDoubleSize() : pbDoubleSize();
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeDouble(value);
    if (mmkv_unlikely(m_enableKeyExpire)) {
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
    } else {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
    }

    return setDataForKey(std::move(data), key);
}

bool MMKV::setDataForKey(mmkv::MMBuffer &&data, MMKV::MMKVKey_t key, uint32_t expireDuration) {
    if (mmkv_likely(!m_enableKeyExpire)) {
        assert(expireDuration == ExpireNever && "setting expire duration without calling enableAutoKeyExpire() first");
        return setDataForKey(std::move(data), key, true);
    } else {
        auto tmp = MMBuffer(pbMMBufferSize(data) + Fixed32Size);
        CodedOutputData output(tmp.getPtr(), tmp.length());
        output.writeData(data);
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        output.writeRawLittleEndian32(UInt32ToInt32(time));
        return setDataForKey(std::move(tmp), key);
    }
}

bool MMKV::set(const char *value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(const char *value, MMKVKey_t key, uint32_t expireDuration) {
    if (!value) {
        removeValueForKey(key);
        return true;
    }
    return setDataForKey(MMBuffer((void *) value, strlen(value), MMBufferNoCopy), key, expireDuration);
}

bool MMKV::set(const string &value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(const string &value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    return setDataForKey(MMBuffer((void *) value.data(), value.length(), MMBufferNoCopy), key, expireDuration);
}

bool MMKV::set(string_view value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(string_view value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    return setDataForKey(MMBuffer((void *) value.data(), value.length(), MMBufferNoCopy), key, expireDuration);
}

bool MMKV::set(const MMBuffer &value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(const MMBuffer &value, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
    return setDataForKey(MMBuffer(value.getPtr(), value.length(), MMBufferNoCopy), key, expireDuration);
}

bool MMKV::set(const vector<string> &value, MMKVKey_t key) {
    return set(value, key, m_expiredInSeconds);
}

bool MMKV::set(const vector<string> &v, MMKVKey_t key, uint32_t expireDuration) {
    if (isKeyEmpty(key)) {
        return false;
    }
#ifdef MMKV_HAS_CPP20
    auto data = MiniPBCoder::encodeDataWithObject(std::span(v));
#else
    auto data = MiniPBCoder::encodeDataWithObject(v);
#endif
    if (mmkv_unlikely(m_enableKeyExpire) && data.length() > 0) {
        auto tmp = MMBuffer(data.length() + Fixed32Size);
        auto ptr = (uint8_t *) tmp.getPtr();
        memcpy(ptr, data.getPtr(), data.length());
        auto time = (expireDuration != ExpireNever) ? getCurrentTimeInSecond() + expireDuration : ExpireNever;
        memcpy(ptr + data.length(), &time, Fixed32Size);
        data = std::move(tmp);
    }
    return setDataForKey(std::move(data), key);
}

bool MMKV::getString(MMKVKey_t key, string &result, bool inplaceModification) {
    if (isKeyEmpty(key)) {
        return false;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (inplaceModification) {
                input.readString(result);
            } else {
                result = input.readString();
            }
            return true;
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    return false;
}

bool MMKV::getBytes(MMKVKey_t key, mmkv::MMBuffer &result) {
    if (isKeyEmpty(key)) {
        return false;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            result = input.readData();
            return true;
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    return false;
}

MMBuffer MMKV::getBytes(MMKVKey_t key) {
    if (isKeyEmpty(key)) {
        return MMBuffer();
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            return input.readData();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    return MMBuffer();
}

bool MMKV::getVector(MMKVKey_t key, vector<string> &result) {
    if (isKeyEmpty(key)) {
        return false;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            result = MiniPBCoder::decodeVector(data);
            return true;
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    return false;
}

void MMKV::shared_lock() {
    m_lock->lock();
    m_sharedProcessLock->lock();
}

void MMKV::shared_unlock() {
    m_sharedProcessLock->unlock();
    m_lock->unlock();
}

bool MMKV::getBool(MMKVKey_t key, bool defaultValue, bool *hasValue) {
    if (isKeyEmpty(key)) {
        if (hasValue != nullptr) {
            *hasValue = false;
        }
        return defaultValue;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (hasValue != nullptr) {
                *hasValue = true;
            }
            return input.readBool();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    if (hasValue != nullptr) {
        *hasValue = false;
    }
    return defaultValue;
}

int32_t MMKV::getInt32(MMKVKey_t key, int32_t defaultValue, bool *hasValue) {
    if (isKeyEmpty(key)) {
        if (hasValue != nullptr) {
            *hasValue = false;
        }
        return defaultValue;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (hasValue != nullptr) {
                *hasValue = true;
            }
            return input.readInt32();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    if (hasValue != nullptr) {
        *hasValue = false;
    }
    return defaultValue;
}

uint32_t MMKV::getUInt32(MMKVKey_t key, uint32_t defaultValue, bool *hasValue) {
    if (isKeyEmpty(key)) {
        if (hasValue != nullptr) {
            *hasValue = false;
        }
        return defaultValue;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (hasValue != nullptr) {
                *hasValue = true;
            }
            return input.readUInt32();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    if (hasValue != nullptr) {
        *hasValue = false;
    }
    return defaultValue;
}

int64_t MMKV::getInt64(MMKVKey_t key, int64_t defaultValue, bool *hasValue) {
    if (isKeyEmpty(key)) {
        if (hasValue != nullptr) {
            *hasValue = false;
        }
        return defaultValue;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (hasValue != nullptr) {
                *hasValue = true;
            }
            return input.readInt64();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    if (hasValue != nullptr) {
        *hasValue = false;
    }
    return defaultValue;
}

uint64_t MMKV::getUInt64(MMKVKey_t key, uint64_t defaultValue, bool *hasValue) {
    if (isKeyEmpty(key)) {
        if (hasValue != nullptr) {
            *hasValue = false;
        }
        return defaultValue;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (hasValue != nullptr) {
                *hasValue = true;
            }
            return input.readUInt64();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    if (hasValue != nullptr) {
        *hasValue = false;
    }
    return defaultValue;
}

float MMKV::getFloat(MMKVKey_t key, float defaultValue, bool *hasValue) {
    if (isKeyEmpty(key)) {
        if (hasValue != nullptr) {
            *hasValue = false;
        }
        return defaultValue;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (hasValue != nullptr) {
                *hasValue = true;
            }
            return input.readFloat();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    if (hasValue != nullptr) {
        *hasValue = false;
    }
    return defaultValue;
}

double MMKV::getDouble(MMKVKey_t key, double defaultValue, bool *hasValue) {
    if (isKeyEmpty(key)) {
        if (hasValue != nullptr) {
            *hasValue = false;
        }
        return defaultValue;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            if (hasValue != nullptr) {
                *hasValue = true;
            }
            return input.readDouble();
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    if (hasValue != nullptr) {
        *hasValue = false;
    }
    return defaultValue;
}

size_t MMKV::getValueSize(MMKVKey_t key, bool actualSize) {
    if (isKeyEmpty(key)) {
        return 0;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    if (actualSize) {
        try {
            CodedInputData input(data.getPtr(), data.length());
            auto length = input.readInt32();
            if (length >= 0) {
                auto s_length = static_cast<size_t>(length);
                if (pbRawVarint32Size(length) + s_length == data.length()) {
                    return s_length;
                }
            }
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        } catch (...) {
            MMKVError("decode fail");
        }
    }
    return data.length();
}

int32_t MMKV::writeValueToBuffer(MMKVKey_t key, void *ptr, int32_t size) {
    if (isKeyEmpty(key) || size < 0) {
        return -1;
    }
    auto s_size = static_cast<size_t>(size);

    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_sharedProcessLock);
    auto data = getDataForKey(key);
    try {
        CodedInputData input(data.getPtr(), data.length());
        auto length = input.readInt32();
        auto offset = pbRawVarint32Size(length);
        if (length >= 0) {
            auto s_length = static_cast<size_t>(length);
            if (offset + s_length == data.length()) {
                if (s_length <= s_size) {
                    memcpy(ptr, (uint8_t *) data.getPtr() + offset, s_length);
                    return length;
                }
            } else {
                if (data.length() <= s_size) {
                    memcpy(ptr, data.getPtr(), data.length());
                    return static_cast<int32_t>(data.length());
                }
            }
        }
    } catch (std::exception &exception) {
        MMKVError("%s", exception.what());
    } catch (...) {
        MMKVError("encode fail");
    }
    return -1;
}

// enumerate

bool MMKV::containsKey(MMKVKey_t key) {
    SCOPED_LOCK(m_lock);
    checkLoadData();

    if (mmkv_likely(!m_enableKeyExpire)) {
        if (m_crypter) {
            return m_dicCrypt->find(key) != m_dicCrypt->end();
        } else {
            return m_dic->find(key) != m_dic->end();
        }
    }
    auto raw = getDataWithoutMTimeForKey(key);
    return raw.length() != 0;
}

size_t MMKV::count(bool filterExpire) {
    SCOPED_LOCK(m_lock);
    checkLoadData();

    if (mmkv_unlikely(filterExpire && m_enableKeyExpire)) {
        SCOPED_LOCK(m_exclusiveProcessLock);
        fullWriteback(nullptr, true);
    }

    if (m_crypter) {
        return m_dicCrypt->size();
    } else {
        return m_dic->size();
    }
}

size_t MMKV::totalSize() {
    SCOPED_LOCK(m_lock);
    checkLoadData();
    return m_file->getFileSize();
}

size_t MMKV::actualSize() {
    SCOPED_LOCK(m_lock);
    checkLoadData();
    return m_actualSize;
}

bool MMKV::removeValueForKey(MMKVKey_t key) {
    if (isKeyEmpty(key)) {
        return false;
    }
    if (isReadOnly()) {
        MMKVWarning("[%s] file readonly", m_mmapID.c_str());
        return false;
    }
    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_exclusiveProcessLock);
    checkLoadData();

    return removeDataForKey(key);
}

#ifndef MMKV_APPLE

vector<string> MMKV::allKeys(bool filterExpire) {
    SCOPED_LOCK(m_lock);
    checkLoadData();

    if (mmkv_unlikely(filterExpire && m_enableKeyExpire)) {
        SCOPED_LOCK(m_exclusiveProcessLock);
        fullWriteback(nullptr, true);
    }

    vector<string> keys;
    if (m_crypter) {
        for (const auto &itr : *m_dicCrypt) {
            keys.push_back(itr.first);
        }
    } else {
        for (const auto &itr : *m_dic) {
            keys.push_back(itr.first);
        }
    }
    return keys;
}

bool MMKV::removeValuesForKeys(const vector<string> &arrKeys) {
    if (isReadOnly()) {
        MMKVWarning("[%s] file readonly", m_mmapID.c_str());
        return false;
    }
    if (arrKeys.empty()) {
        return true;
    }
    if (arrKeys.size() == 1) {
        return removeValueForKey(arrKeys[0]);
    }

    SCOPED_LOCK(m_lock);
    SCOPED_LOCK(m_exclusiveProcessLock);
    checkLoadData();

    size_t deleteCount = 0;
    if (m_crypter) {
        for (const auto &key : arrKeys) {
            auto itr = m_dicCrypt->find(key);
            if (itr != m_dicCrypt->end()) {
                m_dicCrypt->erase(itr);
                deleteCount++;
            }
        }
    } else {
        for (const auto &key : arrKeys) {
            auto itr = m_dic->find(key);
            if (itr != m_dic->end()) {
                m_dic->erase(itr);
                deleteCount++;
            }
        }
    }
    if (deleteCount > 0) {
        m_hasFullWriteback = false;

        return fullWriteback();
    }
    return true;
}

#endif // MMKV_APPLE

// file

void MMKV::sync(SyncFlag flag) {
    MMKVInfo("MMKV::sync, SyncFlag = %d", flag);
    SCOPED_LOCK(m_lock);
    if (m_needLoadFromFile || !isFileValid()) {
        return;
    }
    SCOPED_LOCK(m_exclusiveProcessLock);

    m_file->msync(flag);
    m_metaFile->msync(flag);
}

void MMKV::lock() {
    SCOPED_LOCK(m_lock);
    m_exclusiveProcessLock->lock();
}
void MMKV::unlock() {
    SCOPED_LOCK(m_lock);
    m_exclusiveProcessLock->unlock();
}
bool MMKV::try_lock() {
    SCOPED_LOCK(m_lock);
    return m_exclusiveProcessLock->try_lock();
}

#ifndef MMKV_WIN32
void MMKV::lock_thread() {
    m_lock->lock();
}
void MMKV::unlock_thread() {
    m_lock->unlock();
}
bool MMKV::try_lock_thread() {
    return m_lock->try_lock();
}
#endif

// backup

static bool backupOneToDirectoryByFilePath(const string &mmapKey, const MMKVPath_t &srcPath, const MMKVPath_t &dstPath) {
    File crcFile(srcPath, OpenFlag::ReadOnly);
    if (!crcFile.isFileValid()) {
        return false;
    }

    bool ret;
    {
#ifdef MMKV_WIN32
        MMKVInfo("backup one mmkv[%s] from [%ls] to [%ls]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#else
        MMKVInfo("backup one mmkv[%s] from [%s] to [%s]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#endif
        FileLock fileLock(crcFile.getFd());
        InterProcessLock lock(&fileLock, SharedLockType);
        SCOPED_LOCK(&lock);

        ret = copyFile(srcPath, dstPath);
        if (ret) {
            auto srcCRCPath = srcPath + CRC_SUFFIX;
            auto dstCRCPath = dstPath + CRC_SUFFIX;
            ret = copyFile(srcCRCPath, dstCRCPath);
        }
        MMKVInfo("finish backup one mmkv[%s]", mmapKey.c_str());
    }
    return ret;
}

bool MMKV::backupOneToDirectory(const string &mmapKey, const MMKVPath_t &dstPath, const MMKVPath_t &srcPath, bool compareFullPath) {
    if (!g_instanceLock) {
        return false;
    }
    // we have to lock the creation of MMKV instance, regardless of in cache or not
    SCOPED_LOCK(g_instanceLock);
    MMKV *kv = nullptr;
    if (!compareFullPath) {
        auto itr = g_instanceDic->find(mmapKey);
        if (itr != g_instanceDic->end()) {
            kv = itr->second;
        }
    } else {
        // mmapKey is actually filename, we can't simply call find()
        for (auto &pair : *g_instanceDic) {
            if (pair.second->m_path == srcPath) {
                kv = pair.second;
                break;
            }
        }
    }
    // get one in cache, do it the easy way
    if (kv) {
#ifdef MMKV_WIN32
        MMKVInfo("backup one cached mmkv[%s] from [%ls] to [%ls]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#else
        MMKVInfo("backup one cached mmkv[%s] from [%s] to [%s]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#endif
        SCOPED_LOCK(kv->m_lock);
        SCOPED_LOCK(kv->m_sharedProcessLock);

        kv->sync();
        auto ret = copyFile(kv->m_path, dstPath);
        if (ret) {
            auto dstCRCPath = dstPath + CRC_SUFFIX;
            ret = copyFile(kv->m_crcPath, dstCRCPath);
        }
        MMKVInfo("finish backup one mmkv[%s], ret: %d", mmapKey.c_str(), ret);
        return ret;
    }

    // no luck with cache, do it the hard way
    bool ret = backupOneToDirectoryByFilePath(mmapKey, srcPath, dstPath);
    return ret;
}

bool MMKV::backupOneToDirectory(const string &mmapID, const MMKVPath_t &dstDir, const MMKVPath_t *srcDir) {
    auto rootPath = srcDir ? srcDir : &g_realRootDir;
    if (*rootPath == dstDir) {
        return true;
    }
    mkPath(dstDir);
    auto dstPath = mappedKVPathWithID(mmapID, &dstDir);
    string  mmapKey = mmapedKVKey(mmapID, rootPath);
#ifdef MMKV_ANDROID
    string srcPath;
    switch (tryMigrateLegacyMMKVFile(mmapID, rootPath)) {
        case MigrateStatus::OldToNewMigrateFail: {
            auto legacyID = legacyMmapedKVKey(mmapID, rootPath);
            srcPath = mappedKVPathWithID(legacyID, rootPath);
            break;
        }
        case MigrateStatus::NoneExist:
            MMKVWarning("file with ID [%s] not exist in path [%s]", mmapID.c_str(), rootPath->c_str());
            return false;
        default:
            srcPath = mappedKVPathWithID(mmapID, rootPath);
            break;
    }
#else
    auto srcPath = mappedKVPathWithID(mmapID, rootPath);
#endif
    return backupOneToDirectory(mmapKey, dstPath, srcPath, false);
}

bool endsWith(const MMKVPath_t &str, const MMKVPath_t &suffix) {
    if (str.length() >= suffix.length()) {
        return str.compare(str.length() - suffix.length(), suffix.length(), suffix) == 0;
    } else {
        return false;
    }
}

MMKVPath_t filename(const MMKVPath_t &path) {
    auto startPos = path.rfind(MMKV_PATH_SLASH);
    startPos++; // don't need to check for npos, because npos+1 == 0
    auto filename = path.substr(startPos);
    return filename;
}

size_t MMKV::backupAllToDirectory(const MMKVPath_t &dstDir, const MMKVPath_t &srcDir, bool isInSpecialDir) {
    unordered_set<MMKVPath_t> mmapIDSet;
    unordered_set<MMKVPath_t> mmapIDCRCSet;
    walkInDir(srcDir, WalkFile, [&](const MMKVPath_t &filePath, WalkType) {
        if (endsWith(filePath, CRC_SUFFIX)) {
            mmapIDCRCSet.insert(filePath);
        } else {
            mmapIDSet.insert(filePath);
        }
    });

    size_t count = 0;
    if (!mmapIDSet.empty()) {
        mkPath(dstDir);
        auto compareFullPath = isInSpecialDir;
        for (auto &srcPath : mmapIDSet) {
            auto srcCRCPath = srcPath + CRC_SUFFIX;
            if (mmapIDCRCSet.find(srcCRCPath) == mmapIDCRCSet.end()) {
#ifdef MMKV_WIN32
                MMKVWarning("crc not exist [%ls]", srcCRCPath.c_str());
#else
                MMKVWarning("crc not exist [%s]", srcCRCPath.c_str());
#endif
                continue;
            }
            auto basename = filename(srcPath);
            const auto &strBasename = MMKVPath_t2String(basename);
            auto mmapKey = isInSpecialDir ? strBasename : mmapedKVKey(strBasename, &srcDir);
            auto dstPath = dstDir + MMKV_PATH_SLASH;
            dstPath += basename;
            if (backupOneToDirectory(mmapKey, dstPath, srcPath, compareFullPath)) {
                count++;
            }
        }
    }
    return count;
}

size_t MMKV::backupAllToDirectory(const MMKVPath_t &dstDir, const MMKVPath_t *srcDir) {
    auto rootPath = srcDir ? srcDir : &g_realRootDir;
    if (*rootPath == dstDir) {
        return true;
    }
    auto count = backupAllToDirectory(dstDir, *rootPath, false);

    auto specialSrcDir = *rootPath + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
    if (isFileExist(specialSrcDir)) {
        auto specialDstDir = dstDir + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
        count += backupAllToDirectory(specialDstDir, specialSrcDir, true);
    }
    return count;
}

// restore

static bool restoreOneFromDirectoryByFilePath(const string &mmapKey, const MMKVPath_t &srcPath, const MMKVPath_t &dstPath) {
    auto dstCRCPath = dstPath + CRC_SUFFIX;
    File dstCRCFile(std::move(dstCRCPath), OpenFlag::ReadWrite | OpenFlag::Create);
    if (!dstCRCFile.isFileValid()) {
        return false;
    }

    bool ret;
    {
#ifdef MMKV_WIN32
        MMKVInfo("restore one mmkv[%s] from [%ls] to [%ls]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#else
        MMKVInfo("restore one mmkv[%s] from [%s] to [%s]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#endif
        FileLock fileLock(dstCRCFile.getFd());
        InterProcessLock lock(&fileLock, ExclusiveLockType);
        SCOPED_LOCK(&lock);

        ret = copyFileContent(srcPath, dstPath);
        if (ret) {
            auto srcCRCPath = srcPath + CRC_SUFFIX;
            ret = copyFileContent(srcCRCPath, dstCRCFile.getFd());
        }
        MMKVInfo("finish restore one mmkv[%s]", mmapKey.c_str());
    }
    return ret;
}

// We can't simply replace the existing file, because other processes might have already open it.
// They won't know a difference when the file has been replaced.
// We have to let them know by overriding the existing file with new content.
bool MMKV::restoreOneFromDirectory(const string &mmapKey, const MMKVPath_t &srcPath, const MMKVPath_t &dstPath, bool compareFullPath) {
    if (!g_instanceLock) {
        return false;
    }
    // we have to lock the creation of MMKV instance, regardless of in cache or not
    SCOPED_LOCK(g_instanceLock);
    MMKV *kv = nullptr;
    if (!compareFullPath) {
        auto itr = g_instanceDic->find(mmapKey);
        if (itr != g_instanceDic->end()) {
            kv = itr->second;
        }
    } else {
        // mmapKey is actually filename, we can't simply call find()
        for (auto &pair : *g_instanceDic) {
            if (pair.second->m_path == dstPath) {
                kv = pair.second;
                break;
            }
        }
    }
    // get one in cache, do it the easy way
    if (kv) {
#ifdef MMKV_WIN32
        MMKVInfo("restore one cached mmkv[%s] from [%ls] to [%ls]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#else
        MMKVInfo("restore one cached mmkv[%s] from [%s] to [%s]", mmapKey.c_str(), srcPath.c_str(), dstPath.c_str());
#endif
        SCOPED_LOCK(kv->m_lock);
        SCOPED_LOCK(kv->m_exclusiveProcessLock);

        kv->sync();
        auto ret = copyFileContent(srcPath, kv->m_file->getFd());
        kv->m_file->cleanMayflyFD();
        if (ret) {
            auto srcCRCPath = srcPath + CRC_SUFFIX;
            // ret = copyFileContent(srcCRCPath, kv->m_metaFile->getFd());
            // kv->m_metaFile->cleanMayflyFD();
#ifndef MMKV_ANDROID
            MemoryFile srcCRCFile(srcCRCPath);
#else
            MemoryFile srcCRCFile(srcCRCPath, DEFAULT_MMAP_SIZE, MMFILE_TYPE_FILE);
#endif
            if (srcCRCFile.isFileValid()) {
                memcpy(kv->m_metaFile->getMemory(), srcCRCFile.getMemory(), sizeof(MMKVMetaInfo));
            } else {
                ret = false;
            }
        }

        // reload data after restore
        kv->clearMemoryCache();
        kv->loadFromFile();
        if (kv->isMultiProcess()) {
            kv->notifyContentChanged();
        }

        MMKVInfo("finish restore one mmkv[%s], ret: %d", mmapKey.c_str(), ret);
        return ret;
    }

    // no luck with cache, do it the hard way
    bool ret = restoreOneFromDirectoryByFilePath(mmapKey, srcPath, dstPath);
    return ret;
}

bool MMKV::restoreOneFromDirectory(const string &mmapID, const MMKVPath_t &srcDir, const MMKVPath_t *dstDir) {
    auto rootPath = dstDir ? dstDir : &g_realRootDir;
    if (*rootPath == srcDir) {
        return true;
    }
    mkPath(*rootPath);
    auto srcPath = mappedKVPathWithID(mmapID, &srcDir);
    auto mmapKey = mmapedKVKey(mmapID, rootPath);
#ifdef MMKV_ANDROID
    string dstPath;
    if (tryMigrateLegacyMMKVFile(mmapID, rootPath) == MigrateStatus::OldToNewMigrateFail) {
        auto legacyID = legacyMmapedKVKey(mmapID, rootPath);
        dstPath = mappedKVPathWithID(legacyID, rootPath);
    } else {
        dstPath = mappedKVPathWithID(mmapID, rootPath);
    }
#else
    auto dstPath = mappedKVPathWithID(mmapID, rootPath);
#endif
    return restoreOneFromDirectory(mmapKey, srcPath, dstPath, false);
}

size_t MMKV::restoreAllFromDirectory(const MMKVPath_t &srcDir, const MMKVPath_t &dstDir, bool isInSpecialDir) {
    unordered_set<MMKVPath_t> mmapIDSet;
    unordered_set<MMKVPath_t> mmapIDCRCSet;
    walkInDir(srcDir, WalkFile, [&](const MMKVPath_t &filePath, WalkType) {
        if (endsWith(filePath, CRC_SUFFIX)) {
            mmapIDCRCSet.insert(filePath);
        } else {
            mmapIDSet.insert(filePath);
        }
    });

    size_t count = 0;
    if (!mmapIDSet.empty()) {
        mkPath(dstDir);
        auto compareFullPath = isInSpecialDir;
        for (auto &srcPath : mmapIDSet) {
            auto srcCRCPath = srcPath + CRC_SUFFIX;
            if (mmapIDCRCSet.find(srcCRCPath) == mmapIDCRCSet.end()) {
#ifdef MMKV_WIN32
                MMKVWarning("crc not exist [%ls]", srcCRCPath.c_str());
#else
                MMKVWarning("crc not exist [%s]", srcCRCPath.c_str());
#endif
                continue;
            }
            auto basename = filename(srcPath);
            const auto &strBasename = MMKVPath_t2String(basename);
            auto mmapKey = isInSpecialDir ? strBasename : mmapedKVKey(strBasename, &dstDir);
            auto dstPath = dstDir + MMKV_PATH_SLASH;
            dstPath += basename;
            if (restoreOneFromDirectory(mmapKey, srcPath, dstPath, compareFullPath)) {
                count++;
            }
        }
    }
    return count;
}

size_t MMKV::restoreAllFromDirectory(const MMKVPath_t &srcDir, const MMKVPath_t *dstDir) {
    auto rootPath = dstDir ? dstDir : &g_realRootDir;
    if (*rootPath == srcDir) {
        return true;
    }
    auto count = restoreAllFromDirectory(srcDir, *rootPath, true);

    auto specialSrcDir = srcDir + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
    if (isFileExist(specialSrcDir)) {
        auto specialDstDir = *rootPath + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
        count += restoreAllFromDirectory(specialSrcDir, specialDstDir, false);
    }
    return count;
}

// callbacks

void MMKV::registerErrorHandler(ErrorHandler handler) {
    if (!g_instanceLock) {
        return;
    }
    SCOPED_LOCK(g_instanceLock);
    g_errorHandler = handler;
}

void MMKV::unRegisterErrorHandler() {
    if (!g_instanceLock) {
        return;
    }
    SCOPED_LOCK(g_instanceLock);
    g_errorHandler = nullptr;
}

void MMKV::registerLogHandler(LogHandler handler) {
    if (!g_instanceLock) {
        return;
    }
    SCOPED_LOCK(g_instanceLock);
    g_logHandler = handler;
}

void MMKV::unRegisterLogHandler() {
    if (!g_instanceLock) {
        return;
    }
    SCOPED_LOCK(g_instanceLock);
    g_logHandler = nullptr;
}

void MMKV::setLogLevel(MMKVLogLevel level) {
    if (!g_instanceLock) {
        return;
    }
    SCOPED_LOCK(g_instanceLock);
    g_currentLogLevel = level;
}

static void mkSpecialCharacterFileDirectory() {
    MMKVPath_t path = g_realRootDir + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
    mkPath(path);
}

template <typename T>
static string md5(const basic_string<T> &value) {
    uint8_t md[MD5_DIGEST_LENGTH] = {};
    char tmp[3] = {}, buf[33] = {};
    openssl::MD5((const uint8_t *) value.c_str(), value.size() * (sizeof(T) / sizeof(uint8_t)), md);
    for (auto ch : md) {
        snprintf(tmp, sizeof(tmp), "%2.2x", ch);
        strcat(buf, tmp);
    }
    return {buf};
}

static MMKVPath_t encodeFilePath(const string &mmapID) {
    const char *specialCharacters = "\\/:*?\"<>|";
    string encodedID;
    bool hasSpecialCharacter = false;
    for (auto ch : mmapID) {
        if (strchr(specialCharacters, ch) != nullptr) {
            encodedID = md5(mmapID);
            hasSpecialCharacter = true;
            break;
        }
    }
    if (hasSpecialCharacter) {
        static ThreadOnceToken_t once = ThreadOnceUninitialized;
        ThreadLock::ThreadOnce(&once, mkSpecialCharacterFileDirectory);
        return MMKVPath_t(SPECIAL_CHARACTER_DIRECTORY_NAME) + MMKV_PATH_SLASH + string2MMKVPath_t(encodedID);
    } else {
        return string2MMKVPath_t(mmapID);
    }
}

static MMKVPath_t encodeFilePath(const string &mmapID, const MMKVPath_t &rootDir) {
    const char *specialCharacters = "\\/:*?\"<>|";
    string encodedID;
    bool hasSpecialCharacter = false;
    for (auto ch : mmapID) {
        if (strchr(specialCharacters, ch) != nullptr) {
            encodedID = md5(mmapID);
            hasSpecialCharacter = true;
            break;
        }
    }
    if (hasSpecialCharacter) {
        MMKVPath_t path = rootDir + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
        mkPath(path);

        return MMKVPath_t(SPECIAL_CHARACTER_DIRECTORY_NAME) + MMKV_PATH_SLASH + string2MMKVPath_t(encodedID);
    } else {
        return string2MMKVPath_t(mmapID);
    }
}

string mmapedKVKey(const string &mmapID, const MMKVPath_t *rootPath) {
    MMKVPath_t path;
    // compare by pointer to speedup a bit, it's OK false detecting
    if (rootPath && (rootPath != &g_realRootDir)) {
        auto tmp = *rootPath + MMKV_PATH_SLASH + string2MMKVPath_t(mmapID);
        path = absolutePath(tmp);
    } else {
        path = g_realRootDir + MMKV_PATH_SLASH + string2MMKVPath_t(mmapID);
    }
    return md5(path);
}

string legacyMmapedKVKey(const string &mmapID, const MMKVPath_t *rootPath) {
    if (rootPath && (*rootPath != g_rootDir)) {
        return md5(*rootPath + MMKV_PATH_SLASH + string2MMKVPath_t(mmapID));
    }
    return mmapID;
}

#ifndef MMKV_ANDROID
MMKVPath_t mappedKVPathWithID(const string &mmapID, const MMKVPath_t *rootPath) {
    if (rootPath && (rootPath != &g_realRootDir)) {
        auto path = *rootPath + MMKV_PATH_SLASH + encodeFilePath(mmapID, *rootPath);
        return absolutePath(path);
    }
    auto path = g_realRootDir + MMKV_PATH_SLASH + encodeFilePath(mmapID);
    return path;
}
#else
MMKVPath_t mappedKVPathWithID(const string &mmapID, const MMKVPath_t *rootPath, MMKVMode mode) {
    if (mode & MMKV_ASHMEM) {
        return ashmemMMKVPathWithID(encodeFilePath(mmapID));
    } else if (rootPath && (rootPath != &g_realRootDir)) {
        auto path = *rootPath + MMKV_PATH_SLASH + encodeFilePath(mmapID, *rootPath);
        return absolutePath(path);
    }
    auto path = g_realRootDir + MMKV_PATH_SLASH + encodeFilePath(mmapID);
    return path;
}
#endif

MMKVPath_t crcPathWithPath(const MMKVPath_t &kvPath) {
    return kvPath + CRC_SUFFIX;
}

MMKVRecoverStrategic onMMKVCRCCheckFail(const string &mmapID) {
    if (g_errorHandler) {
        return g_errorHandler(mmapID, MMKVErrorType::MMKVCRCCheckFail);
    }
    return OnErrorDiscard;
}

MMKVRecoverStrategic onMMKVFileLengthError(const string &mmapID) {
    if (g_errorHandler) {
        return g_errorHandler(mmapID, MMKVErrorType::MMKVFileLength);
    }
    return OnErrorDiscard;
}

// NameSpace

NameSpace MMKV::nameSpace(const MMKVPath_t &rootDir) {
    if (!g_instanceLock) {
        ensureMinimalInitialize();
    }

    static ThreadOnceToken_t once = ThreadOnceUninitialized;
    ThreadLock::ThreadOnce(&once, []{
        g_namespaceLock = new ThreadLock;
        g_namespaceLock->initialize();
    });
    SCOPED_LOCK(g_namespaceLock);

    auto itr = g_realRootMap.find(rootDir);
    if (itr == g_realRootMap.end()) {
        auto realRoot = absolutePath(rootDir);
        if (realRoot.ends_with(MMKV_PATH_SLASH)) {
            realRoot.erase(realRoot.size() - 1);
        }
        itr = g_realRootMap.emplace(rootDir, realRoot).first;
    }
    return NameSpace(itr->second);
}

NameSpace MMKV::defaultNameSpace() {
    if (g_rootDir.empty()) {
        MMKVWarning("MMKV has not been initialized, there's no default NameSpace.");
        return NameSpace(MMKVPath_t());
    }
    return NameSpace(g_realRootDir);
}

#ifndef MMKV_ANDROID
MMKV *NameSpace::mmkvWithID(const string &mmapID, MMKVMode mode, const string *cryptKey, size_t expectedCapacity) {
    return MMKV::mmkvWithID(mmapID, mode, cryptKey, &m_rootDir, expectedCapacity);
}
#endif

bool NameSpace::backupOneToDirectory(const std::string &mmapID, const MMKVPath_t &dstDir) {
    return MMKV::backupOneToDirectory(mmapID, dstDir, &m_rootDir);
}

bool NameSpace::restoreOneFromDirectory(const std::string &mmapID, const MMKVPath_t &srcDir) {
    return MMKV::restoreOneFromDirectory(mmapID, srcDir, &m_rootDir);
}

size_t NameSpace::backupAllToDirectory(const MMKVPath_t &dstDir) {
    return MMKV::backupAllToDirectory(dstDir, &m_rootDir);
}

size_t NameSpace::restoreAllFromDirectory(const MMKVPath_t &srcDir) {
    return MMKV::restoreAllFromDirectory(srcDir, &m_rootDir);
}

bool NameSpace::isFileValid(const std::string &mmapID) {
    return MMKV::isFileValid(mmapID, &m_rootDir);
}

bool NameSpace::removeStorage(const std::string &mmapID) {
    return MMKV::removeStorage(mmapID, &m_rootDir);
}

bool NameSpace::checkExist(const std::string &mmapID) {
    return MMKV::checkExist(mmapID, &m_rootDir);
}

MMKV_NAMESPACE_END
