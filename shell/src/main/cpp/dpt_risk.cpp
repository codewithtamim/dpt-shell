//
// Created by luoyesiqiu
//

#include "dpt_risk.h"
#include <android/api-level.h>
#include <climits>
#include "mbedtls/sha256.h"
#include "mz_crypt.h"
#include "dpt.h"

extern ShellConfig g_shell_config;

DPT_ENCRYPT NO_INLINE void dpt_crash() {
#ifdef DEBUG
    abort();
#else
    asm volatile(
#ifdef __aarch64__
    "mov x30,#0\t\n"
#elif __arm__
    "mov lr,#0\t\n"
#elif __i386__
    "ret\t\n"
#elif __x86_64__
    "pop %rbp\t\n"
#endif
);
#endif
}

DPT_ENCRYPT void junkCodeDexProtect(JNIEnv *env) {
    const char *className = g_shell_config.junk_class_name.empty()
            ? AY_OBFUSCATE(JUNK_CLASS_FULL_NAME)
            : g_shell_config.junk_class_name.c_str();
    jclass klass = dpt::jni::FindClass(env, className);
    if(klass == nullptr) {
        dpt_crash();
    }
}

// Compare in-memory libc .text CRC with on-disk .text CRC; crash if mismatched.
DPT_ENCRYPT NO_INLINE void verifyLibcTextCrc() {
    Dl_info info = {};
    if (dladdr(reinterpret_cast<const void *>(&fopen), &info) == 0
        || info.dli_fbase == nullptr) {
        DLOGW("dladdr libc failed, skip text crc");
        return;
    }

    std::string libc_path;
    if (info.dli_fname != nullptr) {
        if (info.dli_fname[0] == '/') {
            libc_path.assign(info.dli_fname);
        } else {
            libc_path = find_so_path(info.dli_fname);
        }
    }
    if (libc_path.empty()) {
        libc_path = find_so_path(AY_OBFUSCATE("libc.so"));
    }
    if (libc_path.empty()) {
        DLOGW("cannot resolve libc path, skip text crc");
        return;
    }

    Elf_Shdr shdr = {};
    get_elf_section(&shdr, libc_path.c_str(), AY_OBFUSCATE(".text"));
    if (shdr.sh_size == 0) {
        DLOGW("libc .text section missing or empty, skip text crc");
        return;
    }

    FILE *fp = fopen(libc_path.c_str(), "r");
    if (fp == nullptr) {
        DLOGW("cannot open libc file: %s, skip text crc", libc_path.c_str());
        return;
    }

    if (fseek(fp, static_cast<long>(shdr.sh_offset), SEEK_SET) != 0) {
        DLOGW("fseek libc .text failed, skip text crc");
        fclose(fp);
        return;
    }

    auto *file_buf = static_cast<uint8_t *>(malloc(shdr.sh_size));
    if (file_buf == nullptr) {
        DLOGW("malloc for libc .text failed, skip text crc");
        fclose(fp);
        return;
    }

    size_t nread = fread(file_buf, 1, shdr.sh_size, fp);
    fclose(fp);
    if (nread != shdr.sh_size) {
        DLOGW("fread libc .text incomplete, skip text crc");
        DPT_FREE(file_buf);
        return;
    }

    const auto *mem_base = reinterpret_cast<const uint8_t *>(info.dli_fbase) + shdr.sh_addr;
    if (!isMemReadable(mem_base, shdr.sh_size)) {
        DLOGW("libc .text memory not readable, skip text crc");
        DPT_FREE(file_buf);
        return;
    }

    uint32_t crc_file = 0;
    uint32_t crc_mem = 0;
    size_t remaining = shdr.sh_size;
    size_t offset = 0;
    while (remaining > 0) {
        int32_t chunk = remaining > static_cast<size_t>(INT32_MAX)
                        ? INT32_MAX
                        : static_cast<int32_t>(remaining);
        crc_file = mz_crypt_crc32_update(crc_file, file_buf + offset, chunk);
        crc_mem = mz_crypt_crc32_update(crc_mem, mem_base + offset, chunk);
        offset += static_cast<size_t>(chunk);
        remaining -= static_cast<size_t>(chunk);
    }
    DPT_FREE(file_buf);

    DLOGD("libc .text crc file=%08x mem=%08x size=%u", crc_file, crc_mem,
          static_cast<unsigned>(shdr.sh_size));
    if (crc_file != crc_mem) {
        DLOGW("libc .text crc mismatch, file=%08x mem=%08x", crc_file, crc_mem);
        dpt_crash();
    }
}

DPT_ENCRYPT void detectFrida() {
    const char *frida_agent = AY_OBFUSCATE("frida-agent");
    const char *pool_frida = AY_OBFUSCATE("pool-frida");
    const char *gmain = AY_OBFUSCATE("gmain");
    const char *gbus = AY_OBFUSCATE("gdbus");
    const char *gum_js_loop = AY_OBFUSCATE("gum-js-loop");

    int frida_so_count = find_in_maps(1, frida_agent);
    if (frida_so_count > 0) {
        DLOGD("found frida so");
        dpt_crash();
    }
    int frida_thread_count = find_in_threads_list(4
            , pool_frida
            , gmain
            , gbus
            , gum_js_loop);

    if (frida_thread_count >= 2) {
        DLOGD("found frida threads");
        dpt_crash();
    }
}

DPT_ENCRYPT void detectLsposed() {
    // Vendored signal list from Duck-Detector (Apache-2.0)
    // feature/lsposed/.../probes/maps_probe.cpp kSignatures.
    const char *t_lsposed = AY_OBFUSCATE("lsposed");
    const char *t_org_lsposed = AY_OBFUSCATE("org.lsposed");
    const char *t_libxposed = AY_OBFUSCATE("libxposed");
    const char *t_xposedbridge = AY_OBFUSCATE("xposedbridge");
    const char *t_api = AY_OBFUSCATE("de.robv.android.xposed");
    const char *t_lsplant = AY_OBFUSCATE("lsplant");
    const char *t_lspd_dex = AY_OBFUSCATE("lspd.dex");
    const char *t_lspd_so = AY_OBFUSCATE("lspd.so");
    const char *t_liblspd = AY_OBFUSCATE("liblspd");
    const char *t_edxposed = AY_OBFUSCATE("edxposed");
    const char *t_libedxp = AY_OBFUSCATE("libedxp");
    const char *t_lspatch = AY_OBFUSCATE("lspatch");
    const char *tokens[] = {
        t_lsposed, t_org_lsposed, t_libxposed, t_xposedbridge, t_api, t_lsplant,
        t_lspd_dex, t_lspd_so, t_liblspd, t_edxposed, t_libedxp, t_lspatch
    };

    char maps_path[64] = {0};
    snprintf(maps_path, sizeof(maps_path), AY_OBFUSCATE("/proc/%d/maps"), getpid());
    FILE *fp = fopen(maps_path, "r");
    if (fp == nullptr) {
        return;
    }
    char line[1024];
    while (fgets(line, sizeof(line), fp) != nullptr) {
        for (size_t i = 0; i < sizeof(tokens) / sizeof(tokens[0]); i++) {
            const char *needle = tokens[i];
            size_t nlen = strlen(needle);
            if (nlen == 0) {
                continue;
            }
            size_t hlen = strlen(line);
            if (nlen > hlen) {
                continue;
            }
            for (size_t off = 0; off + nlen <= hlen; off++) {
                if (dpt_strncasecmp(line + off, needle, nlen) == 0) {
                    DLOGD("found lsposed maps hit");
                    fclose(fp);
                    dpt_crash();
                    return;
                }
            }
        }
    }
    fclose(fp);
}

// Forward declarations (defined below, shared by the maps/mount checks).
static bool lineHasTokenCi(const char *line, const char *needle);
static bool lineField(const char *line, int idx, char *out, size_t outSize);
static bool optsHasRw(const char *opts);
static bool isSystemTarget(const char *target);

DPT_ENCRYPT void detectZygisk() {
    // Signal list derived from Duck-Detector (Apache-2.0)
    // feature/zygisk + feature/mount evidence: zygote-injected
    // frameworks and root hiders leave their names in mappings.
    const char *t_magisk = AY_OBFUSCATE("magisk");
    const char *t_magiskhide = AY_OBFUSCATE("magiskhide");
    const char *t_shamiko = AY_OBFUSCATE("shamiko");
    const char *t_riru = AY_OBFUSCATE("riru");
    const char *t_libriru = AY_OBFUSCATE("libriru");
    const char *t_zygisk = AY_OBFUSCATE("zygisk");
    const char *t_libzygisk = AY_OBFUSCATE("libzygisk");
    const char *t_zygisknext = AY_OBFUSCATE("zygisknext");
    const char *t_rezygisk = AY_OBFUSCATE("rezygisk");
    const char *t_nezygisk = AY_OBFUSCATE("nezygisk");
    const char *tokens[] = {
        t_magisk, t_magiskhide, t_shamiko, t_riru, t_libriru,
        t_zygisk, t_libzygisk, t_zygisknext, t_rezygisk, t_nezygisk
    };

    char maps_path[64] = {0};
    snprintf(maps_path, sizeof(maps_path), AY_OBFUSCATE("/proc/%d/maps"), getpid());
    FILE *fp = fopen(maps_path, "r");
    if (fp == nullptr) {
        return;
    }
    char line[1024];
    while (fgets(line, sizeof(line), fp) != nullptr) {
        for (size_t i = 0; i < sizeof(tokens) / sizeof(tokens[0]); i++) {
            if (lineHasTokenCi(line, tokens[i])) {
                DLOGD("found zygisk maps hit");
                fclose(fp);
                dpt_crash();
                return;
            }
        }
    }
    fclose(fp);
}

// Case-insensitive substring search.
static bool lineHasTokenCi(const char *line, const char *needle) {
    if (line == nullptr || needle == nullptr) {
        return false;
    }
    size_t nlen = strlen(needle);
    if (nlen == 0) {
        return false;
    }
    size_t hlen = strlen(line);
    if (nlen > hlen) {
        return false;
    }
    for (size_t off = 0; off + nlen <= hlen; off++) {
        if (dpt_strncasecmp(line + off, needle, nlen) == 0) {
            return true;
        }
    }
    return false;
}

// Copy 1-based whitespace-separated field; false if missing/truncated.
static bool lineField(const char *line, int idx, char *out, size_t outSize) {
    if (line == nullptr || out == nullptr || outSize == 0 || idx < 1) {
        return false;
    }
    int field = 1;
    const char *p = line;
    while (*p == ' ' || *p == '\t') {
        p++;
    }
    const char *start = p;
    while (true) {
        if (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\0') {
            if (field == idx) {
                size_t len = static_cast<size_t>(p - start);
                if (len == 0 || len >= outSize) {
                    return false;
                }
                memcpy(out, start, len);
                out[len] = '\0';
                return true;
            }
            field++;
            while (*p == ' ' || *p == '\t') {
                p++;
            }
            start = p;
            if (*p == '\n' || *p == '\0') {
                return false;
            }
            continue;
        }
        p++;
    }
}

// True when comma-separated mount options contain exact "rw".
static bool optsHasRw(const char *opts) {
    if (opts == nullptr) {
        return false;
    }
    const char *p = opts;
    while (*p != '\0') {
        while (*p == ',' || *p == ' ' || *p == '\t') {
            p++;
        }
        if (*p == '\0' || *p == '\n') {
            break;
        }
        const char *start = p;
        while (*p != ',' && *p != ' ' && *p != '\t' && *p != '\n' && *p != '\0') {
            p++;
        }
        size_t len = static_cast<size_t>(p - start);
        if (len == 2 && start[0] == 'r' && start[1] == 'w') {
            return true;
        }
    }
    return false;
}

static bool isSystemTarget(const char *target) {
    if (target == nullptr) {
        return false;
    }
    if (strcmp(target, "/system") == 0 || strcmp(target, "/system_ext") == 0) {
        return true;
    }
    if (strncmp(target, "/system/", 8) == 0) {
        return true;
    }
    if (strncmp(target, "/system_ext/", 12) == 0) {
        return true;
    }
    return false;
}

DPT_ENCRYPT void detectRootMounts() {
    // Derived from Duck-Detector (Apache-2.0) feature/mount evidence:
    // systemless root bind-mounts/overlays its files over system paths.
    const char *t_magisk = AY_OBFUSCATE("magisk");
    const char *t_overlay = AY_OBFUSCATE("overlay");
    const char *files[] = {
        AY_OBFUSCATE("/proc/self/mounts"),
        AY_OBFUSCATE("/proc/self/mountinfo"),
    };
    char target[256];
    char fstype[64];
    char opts[256];
    char line[1024];
    for (size_t f = 0; f < sizeof(files) / sizeof(files[0]); f++) {
        FILE *fp = fopen(files[f], "r");
        if (fp == nullptr) {
            continue;
        }
        while (fgets(line, sizeof(line), fp) != nullptr) {
            if (lineHasTokenCi(line, t_magisk)) {
                DLOGD("found magisk mount hit");
                fclose(fp);
                dpt_crash();
                return;
            }
            // mounts: <src> <target> <fstype> <opts>
            // mountinfo: <id> ... <root> <target> <opts> ... - <fstype> <src> ...
            bool parsed = false;
            const char *sep = strstr(line, " - ");
            if (sep != nullptr) {
                // mountinfo: target = field 5, opts = field 6 (pre-separator).
                char pre[896] = {0};
                size_t preLen = static_cast<size_t>(sep - line);
                if (preLen < sizeof(pre)) {
                    memcpy(pre, line, preLen);
                    pre[preLen] = '\0';
                    if (lineField(pre, 5, target, sizeof(target))
                            && lineField(pre, 6, opts, sizeof(opts))
                            && lineField(sep + 3, 1, fstype, sizeof(fstype))) {
                        parsed = true;
                    }
                }
            } else if (lineField(line, 2, target, sizeof(target))
                    && lineField(line, 3, fstype, sizeof(fstype))
                    && lineField(line, 4, opts, sizeof(opts))) {
                parsed = true;
            }
            if (!parsed) {
                continue;
            }
            if (!isSystemTarget(target)) {
                continue;
            }
            if (optsHasRw(opts) || dpt_strncasecmp(fstype, t_overlay, 7) == 0) {
                DLOGD("found writable system mount hit");
                fclose(fp);
                dpt_crash();
                return;
            }
        }
        fclose(fp);
    }
}

DPT_ENCRYPT void detectDebugger() {
    const char *status_path = AY_OBFUSCATE("/proc/self/status");
    FILE *fp = fopen(status_path, "r");
    if (fp == nullptr) {
        DLOGW("cannot open /proc/self/status, skip tracer pid check");
        return;
    }

    const char *tracer_key = AY_OBFUSCATE("TracerPid:");
    char line[256];
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (strncmp(line, tracer_key, strlen(tracer_key)) == 0) {
            int tracer_pid = 0;
            sscanf(line + strlen(tracer_key), "%d", &tracer_pid);
            if (tracer_pid != 0) {
                DLOGD("found tracer pid: %d", tracer_pid);
                fclose(fp);
                dpt_crash();
            }
            break;
        }
    }
    fclose(fp);
}

[[noreturn]] DPT_ENCRYPT void *detectRiskOnThread(__unused void *args) {
    while (true) {
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_FRIDA_DETECT) == 0) {
            detectFrida();
        }
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_LSPOSED_DETECT) == 0) {
            detectLsposed();
        }
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_ZYGISK_DETECT) == 0) {
            detectZygisk();
        }
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_MOUNT_DETECT) == 0) {
            detectRootMounts();
        }
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_CRC_DETECT) == 0) {
            verifyLibcTextCrc();
        }
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_ANTI_DEBUG) == 0) {
            detectDebugger();
        }
        sleep(10);
    }
}

DPT_ENCRYPT void detectRisk() {
    pthread_t t;
    pthread_create(&t, nullptr, detectRiskOnThread, nullptr);
}

static bool isSignHex(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
}

static bool tokenMatchesSha256(const char *actual, const char *token, size_t tokenLen) {
    char compact[64];
    size_t n = 0;
    for (size_t i = 0; i < tokenLen; i++) {
        char c = token[i];
        if (c == ':' || c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            continue;
        }
        if (!isSignHex(c) || n >= 64) {
            return false;
        }
        compact[n++] = c;
    }
    if (n != 64) {
        return false;
    }
    return dpt_strncasecmp(actual, compact, 64) == 0;
}

// expectedList is one SHA-256 or several separated by comma or semicolon.
// Colon-separated Play Console fingerprints are accepted inside each entry.
static bool signatureInAllowList(const char *actual, const char *expectedList) {
    if (actual == nullptr || expectedList == nullptr) {
        return false;
    }
    const char *start = expectedList;
    for (const char *p = expectedList;; p++) {
        if (*p == ',' || *p == ';' || *p == '\0') {
            if (tokenMatchesSha256(actual, start, static_cast<size_t>(p - start))) {
                return true;
            }
            if (*p == '\0') {
                return false;
            }
            start = p + 1;
        }
    }
}

DPT_ENCRYPT void verifyAppSignature(JNIEnv *env, jobject context, const char *expectedSha256) {
    static std::string actual = {};
    if (context == nullptr || expectedSha256 == nullptr || strlen(expectedSha256) == 0) {
        DLOGW("signature check not configured, skip");
        return;
    }

    if(!actual.empty()) {
        if (!signatureInAllowList(actual.c_str(), expectedSha256)) {
            DLOGW("signature cache verification failed, expected: %s actual: %s", expectedSha256, actual.c_str());
            dpt_crash();
        }
        return;
    }

    jobject pm = dpt::jni::CallObjectMethod(env, context,
            AY_OBFUSCATE("getPackageManager"),
            AY_OBFUSCATE("()Landroid/content/pm/PackageManager;"));
    if (pm == nullptr) {
        DLOGW("getPackageManager failed");
        dpt_crash();
        return;
    }

    jstring packageName = (jstring) dpt::jni::CallObjectMethod(env, context,
            AY_OBFUSCATE("getPackageName"),
            AY_OBFUSCATE("()Ljava/lang/String;"));
    if (packageName == nullptr) {
        DLOGW("getPackageName failed");
        dpt_crash();
        return;
    }

    int api = android_get_device_api_level();
    jint flags = (api >= 28) ? (jint)0x08000000 : (jint)0x40;

    jobject packageInfo = dpt::jni::CallObjectMethod(env, pm,
            AY_OBFUSCATE("getPackageInfo"),
            AY_OBFUSCATE("(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"),
            packageName, flags);
    if (packageInfo == nullptr) {
        DLOGW("getPackageInfo failed");
        dpt_crash();
        return;
    }

    jbyteArray certBytes = nullptr;
    if (api >= 28) {
        jobject signingInfo = dpt::jni::GetObjectField(env, packageInfo,
                AY_OBFUSCATE("signingInfo"),
                AY_OBFUSCATE("Landroid/content/pm/SigningInfo;"));
        if (signingInfo == nullptr) {
            DLOGW("signingInfo is null");
            dpt_crash();
            return;
        }
        jobjectArray signaturesArr = (jobjectArray) dpt::jni::CallObjectMethod(env, signingInfo,
                AY_OBFUSCATE("getApkContentsSigners"),
                AY_OBFUSCATE("()[Landroid/content/pm/Signature;"));
        if (signaturesArr == nullptr || env->GetArrayLength(signaturesArr) == 0) {
            DLOGW("getApkContentsSigners returned empty");
            dpt_crash();
            return;
        }
        jobject signature = env->GetObjectArrayElement(signaturesArr, 0);
        certBytes = (jbyteArray) dpt::jni::CallObjectMethod(env, signature,
                AY_OBFUSCATE("toByteArray"), AY_OBFUSCATE("()[B"));
    } else {
        jobjectArray signaturesArr = (jobjectArray) dpt::jni::GetObjectField(env, packageInfo,
                AY_OBFUSCATE("signatures"),
                AY_OBFUSCATE("[Landroid/content/pm/Signature;"));
        if (signaturesArr == nullptr || env->GetArrayLength(signaturesArr) == 0) {
            DLOGW("signatures field is empty");
            dpt_crash();
            return;
        }
        jobject signature = env->GetObjectArrayElement(signaturesArr, 0);
        certBytes = (jbyteArray) dpt::jni::CallObjectMethod(env, signature,
                AY_OBFUSCATE("toByteArray"), AY_OBFUSCATE("()[B"));
    }

    if (certBytes == nullptr) {
        DLOGW("certBytes is null");
        dpt_crash();
        return;
    }

    jsize certLen = env->GetArrayLength(certBytes);
    jbyte *certData = env->GetByteArrayElements(certBytes, nullptr);

    uint8_t sha256Output[32];
    mbedtls_sha256(reinterpret_cast<const unsigned char *>(certData),
                   static_cast<size_t>(certLen), sha256Output, 0);

    env->ReleaseByteArrayElements(certBytes, certData, JNI_ABORT);

    char sha256Hex[65] = {0};
    for (int i = 0; i < 32; i++) {
        snprintf(sha256Hex + i * 2, 3, "%02x", sha256Output[i]);
    }

    actual.assign(sha256Hex);

    if (!signatureInAllowList(sha256Hex, expectedSha256)) {
        DLOGW("signature verification failed, expected: %s actual: %s", expectedSha256, sha256Hex);
        dpt_crash();
    }
}